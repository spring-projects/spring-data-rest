/*
 * Copyright 2023-2025 the original author or authors.
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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.AssociationUriResolvingDeserializerModifier.ValueInstantiatorCustomizer;

import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;

/**
 * Registers the {@link StdValueInstantiator}'s {@code _constructorArgs} field for reflection as needed by
 * {@link ValueInstantiatorCustomizer}.
 *
 * @author Oliver Drotbohm
 * @since 4.0.2
 * @soundtrack The Intersphere - Wanderer (https://www.youtube.com/watch?v=Sp_VyFBbDPA)
 */
class ValueInstantiatorCustomizerRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.reflection().registerField(ValueInstantiatorCustomizer.CONSTRUCTOR_ARGS_FIELD);
	}
}
