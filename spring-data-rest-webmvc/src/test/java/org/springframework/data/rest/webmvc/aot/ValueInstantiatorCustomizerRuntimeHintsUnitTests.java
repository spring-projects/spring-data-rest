/*
 * Copyright 2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;

/**
 * Unit tests for {@link ValueInstantiatorCustomizerRuntimeHints}.
 *
 * @author Oliver Drotbohm
 */
class ValueInstantiatorCustomizerRuntimeHintsUnitTests {

	RuntimeHintsRegistrar registrar = new ValueInstantiatorCustomizerRuntimeHints();

	@Test // #2213
	void registersHintsForStdValueInstantiator() {

		var hints = new RuntimeHints();

		registrar.registerHints(hints, getClass().getClassLoader());

		assertThat(RuntimeHintsPredicates.reflection().onField(StdValueInstantiator.class, "_constructorArguments"))
				.accepts(hints);
	}
}
