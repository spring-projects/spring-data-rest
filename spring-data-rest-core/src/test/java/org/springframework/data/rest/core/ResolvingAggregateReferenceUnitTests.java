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
package org.springframework.data.rest.core;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResolvingAggregateReference}.
 *
 * @author Oliver Drotbohm
 */
class ResolvingAggregateReferenceUnitTests {

	@Test // GH-2239
	void usesResolverForFinalInstanceLookup() {

		var reference = new ResolvingAggregateReference<>(URI.create("/foo/42"), it -> "aggregate", it -> 42L);

		assertThat(reference.resolveAggregate()).isEqualTo("aggregate");
		assertThat(reference.resolveId()).isEqualTo(42);
	}

	@Test // GH-2239
	void appliesCustomExtractor() {

		var reference = new ResolvingAggregateReference<>(URI.create("/foo/42"), it -> "aggregate",
				it -> Long.valueOf(it.toString())).withIdSource(it -> it.getPathSegments().get(1));

		assertThat(reference.resolveId()).isEqualTo(42);
	}
}
