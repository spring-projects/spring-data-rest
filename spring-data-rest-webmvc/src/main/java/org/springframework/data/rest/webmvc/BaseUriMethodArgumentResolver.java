/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.annotation.BaseURI;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class BaseUriMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final RepositoryRestConfiguration config;

	public BaseUriMethodArgumentResolver(RepositoryRestConfiguration config) {
		this.config = config;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (null != parameter.getParameterAnnotation(BaseURI.class) && parameter.getParameterType() == URI.class);
	}

	@Override
	public URI resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);

		// Use configured URI if there is one or set the current one as the default if not.
		if (null == config.getBaseUri()) {
			URI baseUri = ServletUriComponentsBuilder.fromServletMapping(servletRequest).build().toUri();
			config.setBaseUri(baseUri);
		}

		return config.getBaseUri();
	}
}
