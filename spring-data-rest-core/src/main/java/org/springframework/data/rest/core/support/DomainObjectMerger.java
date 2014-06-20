/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.rest.core.support;

import static org.springframework.data.rest.core.support.DomainObjectMerger.NullHandlingPolicy.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Component to be able to merge the first level of two objects.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Willie Wheeler
 */
public class DomainObjectMerger {

	private final Repositories repositories;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link DomainObjectMerger} for the given {@link Repositories} and {@link ConversionService}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	@Autowired
	public DomainObjectMerger(Repositories repositories, ConversionService conversionService) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.repositories = repositories;
		this.conversionService = conversionService;
	}

	/**
	 * Merges the given target object into the source one.
	 * 
	 * @param from can be {@literal null}.
	 * @param target can be {@literal null}.
	 * @param nullPolicy how to handle {@literal null} values in the source object.
	 */
	public void merge(Object from, Object target, final NullHandlingPolicy nullPolicy) {

		if (from == null || target == null) {
			return;
		}

		final BeanWrapper<Object> fromWrapper = BeanWrapper.create(from, conversionService);
		final BeanWrapper<Object> targetWrapper = BeanWrapper.create(target, conversionService);
		final PersistentEntity<?, ?> entity = repositories.getPersistentEntity(target.getClass());

		entity.doWithProperties(new SimplePropertyHandler() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.SimplePropertyHandler#doWithPersistentProperty(org.springframework.data.mapping.PersistentProperty)
			 */
			@Override
			public void doWithPersistentProperty(PersistentProperty<?> persistentProperty) {

				Object sourceValue = fromWrapper.getProperty(persistentProperty);
				Object targetValue = targetWrapper.getProperty(persistentProperty);

				if (entity.isIdProperty(persistentProperty)) {
					return;
				}

				if (ObjectUtils.nullSafeEquals(sourceValue, targetValue)) {
					return;
				}

				if (nullPolicy == APPLY_NULLS || sourceValue != null) {
					targetWrapper.setProperty(persistentProperty, sourceValue);
				}
			}
		});

		entity.doWithAssociations(new SimpleAssociationHandler() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
			 */
			@Override
			public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {

				PersistentProperty<?> persistentProperty = association.getInverse();
				Object fromVal = fromWrapper.getProperty(persistentProperty);

				if (!isNullOrEmpty(fromVal) && !fromVal.equals(targetWrapper.getProperty(persistentProperty))) {
					targetWrapper.setProperty(persistentProperty, fromVal);
				}
			}
		});
	}

	/**
	 * Returns whether the given source is {@literal null} or considered empty, which means it's an {@link Iterable} or
	 * array and doesn't have any elements.
	 * 
	 * @param source can be {@literal null}.
	 * @return
	 */
	static boolean isNullOrEmpty(Object source) {

		if (source == null) {
			return true;
		}

		if (source instanceof Iterable) {
			return !((Iterable<?>) source).iterator().hasNext();
		}

		if (ObjectUtils.isArray(source)) {
			return ObjectUtils.isEmpty((Object[]) source);
		}

		return false;
	}

	/**
	 * Strategy to express whether {@literal null} values should be ignored or set on the target domain object.
	 */
	public static enum NullHandlingPolicy {
		APPLY_NULLS, IGNORE_NULLS;
	}
}
