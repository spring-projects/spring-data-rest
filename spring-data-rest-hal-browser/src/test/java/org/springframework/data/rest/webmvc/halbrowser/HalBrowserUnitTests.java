/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.halbrowser;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link HalBrowser}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Nils Wülker - Homeless Diamond (feat. Lauren Flynn)
 */
public class HalBrowserUnitTests {

	@Test // DATAREST-565, DATAREST-720
	public void createsContextRelativeRedirectForBrowser() throws Exception {

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/context");
		request.setContextPath("/context");

		View view = new HalBrowser().browser(request);

		assertThat(view).isInstanceOf(RedirectView.class);

		((AbstractView) view).render(Collections.<String, Object> emptyMap(), request, response);

		UriComponents components = UriComponentsBuilder.fromUriString(response.getHeader(HttpHeaders.LOCATION)).build();

		assertThat(components.getPath()).startsWith("/context");
		assertThat(components.getFragment()).isEqualTo("/context");
	}

	@Test // DATAREST-1264
	public void producesProxyRelativeRedirectIfNecessary() throws ServletException, IOException {

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/browser");
		request.addHeader("X-Forwarded-Host", "somehost");
		request.addHeader("X-Forwarded-Port", "4711");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Prefix", "/prefix");
		ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

		filter.doFilter(request, response, (req, resp) -> {
			View view = new HalBrowser().browser((HttpServletRequest) req);

			assertThat(view).isInstanceOf(RedirectView.class);

			String url = ((RedirectView) view).getUrl();

			assertThat(url).startsWith("https://somehost:4711/prefix").endsWith("/prefix");
		});
	}
}
