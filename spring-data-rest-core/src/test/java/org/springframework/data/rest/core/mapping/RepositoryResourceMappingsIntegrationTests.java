/*
 * Copyright 2013-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.domain.Author;
import org.springframework.data.rest.core.domain.CreditCard;
import org.springframework.data.rest.core.domain.JpaRepositoryConfig;
import org.springframework.data.rest.core.domain.Person;
import org.springframework.data.rest.core.domain.Profile;
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
	@Autowired KeyValueMappingContext<?, ?> mappingContext;

	ResourceMappings mappings;

	@Before
	public void setUp() {

		mappingContext.getPersistentEntity(Profile.class);

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration(new ProjectionDefinitionConfiguration(),
				new MetadataConfiguration(), mock(EnumTranslationConfiguration.class));

		Repositories repositories = new Repositories(factory);
		this.mappings = new RepositoryResourceMappings(repositories, new PersistentEntities(Arrays.asList(mappingContext)),
				configuration);
	}

	@Test
	public void detectsAllMappings() {
		assertThat(mappings).hasSize(5);
	}

	@Test
	public void exportsResourceAndSearchesForPersons() {

		ResourceMetadata personMappings = mappings.getMetadataFor(Person.class);

		assertThat(personMappings.isExported()).isTrue();
		assertThat(personMappings.getSearchResourceMappings().isExported()).isTrue();
	}

	@Test
	public void doesNotExportAnyMappingsForHiddenRepository() {

		ResourceMetadata creditCardMapping = mappings.getMetadataFor(CreditCard.class);

		assertThat(creditCardMapping.isExported()).isFalse();
		assertThat(creditCardMapping.getSearchResourceMappings().isExported()).isFalse();
	}

	@Test // DATAREST-112
	public void usesPropertyNameAsRelForPropertyResourceMapping() {

		Repositories repositories = new Repositories(factory);
		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(Person.class);
		PersistentProperty<?> property = entity.getRequiredPersistentProperty("siblings");

		ResourceMetadata metadata = mappings.getMetadataFor(Person.class);
		ResourceMapping mapping = metadata.getMappingFor(property);

		assertThat(mapping.getRel()).isEqualTo("siblings");
		assertThat(mapping.getPath()).isEqualTo(new Path("siblings"));
		assertThat(mapping.isExported()).isTrue();
	}

	@Test // DATAREST-111
	public void exposesResourceByPath() {

		assertThat(mappings.exportsTopLevelResourceFor("people")).isTrue();
		assertThat(mappings.exportsTopLevelResourceFor("orders")).isTrue();

		ResourceMetadata creditCardMapping = mappings.getMetadataFor(CreditCard.class);
		assertThat(creditCardMapping).isNotNull();
		assertThat(creditCardMapping.getPath()).isEqualTo(new Path("creditCards"));
		assertThat(creditCardMapping.isExported()).isFalse();
		assertThat(mappings.exportsTopLevelResourceFor("creditCards")).isFalse();
	}

	@Test // DATAREST-107
	public void skipsSearchMethodsNotExported() {

		ResourceMetadata creditCardMetadata = mappings.getMetadataFor(CreditCard.class);
		SearchResourceMappings searchResourceMappings = creditCardMetadata.getSearchResourceMappings();

		assertThat(searchResourceMappings).isEmpty();

		ResourceMetadata personMetadata = mappings.getMetadataFor(Person.class);
		List<String> methodNames = new ArrayList<String>();

		for (MethodResourceMapping method : personMetadata.getSearchResourceMappings()) {
			methodNames.add(method.getMethod().getName());
		}

		assertThat(methodNames).hasSize(2);
		assertThat(methodNames).contains("findByFirstName", "findByCreatedGreaterThan");
	}

	@Test // DATAREST-325
	public void exposesMethodResourceMappingInPackageProtectedButExportedRepo() {

		ResourceMetadata metadata = mappings.getMetadataFor(Author.class);
		assertThat(metadata.isExported()).isTrue();

		SearchResourceMappings searchMappings = metadata.getSearchResourceMappings();

		assertThat(searchMappings.isExported()).isTrue();
		assertThat(searchMappings.getMappedMethod("findByFirstnameContaining")).isNotNull();

		for (MethodResourceMapping methodMapping : searchMappings) {

			System.out.println(methodMapping.getMethod().getName());
			assertThat(methodMapping.isExported()).isTrue();
		}
	}

	@Test
	public void testname() {

		ResourceMetadata metadata = mappings.getMetadataFor(Person.class);

		PropertyAwareResourceMapping propertyMapping = metadata.getProperty("father-mapped");

		assertThat(propertyMapping.getRel()).isEqualTo("father");
		assertThat(propertyMapping.getPath()).isEqualTo(new Path("father-mapped"));
	}
}
