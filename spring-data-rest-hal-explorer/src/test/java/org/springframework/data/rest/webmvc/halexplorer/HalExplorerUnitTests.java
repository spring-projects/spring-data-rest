/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.rest.webmvc.halexplorer;

import static org.assertj.core.api.Assertions.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link HalExplorer}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Nils Wülker - Homeless Diamond (feat. Lauren Flynn)
 */
class HalExplorerUnitTests {

	@Test // DATAREST-565, DATAREST-720
	void createsContextRelativeRedirectForBrowser() throws Exception {

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/context");
		request.setContextPath("/context");

		View view = new HalExplorer().explorer(request);

		assertThat(view).isInstanceOf(RedirectView.class);

		view.render(Collections.emptyMap(), request, response);

		UriComponents components = UriComponentsBuilder.fromUriString(response.getHeader(HttpHeaders.LOCATION)).build();

		assertThat(components.getPath()).startsWith("/context");
		assertThat(components.getFragment()).isEqualTo("uri=/context");
	}

	@Test // DATAREST-1264
	void producesProxyRelativeRedirectIfNecessary() throws ServletException, IOException {

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/explorer");
		request.addHeader("X-Forwarded-Host", "somehost");
		request.addHeader("X-Forwarded-Port", "4711");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Prefix", "/prefix");
		ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

		filter.doFilter(request, response, (req, resp) -> {
			View view = new HalExplorer().explorer((HttpServletRequest) req);

			assertThat(view).isInstanceOf(RedirectView.class);

			String url = ((RedirectView) view).getUrl();

			assertThat(url).startsWith("https://somehost:4711/prefix").endsWith("/prefix");
		});
	}
}
