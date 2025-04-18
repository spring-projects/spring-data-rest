/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.AbstractPropertyBindingResult;

/**
 * An {@link org.springframework.validation.Errors} implementation for use in the events mechanism of Spring Data REST.
 * Customizes actual field lookup by using a {@link org.springframework.data.mapping.PersistentPropertyAccessor} for
 * actual value lookups.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Florian Cramer
 */
public class ValidationErrors extends AbstractPropertyBindingResult {

	private static final long serialVersionUID = 8141826537389141361L;

	private final Object source;
	private final PersistentEntities entities;

	/**
	 * Creates a new {@link ValidationErrors} instance for the given source object and {@link PersistentEntity}.
	 *
	 * @param source the source object to gather validation errors on, must not be {@literal null}.
	 * @param entities the {@link PersistentEntities} for the given source instance, must not be {@literal null}.
	 */
	public ValidationErrors(Object source, PersistentEntities entities) {

		super(source.getClass().getSimpleName());

		Assert.notNull(source, "Entity must not be null");
		Assert.notNull(entities, "PersistentEntities must not be null");

		this.entities = entities;
		this.source = source;
	}

	@Override
	public ConfigurablePropertyAccessor getPropertyAccessor() {

		return new DirectFieldAccessor(getTarget()) {

			@Override
			public Object getPropertyValue(String propertyName) throws BeansException {

				Collection<String> segments = Arrays.asList(propertyName.split("\\."));
				Iterator<String> iterator = segments.iterator();
				Object value = source;

				do {

					value = lookupValueOn(value, iterator.next());

				} while (iterator.hasNext());

				return value;
			}

			/**
			 * @param value the original value, must not be {@literal null}..
			 * @param segment the property segment to look up, must not be {@literal null} or empty.
			 * @return
			 */
			@Nullable
			private Object lookupValueOn(Object value, String segment) {

				Optional<PersistentEntity<?, ? extends PersistentProperty<?>>> entity = entities
						.getPersistentEntity(value.getClass());

				return getAccessor(entity, value, segment).getPropertyValue(segment);
			}

			private static ConfigurablePropertyAccessor getAccessor(
					Optional<PersistentEntity<?, ? extends PersistentProperty<?>>> entity, Object value, String segment) {

				if (!entity.isPresent()) {
					return PropertyAccessorFactory.forDirectFieldAccess(value);
				}

				PersistentProperty<?> property = entity //
						.map(it -> it.getPersistentProperty(PropertyAccessorUtils.getPropertyName(segment))) //
						.orElseThrow(() -> new NotReadablePropertyException(value.getClass(), segment));

				return property.usePropertyAccess() //
						? PropertyAccessorFactory.forBeanPropertyAccess(value) //
						: PropertyAccessorFactory.forDirectFieldAccess(value);
			}
		};
	}

	@Override
	public Object getTarget() {
		return source;
	}
}
