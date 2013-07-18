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
package org.springframework.data.rest.core.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.domain.jpa.CreditCard;
import org.springframework.data.rest.core.domain.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link ResourceMappings}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
public class ResourceMappingsIntegrationTest {

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
}
