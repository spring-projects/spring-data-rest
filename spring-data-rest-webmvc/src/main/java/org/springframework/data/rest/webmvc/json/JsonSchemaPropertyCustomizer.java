/*
 * Copyright 2015-2018 the original author or authors.
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

import org.springframework.data.rest.webmvc.json.JsonSchema.JsonSchemaProperty;
import org.springframework.data.util.TypeInformation;

/**
 * Callback interface to customize the {@link JsonSchemaProperty} created by default for a given type.
 *
 * @author Oliver Gierke
 * @since 2.4
 * @soundtrack Superflight - Four Sided Cube (Bellyjam)
 */
public interface JsonSchemaPropertyCustomizer {

	/**
	 * Returns the customized {@link JsonSchemaProperty} based on the given one and the given type.
	 *
	 * @param property will never be {@literal null}.
	 * @param type will never be {@literal null}.
	 * @return
	 */
	JsonSchemaProperty customize(JsonSchemaProperty property, TypeInformation<?> type);
}
