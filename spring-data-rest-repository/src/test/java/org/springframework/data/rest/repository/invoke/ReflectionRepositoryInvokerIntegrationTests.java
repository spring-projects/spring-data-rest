/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.repository.invoke;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.RepositoryTestsConfig;
import org.springframework.data.rest.repository.domain.jpa.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link ReflectionRepositoryInvoker}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoryTestsConfig.class)
public class ReflectionRepositoryInvokerIntegrationTests {

	@Autowired Repositories repositories;
	@Autowired ConversionService conversionService;

	Object repository;
	RepositoryInformation information;
	RepositoryInvoker invoker;

	@Before
	public void setUp() {

		information = repositories.getRepositoryInformationFor(Person.class);
		repository = repositories.getRepositoryFor(Person.class);
		invoker = new ReflectionRepositoryInvoker(repository, information, conversionService);
	}

	@Test
	public void invokesFindOneWithStringIdCorrectly() {

		Object result = invoker.invokeFindOne("1");
		assertThat(result, is(instanceOf(Person.class)));
	}

	@Test
	public void invokesFindAllWithoutPageableCorrectly() {

		Iterable<Object> result = invoker.invokeFindAll((Pageable) null);
		assertThat(result, is(instanceOf(List.class)));
	}

	@Test
	public void invokesFindAllWithPageableCorrectly() {

		Iterable<Object> result = invoker.invokeFindAll(new PageRequest(0, 10));
		assertThat(result, is(instanceOf(Page.class)));
	}
}
