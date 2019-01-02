/*
 * Copyright 2014-2019 the original author or authors.
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

import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;

/**
 * Constants to refer to supported media types.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public class RestMediaTypes {


	public static final MediaType HAL_JSON = MediaTypes.HAL_JSON;

	public static final MediaType JSON_PATCH_JSON = MediaType.valueOf("application/json-patch+json");
	public static final MediaType MERGE_PATCH_JSON = MediaType.valueOf("application/merge-patch+json");

	public static final String ALPS_JSON_VALUE = "application/alps+json";
	public static final MediaType ALPS_JSON = MediaType.parseMediaType(ALPS_JSON_VALUE);

	public static final String SCHEMA_JSON_VALUE = "application/schema+json";
	public static final MediaType SCHEMA_JSON = MediaType.valueOf(SCHEMA_JSON_VALUE);

	public static final MediaType SPRING_DATA_VERBOSE_JSON = MediaType.valueOf("application/x-spring-data-verbose+json");

	public static final String SPRING_DATA_COMPACT_JSON_VALUE = "application/x-spring-data-compact+json";
	public static final MediaType SPRING_DATA_COMPACT_JSON = MediaType.valueOf(SPRING_DATA_COMPACT_JSON_VALUE);

	public static final String TEXT_URI_LIST_VALUE = "text/uri-list";
	public static final MediaType TEXT_URI_LIST = MediaType.valueOf(TEXT_URI_LIST_VALUE);
}
