/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import org.springframework.data.domain.Pageable;

/**
 * Value object to capture a {@link Pageable} as well whether it is the default one configured.
 *
 * @author Oliver Gierke
 */
@Value
public class DefaultedPageable {

	/**
	 * Returns the delegate {@link Pageable}.
	 *
	 * @return can be {@literal null}.
	 */
	private final @NonNull Pageable pageable;
	private final @Getter(value = AccessLevel.NONE) boolean isDefault;

	/**
	 * Returns whether the contained {@link Pageable} is the default one configured.
	 *
	 * @return the isDefault
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * Returns {@link Pageable#unpaged()} if the contained {@link Pageable} is the default one.
	 *
	 * @return will never be {@literal null}.
	 * @since 3.3
	 */
	public Pageable unpagedIfDefault() {
		return isDefault ? Pageable.unpaged() : pageable;
	}
}
