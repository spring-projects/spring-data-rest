/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.domain.jpa.CreditCard;
import org.springframework.data.rest.core.domain.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link ResourceMappings}.
 * 
 * @author Oliver Gierke
 * @author Greg Trunquist
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
public class ResourceMappingsIntegrationTests {

	@Autowired ListableBeanFactory factory;

	ResourceMappings mappings;

	@Before
	public void setUp() {

		Repositories repositories = new Repositories(factory);
		this.mappings = new ResourceMappings(new RepositoryRestConfiguration(), repositories);
	}

	@Test
	public void detectsAllMappings() {
		assertThat(mappings, is(Matchers.<ResourceMetadata> iterableWithSize(6)));
	}

	@Test
	public void exportsResourceAndSearchesForPersons() {

		ResourceMetadata personMappings = mappings.getMappingFor(Person.class);

		assertThat(personMappings.isExported(), is(true));
		assertThat(personMappings.getSearchResourceMappings().isExported(), is(true));
	}

	@Test
	public void doesNotExportAnyMappingsForHiddenRepository() {

		ResourceMetadata creditCardMapping = mappings.getMappingFor(CreditCard.class);

		assertThat(creditCardMapping.isExported(), is(false));
		assertThat(creditCardMapping.getSearchResourceMappings().isExported(), is(false));
	}

	/**
	 * @see DATAREST-112
	 */
	@Test
	public void usesPropertyNameAsRelForPropertyResourceMapping() {

		Repositories repositories = new Repositories(factory);
		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(Person.class);
		PersistentProperty<?> property = entity.getPersistentProperty("siblings");

		ResourceMetadata metadata = mappings.getMappingFor(Person.class);
		ResourceMapping mapping = metadata.getMappingFor(property);

		assertThat(mapping.getRel(), is("siblings"));
		assertThat(mapping.getPath(), is(new Path("siblings")));
		assertThat(mapping.isExported(), is(true));
	}

	/**
	 * @see DATAREST-111
	 */
	@Test
	public void exposesResourceByPath() {

		assertThat(mappings.exportsTopLevelResourceFor("people"), is(true));
		assertThat(mappings.exportsTopLevelResourceFor("orders"), is(true));

		ResourceMetadata creditCardMapping = mappings.getMappingFor(CreditCard.class);
		assertThat(creditCardMapping, is(notNullValue()));
		assertThat(creditCardMapping.getPath(), is(new Path("creditCards")));
		assertThat(creditCardMapping.isExported(), is(false));
		assertThat(mappings.exportsTopLevelResourceFor("creditCards"), is(false));
	}

	/**
	 * @see DATAREST-107
	 */
	@Test
	public void skipsSearchMethodsNotExported() {

		ResourceMetadata creditCardMetadata = mappings.getMappingFor(CreditCard.class);
		SearchResourceMappings searchResourceMappings = creditCardMetadata.getSearchResourceMappings();

		assertThat(searchResourceMappings, is(Matchers.<MethodResourceMapping> iterableWithSize(0)));

		ResourceMetadata personMetadata = mappings.getMappingFor(Person.class);
		List<String> methodNames = new ArrayList<String>();

		for (MethodResourceMapping method : personMetadata.getSearchResourceMappings()) {
			methodNames.add(method.getMethod().getName());
		}

		assertThat(methodNames, hasSize(3));
		assertThat(methodNames, hasItems("findByFirstName", "findByCreatedGreaterThan", "findByCreatedUsingISO8601Date"));
	}
}
