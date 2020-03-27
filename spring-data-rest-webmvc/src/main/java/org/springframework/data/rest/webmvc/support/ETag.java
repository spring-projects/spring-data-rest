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

import static org.springframework.util.StringUtils.*;

import lombok.EqualsAndHashCode;

import java.util.Optional;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * A value object to represent ETags.
 *
 * @author Oliver Gierke
 */
@EqualsAndHashCode
public final class ETag {

	public static final ETag NO_ETAG = new ETag(null);

	private final String value;

	/**
	 * Creates a new {@link ETag} from the given value.
	 *
	 * @param value can be {@literal null}.
	 */
	private ETag(String value) {
		this.value = trimTrailingCharacter(trimLeadingCharacter(value, '"'), '"');
	}

	/**
	 * Creates a new {@link ETag} for the given {@link String} value. Falls back to {@link #NO_ETAG} in case
	 * {@literal null} is provided.
	 *
	 * @param value the source ETag value, must not be {@literal null}.
	 * @return
	 */
	public static ETag from(String value) {
		return new ETag(value);
	}

	public static ETag from(Optional<String> value) {
		return value.map(ETag::new).orElse(NO_ETAG);
	}

	/**
	 * Creates a new {@link ETag} for the given {@link PersistentEntityResource}.
	 *
	 * @param resource must not be {@literal null}.
	 * @return
	 */
	public static ETag from(PersistentEntityResource resource) {

		Assert.notNull(resource, "PersistentEntityResource must not be null!");

		return from(resource.getPersistentEntity(), resource.getContent());
	}

	/**
	 * Creates a new {@link ETag} from the given {@link PersistentEntity} and target bean.
	 *
	 * @param entity must not be {@literal null}.
	 * @param bean must not be {@literal null}.
	 * @return
	 */
	public static ETag from(PersistentEntity<?, ? extends PersistentProperty<?>> entity, Object bean) {
		return getVersionInformation(entity, bean).map(ETag::from).orElse(NO_ETAG);
	}

	/**
	 * Verifies the ETag to be created for the given target bean with the current one and raises a
	 * {@link ETagDoesntMatchException} in case they don't match.
	 *
	 * @param entity must not be {@literal null}.
	 * @param target can be {@literal null}.
	 * @throws ETagDoesntMatchException in case the calculated {@link ETag} for the given bean does not match the current
	 *           one.
	 */
	public void verify(PersistentEntity<?, ?> entity, Object target) {

		if (this == NO_ETAG || target == null) {
			return;
		}

		if (!this.equals(from(entity, target))) {
			throw new ETagDoesntMatchException(target, this);
		}
	}

	/**
	 * Returns whether the {@link ETag} matches the given {@link PersistentEntity} and target. A more dissenting way of
	 * checking matches as it does not match if the ETag is {@link #NO_ETAG}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param target can be {@literal null}.
	 * @return
	 */
	public boolean matches(PersistentEntity<?, ?> entity, Object target) {

		if (this == NO_ETAG || target == null) {
			return false;
		}

		return this.equals(from(entity, target));
	}

	/**
	 * Adds the current {@link ETag} to the given headers.
	 *
	 * @param headers must not be {@literal null}.
	 * @return the {@link HttpHeaders} with the ETag header been set if the current {@link ETag} instance is not
	 *         {@link #NO_ETAG}.
	 */
	public HttpHeaders addTo(HttpHeaders headers) {

		Assert.notNull(headers, "HttpHeaders must not be null!");
		String stringValue = toString();

		if (stringValue == null) {
			return headers;
		}

		headers.setETag(stringValue);
		return headers;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return value == null ? null : "\"".concat(value).concat("\"");
	}

	/**
	 * Returns the quoted version property of a domain object, returns null if it doesn't contains the property
	 *
	 * @param entity
	 * @param bean
	 * @return
	 */
	private static Optional<String> getVersionInformation(PersistentEntity<?, ? extends PersistentProperty<?>> entity,
			Object bean) {

		Assert.notNull(entity, "PersistentEntity must not be null!");
		Assert.notNull(bean, "Target bean must not be null!");

		PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(bean);

		return Optional.ofNullable(entity.getVersionProperty())//
				.map(it -> accessor.getProperty(it))//
				.map(Object::toString);
	}
}
