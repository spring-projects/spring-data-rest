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
package org.springframework.data.rest.webmvc.json.patch;

/**
 * <p>
 * Strategy interface for resolving values from an operation definition.
 * </p>
 * <p>
 * {@link Patch} implementation generically defines a patch without being tied to any particular patch specification.
 * But it's important to know the patch format when resolving the value of an operation, as the value format will likely
 * be tied to the patch specification. For example, the <code>value</code> attribute of a JSON Patch operation will
 * contain a JSON object. A different patch specification may define values in some non-JSON format.
 * </p>
 * <p>
 * This interface allows for pluggable evaluation of values, allowing {@link Patch} to remain independent of any
 * specific patch representation.
 * </p>
 * 
 * @author Craig Walls
 */
interface LateObjectEvaluator {

	Object evaluate(Class<?> type);
}
