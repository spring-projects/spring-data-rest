/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import lombok.EqualsAndHashCode;
import lombok.Value;

import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * @author Oliver Gierke
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class JsonDeserializationException extends HttpMessageNotReadableException {

	private static final long serialVersionUID = 6580250867630022225L;

	private final Class<?> type;
	private final DeserializationErrors errors;

	public JsonDeserializationException(Class<?> type, DeserializationErrors errors) {

		super("Could not read payload!");

		this.type = type;
		this.errors = errors;
	}
}
