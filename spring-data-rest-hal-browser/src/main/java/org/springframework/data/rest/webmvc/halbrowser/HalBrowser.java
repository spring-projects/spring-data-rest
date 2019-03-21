/*
 * Copyright 2015-2016 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;

/**
 * Controller with a few convenience redirects to expose the HAL browser shipped as static content.
 * 
 * @author Oliver Gierke
 * @soundtrack Miles Davis - So what (Kind of blue)
 */
@BasePathAwareController
public class HalBrowser {

	private static String BROWSER = "/browser";
	private static String INDEX = "/index.html";

	/**
	 * Redirects requests to the API root asking for HTML to the HAL browser.
	 * 
	 * @return
	 */
	@RequestMapping(value = { "/", "" }, method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public View index(HttpServletRequest request) {
		return getRedirectView(request, false);
	}

	/**
	 * Redirects to the actual {@code index.html}.
	 * 
	 * @return
	 */
	@RequestMapping(value = "/browser", method = RequestMethod.GET)
	public View browser(HttpServletRequest request) {
		return getRedirectView(request, request.getRequestURI().endsWith("/browser"));
	}

	/**
	 * Returns the View to redirect to to access the HAL browser.
	 * 
	 * @param request must not be {@literal null}.
	 * @param browserRelative
	 * @return
	 */
	private View getRedirectView(HttpServletRequest request, boolean browserRelative) {

		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(request);

		UriComponents components = builder.build();
		String path = components.getPath() == null ? "" : components.getPath();

		if (!browserRelative) {
			builder.path(BROWSER);
		}

		builder.path(INDEX);
		builder.fragment(browserRelative ? path.substring(0, path.lastIndexOf("/browser")) : path);

		return new RedirectView(builder.build().toUriString());
	}
}
