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

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;

/**
 * Controller with a few convenience redirects to expose the HAL explorer shipped as static content.
 *
 * @author Oliver Gierke
 * @soundtrack Miles Davis - So what (Kind of blue)
 */
@BasePathAwareController
class HalExplorer {

	static final String EXPLORER = "/explorer";
	static final String INDEX = "/index.html";

	/**
	 * Redirects requests to the API root asking for HTML to the HAL explorer.
	 *
	 * @return
	 */
	@GetMapping(value = { "/", "" }, produces = MediaType.TEXT_HTML_VALUE)
	View index(HttpServletRequest request) {
		return getRedirectView(request, false);
	}

	/**
	 * Redirects to the actual {@code index.html}.
	 *
	 * @return
	 */
	@GetMapping(value = EXPLORER)
	public View explorer(HttpServletRequest request) {
		return getRedirectView(request, request.getRequestURI().endsWith(EXPLORER));
	}

	/**
	 * Returns the View to redirect to to access the HAL explorer.
	 *
	 * @param request must not be {@literal null}.
	 * @param explorerRelative
	 * @return
	 */
	private View getRedirectView(HttpServletRequest request, boolean explorerRelative) {

		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(request);

		UriComponents components = builder.build();
		String path = components.getPath() == null ? "" : components.getPath();

		if (!explorerRelative) {
			builder.path(EXPLORER);
		}

		builder.path(INDEX);
		builder.fragment(String.format("uri=%s", explorerRelative ? path.substring(0, path.lastIndexOf(EXPLORER)) : path));

		return new RedirectView(builder.build().toUriString());
	}
}
