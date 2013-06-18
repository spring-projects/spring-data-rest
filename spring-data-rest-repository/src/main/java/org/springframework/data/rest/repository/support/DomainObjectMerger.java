package org.springframework.data.rest.repository.support;

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
