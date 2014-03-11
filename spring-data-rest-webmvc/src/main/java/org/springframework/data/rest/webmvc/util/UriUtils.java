/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.hateoas.UriTemplate;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UrlPathHelper;

/**
 * Utility methods to work with requests and URIs.
 * 
 * @author Oliver Gierke
 */
public abstract class UriUtils {

	private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

	private UriUtils() {}

	/**
	 * Returns the value for the mapping variable with the given name.
	 * 
	 * @param variable must not be {@literal null} or empty.
	 * @param parameter
	 * @param request
	 * @return
	 */
	public static String findMappingVariable(String variable, MethodParameter parameter, NativeWebRequest request) {

		Assert.hasText(variable, "Variable name must not be null or empty!");
		Assert.notNull(parameter, "Method parameter must not be null!");
		Assert.notNull(request, "Request must not be null!");

		String lookupPath = getCleanLookupPath(request);
		RequestMapping annotation = parameter.getMethodAnnotation(RequestMapping.class);

		for (String mapping : annotation.value()) {

			Map<String, String> variables = new org.springframework.web.util.UriTemplate(mapping).match(lookupPath);
			String value = variables.get(variable);

			if (value != null) {
				return value;
			}
		}

		return null;
	}

	private static String getCleanLookupPath(NativeWebRequest request) {

		HttpServletRequest httpServletRequest = request.getNativeRequest(HttpServletRequest.class);
		String lookupPath = URL_PATH_HELPER.getLookupPathForRequest(httpServletRequest);
		return new UriTemplate(lookupPath).expand().toString();
	}
}
