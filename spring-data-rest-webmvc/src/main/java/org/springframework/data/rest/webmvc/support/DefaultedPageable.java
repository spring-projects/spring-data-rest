/*
 * Copyright 2014-2022 the original author or authors.
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

import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object to capture a {@link Pageable} as well whether it is the default one configured.
 *
 * @author Oliver Gierke
 */
public final class DefaultedPageable {

	private final Pageable pageable;
	private final boolean isDefault;

	public DefaultedPageable(Pageable pageable, boolean isDefault) {

		Assert.notNull(pageable, "Pageable must not be null");

		this.pageable = pageable;
		this.isDefault = isDefault;
	}

	/**
	 * Returns the delegate {@link Pageable}.
	 *
	 * @return can be {@literal null}.
	 */
	public Pageable getPageable() {
		return this.pageable;
	}

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

	@Override
	public boolean equals(@Nullable Object o) {

		if (o == this) {
			return true;
		}

		if (!(o instanceof DefaultedPageable)) {
			return false;
		}

		DefaultedPageable that = (DefaultedPageable) o;

		return Objects.equals(pageable, that.pageable) //
				&& isDefault == that.isDefault;
	}

	@Override
	public int hashCode() {
		return Objects.hash(pageable, isDefault);
	}

	@Override
	public java.lang.String toString() {
		return "DefaultedPageable(pageable=" + pageable + ", isDefault=" + isDefault + ")";
	}
}
