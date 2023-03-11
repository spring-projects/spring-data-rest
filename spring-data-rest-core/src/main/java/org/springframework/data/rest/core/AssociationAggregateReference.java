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
package org.springframework.data.rest.core;

import java.util.function.Function;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponents;

/**
 * An {@link AggregateReference} that can also resolve into jMolecules {@link Association} instances.
 *
 * @author Oliver Drotbohm
 * @since 4.1
 */
public interface AssociationAggregateReference<T extends AggregateRoot<T, ID>, ID extends Identifier>
		extends AggregateReference<T, ID> {

	/**
	 * Resolves the underlying URI into an {@link Association}, potentially applying the configured identifier extractor.
	 *
	 * @return can be {@literal null}.
	 * @see #withIdSource(Function)
	 */
	@Nullable
	default Association<T, ID> resolveAssociation() {
		return Association.forId(resolveId());
	}

	/**
	 * Resolves the underlying URI into an {@link Association}, potentially applying the configured identifier extractor.
	 *
	 * @return will never be {@literal null}.
	 * @throws IllegalStateException in case the value resolved is {@literal null}.
	 */
	@SuppressWarnings("null")
	default Association<T, ID> resolveRequiredAssociation() {
		return Association.forId(resolveRequiredId());
	}

	@Override
	AssociationAggregateReference<T, ID> withIdSource(Function<UriComponents, Object> extractor);
}
