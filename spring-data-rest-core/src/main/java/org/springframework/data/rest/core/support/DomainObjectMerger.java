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
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;

/**
 * @author Jon Brisbin
 */
public class DomainObjectMerger {

	private final Repositories repositories;
	private final ConversionService conversionService;

	@Autowired
	public DomainObjectMerger(Repositories repositories, ConversionService conversionService) {
		this.repositories = repositories;
		this.conversionService = conversionService;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void merge(Object from, Object target) {
		if (null == from || null == target) {
			return;
		}
		final BeanWrapper<?, Object> fromWrapper = BeanWrapper.create(from, conversionService);
		final BeanWrapper<?, Object> targetWrapper = BeanWrapper.create(target, conversionService);

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(target.getClass());
		entity.doWithProperties(new PropertyHandler() {
			@Override
			public void doWithPersistentProperty(PersistentProperty persistentProperty) {
				Object fromVal = fromWrapper.getProperty(persistentProperty);
				if (null != fromVal && !fromVal.equals(targetWrapper.getProperty(persistentProperty))) {
					targetWrapper.setProperty(persistentProperty, fromVal);
				}
			}
		});
		entity.doWithAssociations(new AssociationHandler() {
			@Override
			public void doWithAssociation(Association association) {
				PersistentProperty persistentProperty = association.getInverse();
				Object fromVal = fromWrapper.getProperty(persistentProperty);
				if (null != fromVal && !fromVal.equals(targetWrapper.getProperty(persistentProperty))) {
					targetWrapper.setProperty(persistentProperty, fromVal);
				}
			}
		});
	}

}
