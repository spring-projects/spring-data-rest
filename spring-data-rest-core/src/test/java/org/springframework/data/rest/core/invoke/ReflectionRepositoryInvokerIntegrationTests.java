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
package org.springframework.data.rest.core.invoke;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.AbstractIntegrationTests;
import org.springframework.data.rest.core.domain.jpa.Order;
import org.springframework.data.rest.core.domain.jpa.OrderRepository;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.data.rest.core.domain.jpa.PersonRepository;

/**
 * Integration tests for {@link ReflectionRepositoryInvoker}.
 * 
 * @author Oliver Gierke
 */
public class ReflectionRepositoryInvokerIntegrationTests extends AbstractIntegrationTests {

	@Autowired Repositories repositories;
	@Autowired ConversionService conversionService;
	@Autowired PersonRepository repository;
	@Autowired OrderRepository orderRepository;

	RepositoryInformation information;
	RepositoryInvoker invoker;

	@Before
	public void setUp() {

		information = repositories.getRepositoryInformationFor(Person.class);
		invoker = new ReflectionRepositoryInvoker(repository, information, conversionService);
	}

	@Test
	public void invokesFindOneWithStringIdCorrectly() {

		Person person = repository.findAll().iterator().next();
		assertThat(person, is(notNullValue()));

		Object result = invoker.invokeFindOne(person.getId().toString());
		assertThat(result, is(instanceOf(Person.class)));
	}

	@Test
	public void invokesFindAllWithoutPageableCorrectly() {

		Iterable<Object> result = invoker.invokeFindAll((Pageable) null);
		assertThat(result, is(instanceOf(Page.class)));
	}

	@Test
	public void invokesFindAllWithPageableCorrectly() {

		Iterable<Object> result = invoker.invokeFindAll(new PageRequest(0, 10));
		assertThat(result, is(instanceOf(Page.class)));
	}

	@Test
	public void fallsBackToPlainFindAllIfRepositoryIsNotPaging() {

		ReflectionRepositoryInvoker invoker = new ReflectionRepositoryInvoker(orderRepository,
				repositories.getRepositoryInformationFor(Order.class), conversionService);
		Iterable<Object> result = invoker.invokeFindAll(new PageRequest(0, 10));

		assertThat(result, is(instanceOf(List.class)));
	}

	@Test
	public void invokesQueryMethod() throws Exception {

		HashMap<String, String[]> parameters = new HashMap<String, String[]>();
		parameters.put("firstName", new String[] { "John" });

		Method method = PersonRepository.class.getMethod("findByFirstName", String.class, Pageable.class);
		Object result = invoker.invokeQueryMethod(method, parameters, null, null);

		assertThat(result, is(instanceOf(Page.class)));
	}

	@Test
	public void considersFormattingAnnotationsOnQueryMethodParameters() throws Exception {

		HashMap<String, String[]> parameters = new HashMap<String, String[]>();
		parameters.put("date", new String[] { "2013-07-18T10:49:00.000+02:00" });

		Method method = PersonRepository.class.getMethod("findByCreatedUsingISO8601Date", Date.class, Pageable.class);
		Object result = invoker.invokeQueryMethod(method, parameters, null, null);

		assertThat(result, is(instanceOf(Page.class)));
		Page<?> page = (Page<?>) result;
		assertThat(page.getNumberOfElements(), is(1));
	}
}
