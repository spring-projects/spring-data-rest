/*
 * Copyright 2013-2017 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.domain.Author;
import org.springframework.data.rest.core.domain.CreditCard;
import org.springframework.data.rest.core.domain.JpaRepositoryConfig;
import org.springframework.data.rest.core.domain.Person;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link RepositoryResourceMappings}.
 * 
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaRepositoryConfig.class)
public class RepositoryResourceMappingsIntegrationTests {

	@Autowired ListableBeanFactory factory;
	@Autowired KeyValueMappingContext mappingContext;

	ResourceMappings mappings;

	@Before
	public void setUp() {

		Repositories repositories = new Repositories(factory);
		this.mappings = new RepositoryResourceMappings(repositories, new PersistentEntities(Arrays.asList(mappingContext)),
				RepositoryDetectionStrategies.DEFAULT, new EvoInflectorRelProvider());
	}

	@Test
	public void detectsAllMappings() {
		assertThat(mappings, is(Matchers.<ResourceMetadata> iterableWithSize(5)));
	}

	@Test
	public void exportsResourceAndSearchesForPersons() {

		ResourceMetadata personMappings = mappings.getMetadataFor(Person.class);

		assertThat(personMappings.isExported(), is(true));
		assertThat(personMappings.getSearchResourceMappings().isExported(), is(true));
	}

	@Test
	public void doesNotExportAnyMappingsForHiddenRepository() {

		ResourceMetadata creditCardMapping = mappings.getMetadataFor(CreditCard.class);

		assertThat(creditCardMapping.isExported(), is(false));
		assertThat(creditCardMapping.getSearchResourceMappings().isExported(), is(false));
	}

	@Test // DATAREST-112
	public void usesPropertyNameAsRelForPropertyResourceMapping() {

		Repositories repositories = new Repositories(factory);
		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(Person.class);
		PersistentProperty<?> property = entity.getPersistentProperty("siblings");

		ResourceMetadata metadata = mappings.getMetadataFor(Person.class);
		ResourceMapping mapping = metadata.getMappingFor(property);

		assertThat(mapping.getRel(), is("siblings"));
		assertThat(mapping.getPath(), is(new Path("siblings")));
		assertThat(mapping.isExported(), is(true));
	}

	@Test // DATAREST-111
	public void exposesResourceByPath() {

		assertThat(mappings.exportsTopLevelResourceFor("people"), is(true));
		assertThat(mappings.exportsTopLevelResourceFor("orders"), is(true));

		ResourceMetadata creditCardMapping = mappings.getMetadataFor(CreditCard.class);
		assertThat(creditCardMapping, is(notNullValue()));
		assertThat(creditCardMapping.getPath(), is(new Path("creditCards")));
		assertThat(creditCardMapping.isExported(), is(false));
		assertThat(mappings.exportsTopLevelResourceFor("creditCards"), is(false));
	}

	@Test // DATAREST-107
	public void skipsSearchMethodsNotExported() {

		ResourceMetadata creditCardMetadata = mappings.getMetadataFor(CreditCard.class);
		SearchResourceMappings searchResourceMappings = creditCardMetadata.getSearchResourceMappings();

		assertThat(searchResourceMappings, is(Matchers.<MethodResourceMapping> iterableWithSize(0)));

		ResourceMetadata personMetadata = mappings.getMetadataFor(Person.class);
		List<String> methodNames = new ArrayList<String>();

		for (MethodResourceMapping method : personMetadata.getSearchResourceMappings()) {
			methodNames.add(method.getMethod().getName());
		}

		assertThat(methodNames, hasSize(2));
		assertThat(methodNames, hasItems("findByFirstName", "findByCreatedGreaterThan"));
	}

	@Test // DATAREST-325
	public void exposesMethodResourceMappingInPackageProtectedButExportedRepo() {

		ResourceMetadata metadata = mappings.getMetadataFor(Author.class);
		assertThat(metadata.isExported(), is(true));

		SearchResourceMappings searchMappings = metadata.getSearchResourceMappings();

		assertThat(searchMappings.isExported(), is(true));
		assertThat(searchMappings.getMappedMethod("findByFirstnameContaining"), is(notNullValue()));

		for (MethodResourceMapping methodMapping : searchMappings) {

			System.out.println(methodMapping.getMethod().getName());
			assertThat(methodMapping.isExported(), is(true));
		}
	}

	@Test
	public void testname() {

		ResourceMetadata metadata = mappings.getMetadataFor(Person.class);

		PropertyAwareResourceMapping propertyMapping = metadata.getProperty("father-mapped");

		assertThat(propertyMapping.getRel(), is("father"));
		assertThat(propertyMapping.getPath(), is(new Path("father-mapped")));
	}
}
