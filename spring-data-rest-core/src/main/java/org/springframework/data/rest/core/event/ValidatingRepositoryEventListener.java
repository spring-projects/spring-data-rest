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
package org.springframework.data.rest.core.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.ValidationErrors;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * {@link org.springframework.context.ApplicationListener} implementation that dispatches {@link RepositoryEvent}s to a
 * specific {@link Validator}.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class ValidatingRepositoryEventListener extends AbstractRepositoryEventListener<Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidatingRepositoryEventListener.class);

	private final ObjectFactory<Repositories> repositoriesFactory;
	private final MultiValueMap<String, Validator> validators;

	/**
	 * Creates a new {@link ValidatingRepositoryEventListener} using the given repositories.
	 * 
	 * @param repositoriesFactory must not be {@literal null}.
	 */
	public ValidatingRepositoryEventListener(ObjectFactory<Repositories> repositoriesFactory) {

		Assert.notNull(repositoriesFactory, "Repositories must not be null!");

		this.repositoriesFactory = repositoriesFactory;
		this.validators = new LinkedMultiValueMap<String, Validator>();
	}

	/**
	 * Assign a Map of {@link Validator}s that are assigned to the various {@link RepositoryEvent}s.
	 * 
	 * @param validators A Map of Validators to wire.
	 * @return @this
	 */
	public ValidatingRepositoryEventListener setValidators(Map<String, Collection<Validator>> validators) {

		for (Map.Entry<String, Collection<Validator>> entry : validators.entrySet()) {
			this.validators.put(entry.getKey(), new ArrayList<Validator>(entry.getValue()));
		}

		return this;
	}

	/**
	 * Add a {@link Validator} that will be triggered on the given event.
	 * 
	 * @param event The event to listen for.
	 * @param validator The Validator to execute when that event fires.
	 * @return @this
	 */
	public ValidatingRepositoryEventListener addValidator(String event, Validator validator) {
		validators.add(event, validator);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.event.AbstractRepositoryEventListener#onBeforeCreate(java.lang.Object)
	 */
	@Override
	protected void onBeforeCreate(Object entity) {
		validate("beforeCreate", entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.event.AbstractRepositoryEventListener#onAfterCreate(java.lang.Object)
	 */
	@Override
	protected void onAfterCreate(Object entity) {
		validate("afterCreate", entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.event.AbstractRepositoryEventListener#onBeforeSave(java.lang.Object)
	 */
	@Override
	protected void onBeforeSave(Object entity) {
		validate("beforeSave", entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.event.AbstractRepositoryEventListener#onAfterSave(java.lang.Object)
	 */
	@Override
	protected void onAfterSave(Object entity) {
		validate("afterSave", entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.event.AbstractRepositoryEventListener#onBeforeLinkSave(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void onBeforeLinkSave(Object parent, Object linked) {
		validate("beforeLinkSave", parent);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.event.AbstractRepositoryEventListener#onAfterLinkSave(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void onAfterLinkSave(Object parent, Object linked) {
		validate("afterLinkSave", parent);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.event.AbstractRepositoryEventListener#onBeforeDelete(java.lang.Object)
	 */
	@Override
	protected void onBeforeDelete(Object entity) {
		validate("beforeDelete", entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.event.AbstractRepositoryEventListener#onAfterDelete(java.lang.Object)
	 */
	@Override
	protected void onAfterDelete(Object entity) {
		validate("afterDelete", entity);
	}

	private Errors validate(String event, Object o) {

		if (o == null) {
			return null;
		}

		Class<?> domainType = o.getClass();
		Repositories repositories = repositoriesFactory.getObject();
		Errors errors = new ValidationErrors(domainType.getSimpleName(), o, repositories.getPersistentEntity(domainType));

		for (Validator v : getValidatorsForEvent(event)) {
			if (v.supports(domainType)) {
				LOGGER.debug("{}: {} with {}", event, o, v);
				ValidationUtils.invokeValidator(v, o, errors);
			}
		}

		if (errors.getErrorCount() > 0) {
			throw new RepositoryConstraintViolationException(errors);
		}

		return errors;
	}

	private Collection<Validator> getValidatorsForEvent(String event) {
		Collection<Validator> validators = this.validators.get(event);
		return validators == null ? Collections.<Validator> emptySet() : validators;
	}
}
