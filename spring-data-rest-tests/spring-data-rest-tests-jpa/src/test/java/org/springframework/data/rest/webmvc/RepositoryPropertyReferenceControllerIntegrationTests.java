/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.jpa.Book;
import org.springframework.data.rest.webmvc.jpa.BookRepository;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.TestDataPopulator;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
class RepositoryPropertyReferenceControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryPropertyReferenceController controller;
	@Autowired TestDataPopulator populator;
	@Autowired BookRepository books;

	PersistentEntityResourceAssembler assembler;
	RootResourceInformation information;

	@BeforeEach
	void setUp() {

		this.assembler = mock(PersistentEntityResourceAssembler.class);
		this.information = getResourceInformation(Book.class);
		this.populator.populateRepositories();
	}

	@Test
	void exposesResourceForCustomizedPropertyResourcePath() throws Exception {

		Book book = books.findAll().iterator().next();

		assertThat(controller.followPropertyReference(information, book.id, "creators", assembler).getStatusCode())
				.isEqualTo(HttpStatus.OK);
	}

	@Test
	void doesNotExposeOriginalPathIfPropertyResourcePathIsCustomized() {

		Book book = books.findAll().iterator().next();

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.followPropertyReference(information, book.id, "authors", assembler));
	}
}
