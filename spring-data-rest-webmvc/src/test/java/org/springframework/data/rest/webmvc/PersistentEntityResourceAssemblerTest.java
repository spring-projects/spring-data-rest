/*
 * Copyright 2013-2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.projection.ProxyProjectionFactory;
import org.springframework.data.rest.webmvc.jpa.*;
import org.springframework.data.rest.webmvc.support.PersistentEntityProjector;
import org.springframework.hateoas.EntityLinks;
import org.springframework.test.context.ContextConfiguration;


/**
 * @author Evgeniy Zakharchenko
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
public class PersistentEntityResourceAssemblerTest extends AbstractControllerIntegrationTests {

	@Autowired
	TestDataPopulator populator;

	@Autowired
	AuthorRepository authorRepository;

	@Autowired
	OrderRepository orderRepository;

	@Autowired
	Repositories repositories;

	@Autowired
	EntityLinks entityLinks;

	@Autowired
	RepositoryRestConfiguration config;

	@Autowired
	ListableBeanFactory beanFactory;

	@Autowired
	ResourceMappings resourceMappings;

	PersistentEntityResourceAssembler assembler;


	@PostConstruct
	public void init() {
		// creating real world assembler with a true Projector
		PersistentEntityProjector projector = new PersistentEntityProjector(
				config.projectionConfiguration(),
				new ProxyProjectionFactory(beanFactory),
				null,
				resourceMappings);

		// let's make the projector behave as the Person entity has an excerpt
		projector = spy(projector);
		when(projector.hasExcerptProjection(Person.class)).thenReturn(true);

		assembler = new PersistentEntityResourceAssembler(repositories, entityLinks, projector, resourceMappings);
	}

	@Before
	public void beforeEach() throws Exception {
		populator.populateRepositories();
	}

	/**
	 * @see DATAREST-464
	 * @throws Exception
	 */
	@Test
	public void embeddedManyToSomethingAssociationsShouldBeRepresentedAsPersistentEntityResource() throws Exception {
		Author author = authorRepository.findAll().iterator().next();
		PersistentEntityResource persistentEntityResource = assembler.toResource(author);
		assertThat(persistentEntityResource, is(notNullValue()));
		List<?> embeddedBooksList = (List<?>) persistentEntityResource.getEmbeddeds().getContent().iterator().next().getValue();
		Object firstBook = embeddedBooksList.iterator().next();
		assertThat(firstBook, is(instanceOf(PersistentEntityResource.class)));
	}


	/**
	 * @see DATAREST-464
	 * @throws Exception
	 */
	@Test
	public void embeddedOneToSomethingAssociationsShouldBeRepresentedAsPersistentEntityResource() throws Exception {
		Order order = orderRepository.findAll().iterator().next();
		PersistentEntityResource persistentEntityResource = assembler.toResource(order);
		assertThat(persistentEntityResource, is(notNullValue()));
		Object embeddedItem = persistentEntityResource.getEmbeddeds().getContent().iterator().next().getValue();
		assertThat(embeddedItem, is(instanceOf(PersistentEntityResource.class)));
	}
}