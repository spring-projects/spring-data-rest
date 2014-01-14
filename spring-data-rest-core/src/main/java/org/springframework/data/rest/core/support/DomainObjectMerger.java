/*
 * Copyright 2012-2013 the original author or authors.
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

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class DomainObjectMerger {

	private final Repositories repositories;
	private final ConversionService conversionService;

	@Autowired
	public DomainObjectMerger(Repositories repositories, ConversionService conversionService) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.repositories = repositories;
		this.conversionService = conversionService;
	}

	public void merge(Object from, Object target) {

		if (null == from || null == target) {
			return;
		}

		final BeanWrapper<?, Object> fromWrapper = BeanWrapper.create(from, conversionService);
		final BeanWrapper<?, Object> targetWrapper = BeanWrapper.create(target, conversionService);

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(target.getClass());

		entity.doWithProperties(new SimplePropertyHandler() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.SimplePropertyHandler#doWithPersistentProperty(org.springframework.data.mapping.PersistentProperty)
			 */
			@Override
			public void doWithPersistentProperty(PersistentProperty persistentProperty) {
				Object fromVal = fromWrapper.getProperty(persistentProperty);
				
				// Support PUTting null property values per DATAREST-130.
				boolean mergeProperty =
						!entity.isIdProperty(persistentProperty)
						&& (fromVal == null || !fromVal.equals(targetWrapper.getProperty(persistentProperty)));
				
				if (mergeProperty) {
					targetWrapper.setProperty(persistentProperty, fromVal);
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
				if (null != fromVal && !fromVal.equals(targetWrapper.getProperty(persistentProperty))) {
					targetWrapper.setProperty(persistentProperty, fromVal);
				}
			}
		});
	}
}
