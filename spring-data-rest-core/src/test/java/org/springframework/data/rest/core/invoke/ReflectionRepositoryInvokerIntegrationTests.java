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
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.AbstractIntegrationTests;
import org.springframework.data.rest.core.domain.jpa.Author;
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
	@Autowired EntityManager em;

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

	/**
	 * @see DATAREST-325
	 */
	@Test
	public void invokesMethodOnPackageProtectedRepository() throws Exception {

		Object authorRepository = repositories.getRepositoryFor(Author.class);
		RepositoryInformation repositoryInformation = repositories.getRepositoryInformationFor(Author.class);
		Method method = repositoryInformation.getRepositoryInterface().getMethod("findByFirstnameContaining", String.class);

		Map<String, String[]> parameters = Collections.singletonMap("firstname", new String[] { "Oliver" });

		ReflectionRepositoryInvoker invoker = new ReflectionRepositoryInvoker(authorRepository, information,
				conversionService);
		invoker.invokeQueryMethod(method, parameters, null, null);
	}

	/**
	 * @see DATAREST-335, DATAREST-346
	 */
	@Test
	public void invokesOverriddenDeleteMethodCorrectly() {

		MyRepo repository = mock(MyRepo.class);

		MongoRepositoryFactoryBean<MyRepo, Domain, ObjectId> factory = new MongoRepositoryFactoryBean<MyRepo, Domain, ObjectId>();
		factory.setMongoOperations(new MongoTemplate(mock(MongoDbFactory.class)));
		factory.setRepositoryInterface(MyRepo.class);
		factory.setLazyInit(true);
		factory.afterPropertiesSet();

		ObjectId id = new ObjectId();

		ReflectionRepositoryInvoker invoker = new ReflectionRepositoryInvoker(repository,
				factory.getRepositoryInformation(), conversionService);

		// We must assume a non matching type here as clients might provide the raw ID value obtained from the request
		invoker.invokeDelete(id.toString());

		verify((CustomRepo) repository, times(1)).delete(id);
		verify(repository, times(0)).findOne(Matchers.any(ObjectId.class));

	}

	interface MyRepo extends CustomRepo, CrudRepository<Domain, ObjectId> {}

	interface Domain {}

	interface CustomRepo {
		void delete(ObjectId id);
	}
}
