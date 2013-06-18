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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Oliver Gierke
 */
public class ControllerUtils {

	public static final Resource<?> EMPTY_RESOURCE = new Resource<Object>(Collections.emptyList());
	public static final Resources<Resource<?>> EMPTY_RESOURCES = new Resources<Resource<?>>(
			Collections.<Resource<?>> emptyList());
	public static final Iterable<Resource<?>> EMPTY_RESOURCE_LIST = Collections.emptyList();
	public static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);

	public static <R extends Resource<?>> ResponseEntity<Resource<?>> toResponseEntity(HttpHeaders headers, R resource,
			HttpStatus status) {

		HttpHeaders hdrs = new HttpHeaders();
		if (null != headers) {
			hdrs.putAll(headers);
		}

		return new ResponseEntity<Resource<?>>(resource, hdrs, status);
	}
}
