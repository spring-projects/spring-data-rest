/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.data.rest.webmvc.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ValueConstants;

/**
 * An ETag validator that verifies concurrency issues and the validity of the Etag/ If-Match header values to implement
 * optimistic locking.
 * 
 * @author Pablo Lozano
 */

public class EtagValidator {

	private final ConversionService conversionService;

	public EtagValidator(ConversionService conversionService) {
		this.conversionService = conversionService;
		Assert.notNull(conversionService, "Conversion service must not be null");
	}

	/**
	 * Compares the given eTag with the entity's version property, if there are different an
	 * OptimisticLockingFailureException is thrown.
	 *
	 * @param requestEtag
	 * @param resourceInformation
	 * @param domainObject
	 * @return
	 */
	public void validateEtag(String requestEtag, RootResourceInformation resourceInformation, Object domainObject) {

		if (requestEtag != null && !requestEtag.equals(ValueConstants.DEFAULT_NONE)) {
			final String entityEtag = getVersionInformation(resourceInformation.getPersistentEntity(), domainObject);
			if (!requestEtag.equals(entityEtag)) {
				throw new OptimisticLockingFailureException("Invalid If-Match version provided, the resource has gone thru"
						+ " changes after resource's request");
			}
		}
	}

	/**
	 * Sets the Etag to the header, if the domain object does not contain a Version property it will return and leave the
	 * headers as is.
	 *
	 * @param headers
	 * @param persistentEntityResource
	 */
	public void addEtagHeader(HttpHeaders headers, PersistentEntityResource persistentEntityResource) {

		String version = getVersionInformation(persistentEntityResource.getPersistentEntity(),
				persistentEntityResource.getContent());
		if (version != null) {
			headers.setETag(version);
		}
	}

	/**
	 * Returns the quoted version property of a domain object, returns null if it doesn't contains the property
	 *
	 * @param persistentEntity
	 * @param domainObject
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private String getVersionInformation(PersistentEntity persistentEntity, Object domainObject) {

		if (persistentEntity.hasVersionProperty()) {

			BeanWrapper<Object> beanWrapper = BeanWrapper.create(domainObject, conversionService);
			Object version = beanWrapper.getProperty(persistentEntity.getVersionProperty());
			return "\"" + version.toString() + "\"";
		}
		return null;
	}
	
}
