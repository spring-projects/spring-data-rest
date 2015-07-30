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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller with a few convenience redirects to expose the HAL browser shipped as static content.
 *
 * @author Oliver Gierke
 * @soundtrack Miles Davis - So what (Kind of blue)
 */
@BasePathAwareController
public class HalBrowser {

	private static String BROWSER = "/browser";
	public static String BROWSER_INDEX = BROWSER.concat("/index.html");

	private final RepositoryRestConfiguration configuration;

	/**
	 * Creates a new {@link HalBrowser} for the given {@link RepositoryRestConfiguration}.
	 *
	 * @param configuration must not be {@literal null}.
	 */
	@Autowired
	public HalBrowser(RepositoryRestConfiguration configuration) {

		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");

		this.configuration = configuration;
	}

	/**
	 * Redirects requests to the API root asking for HTML to the HAL browser.
	 *
	 * @return
	 */
	@RequestMapping(value = { "/", "" }, method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public View index() {
		return browser();
	}

	/**
	 * Redirects to the actual {@code index.html}.
	 *
	 * @return
	 */
	@RequestMapping(value = "/browser", method = RequestMethod.GET)
	public View browser() {

		String basePath = configuration.getBasePath().toString();
		return new RedirectView(basePath.concat(BROWSER_INDEX).concat("#").concat(basePath), true);
	}
}