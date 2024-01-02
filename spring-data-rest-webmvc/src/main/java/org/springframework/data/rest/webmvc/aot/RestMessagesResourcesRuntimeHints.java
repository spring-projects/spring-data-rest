/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.aot;

import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * @author Oliver Drotbohm
 */
class RestMessagesResourcesRuntimeHints implements RuntimeHintsRegistrar {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.aot.hint.RuntimeHintsRegistrar#registerHints(org.springframework.aot.hint.RuntimeHints, java.lang.ClassLoader)
	 */
	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		ResourceHints resources = hints.resources();

		resources.registerPattern("rest-messages*.properties");
		resources.registerPattern("rest-default-messages.properties");
	}
}
