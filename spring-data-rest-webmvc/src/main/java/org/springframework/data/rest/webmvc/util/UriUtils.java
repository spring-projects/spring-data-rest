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

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.hateoas.core.AnnotationMappingDiscoverer;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Utility methods to work with requests and URIs.
 * 
 * @author Oliver Gierke
 */
public abstract class UriUtils {

	private static AnnotationMappingDiscoverer DISCOVERER = new AnnotationMappingDiscoverer(RequestMapping.class);

	private UriUtils() {}

	/**
	 * Returns the value for the mapping variable with the given name.
	 * 
	 * @param variable must not be {@literal null} or empty.
	 * @param parameter
	 * @param request
	 * @return
	 */
	public static String findMappingVariable(String variable, Method method, String lookupPath) {

		Assert.hasText(variable, "Variable name must not be null or empty!");
		Assert.notNull(method, "Method must not be null!");

		String mapping = DISCOVERER.getMapping(method);

		Map<String, String> variables = new org.springframework.web.util.UriTemplate(mapping).match(lookupPath);
		String value = variables.get(variable);

		if (value != null) {
			return value;
		}

		return null;
	}
}
