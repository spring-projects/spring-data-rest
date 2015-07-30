/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.halbrowser;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Unit tests for {@link HalBrowser}.
 *
 * @author Oliver Gierke
 * @soundtrack Nils Wülker - Homeless Diamond (feat. Lauren Flynn)
 */
public class HalBrowserUnitTests {

	/**
	 * @see DATAREST-565
	 */
	@Test
	public void createsContextRelativeRedirectForBrowser() throws Exception {

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration(new ProjectionDefinitionConfiguration(),
				new MetadataConfiguration(), mock(EnumTranslationConfiguration.class));
		View view = new HalBrowser(configuration).browser();

		assertThat(view, is(instanceOf(RedirectView.class)));

		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContextPath("/context");

		((AbstractView) view).render(Collections.<String, Object> emptyMap(), request, response);

		assertThat(response.getHeader(HttpHeaders.LOCATION), Matchers.startsWith("/context"));
	}
}