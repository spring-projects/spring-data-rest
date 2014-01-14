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
package org.springframework.data.rest.core.invoke;


import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.AbstractIntegrationTests;
import org.springframework.data.rest.core.domain.jpa.Agent;
import org.springframework.data.rest.core.domain.jpa.AgentRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * Integration tests for {@link PagingAndSortingRepository}.
 * 
 * @author Nick Weedon
 */
public class PagingAndSortingRepositoryInvokerIntegrationTests extends AbstractIntegrationTests {

	@Autowired Repositories repositories;
	@Autowired ConversionService conversionService;
	@Autowired AgentRepository repository;

	RepositoryInformation information;
	RepositoryInvoker invoker;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {

		information = repositories.getRepositoryInformationFor(Agent.class);
		Object repo = repository;
		invoker = new PagingAndSortingRepositoryInvoker((PagingAndSortingRepository<Object, Serializable>)repo, information, conversionService);
		if(repository.count() == 0) {
			repository.save(new Agent("Smith"));
			repository.save(new Agent("99"));
			repository.save(new Agent("007"));
			repository.save(new Agent("Powers"));
		}
	}

	/**
	 * @see DATAREST-216
	 */
	@Test(expected=AuthenticationCredentialsNotFoundException.class)
	public void invokesFindOneWithStringIdCorrectly() {
		invoker.invokeFindOne(new Long(2));
	}

}
