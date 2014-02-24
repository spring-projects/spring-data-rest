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
package org.springframework.data.rest.webmvc;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.jpa.Address;
import org.springframework.data.rest.webmvc.jpa.AddressRepository;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Integration tests for {@link RepositoryEntityController}.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
public class RepositoryEntityControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryEntityController controller;
	@Autowired AddressRepository repository;

	/**
	 * @see DATAREST-217
	 */
	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void returnsNotFoundForListingEntitiesIfFindAllNotExported() throws Exception {

		repository.save(new Address());

		RootResourceInformation request = getResourceInformation(Address.class);
		controller.getCollectionResource(request, null, null, null);
	}

	/**
	 * @see DATAREST-217
	 */
	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void rejectsEntityCreationIfSaveIsNotExported() throws Exception {

		RootResourceInformation request = getResourceInformation(Address.class);

		controller.postEntity(request, null, null);
	}
}
