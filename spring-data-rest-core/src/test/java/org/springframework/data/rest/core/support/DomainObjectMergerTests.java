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
package org.springframework.data.rest.core.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.rest.core.support.DomainObjectMerger.NullHandlingPolicy.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.domain.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link DomainObjectMerger}.
 * 
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaRepositoryConfig.class)
public class DomainObjectMergerTests {

	@Autowired ConfigurableApplicationContext context;

	DomainObjectMerger merger;

	@Before
	public void setUp() {
		this.merger = new DomainObjectMerger(new Repositories(context.getBeanFactory()), new DefaultConversionService());
	}

	/**
	 * @see DATAREST-130
	 */
	@Test
	public void mergeNewValue() {

		Person incoming = new Person("Bilbo", "Baggins");
		Person existingDomainObject = new Person("Frodo", "Baggins");

		merger.merge(incoming, existingDomainObject, APPLY_NULLS);

		assertThat(existingDomainObject.getFirstName(), is(incoming.getFirstName()));
		assertThat(existingDomainObject.getLastName(), is(incoming.getLastName()));
	}

	/**
	 * @see DATAREST-130
	 */
	@Test
	public void mergeNullValue() {

		Person incoming = new Person(null, null);
		Person existingDomainObject = new Person("Frodo", "Baggins");

		merger.merge(incoming, existingDomainObject, APPLY_NULLS);

		assertThat(existingDomainObject.getFirstName(), is(incoming.getFirstName()));
		assertThat(existingDomainObject.getLastName(), is(incoming.getLastName()));
	}

	/**
	 * @see DATAREST-327
	 */
	@Test
	public void doesNotMergeEmptyCollectionsForReferences() {

		Person bilbo = new Person("Bilbo", "Baggins");

		Person frodo = new Person("Frodo", "Baggins");
		frodo.setSiblings(Arrays.asList(bilbo));

		merger.merge(new Person("Sam", null), frodo, IGNORE_NULLS);

		assertThat(frodo.getSiblings(), is(not(emptyIterable())));
	}
}
