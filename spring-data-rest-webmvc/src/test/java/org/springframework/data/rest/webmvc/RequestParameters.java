/*
 * Copyright 2013 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
public class RequestParameters {

	public static RequestParameters NONE = new RequestParameters();

	private final Map<String, String[]> parameters;

	public RequestParameters(String key, String... values) {
		this(new HashMap<String, String[]>(), key, values);
	}

	private RequestParameters(Map<String, String[]> parameters, String key, String... values) {

		Assert.notNull(parameters, "Parameters must not be null!");
		Assert.hasText(key, "Key must not be null or empty!");

		this.parameters = new HashMap<String, String[]>(parameters);
		this.parameters.put(key, values);
	}

	private RequestParameters() {
		this.parameters = new HashMap<String, String[]>();
	}

	public RequestParameters and(String key, String... values) {
		return new RequestParameters(parameters, key, values);
	}

	public Map<String, String[]> asMap() {
		return Collections.unmodifiableMap(parameters);
	}
}
