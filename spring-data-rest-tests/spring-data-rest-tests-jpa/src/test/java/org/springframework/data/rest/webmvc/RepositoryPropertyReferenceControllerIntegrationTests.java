/*
 * Copyright 2015-present the original author or authors.
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
import org.mockito.Answers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.jpa.Author;
import org.springframework.data.rest.webmvc.jpa.AuthorRepository;
import org.springframework.data.rest.webmvc.jpa.Book;
import org.springframework.data.rest.webmvc.jpa.BookRepository;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.TestDataPopulator;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Oliver Gierke
 * @author Steve Rutherford
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
class RepositoryPropertyReferenceControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryPropertyReferenceController controller;
	@Autowired TestDataPopulator populator;
	@Autowired BookRepository books;
	@Autowired AuthorRepository authors;
	@Autowired Repositories repos;

	RepresentationModelAssemblers assembler = mock(RepresentationModelAssemblers.class,
			withSettings().defaultAnswer(Answers.RETURNS_MOCKS));
	RootResourceInformation bookInformation;
	RootResourceInformation authorInformation;

	@BeforeEach
	void setUp() {
		this.bookInformation = getResourceInformation(Book.class);
		this.authorInformation = getResourceInformation(Author.class);
		this.populator.populateRepositories();
	}

	@Test
	void exposesResourceForCustomizedPropertyResourcePath() throws Exception {

		Book book = books.findAll().iterator().next();

		assertThat(controller.followPropertyReference(bookInformation, book.id, "creators", assembler).getStatusCode())
				.isEqualTo(HttpStatus.OK);
	}

	@Test
	void doesNotExposeOriginalPathIfPropertyResourcePathIsCustomized() {

		Book book = books.findAll().iterator().next();

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.followPropertyReference(bookInformation, book.id, "authors", assembler));
	}

	/**
	 * GH-1810: Writing to the inverse (mappedBy) side of a JPA association must be rejected with HTTP 405.
	 *
	 * <p>{@code Author.books} is annotated with {@code @ManyToMany(mappedBy = "authors")}, making it the inverse side.
	 * JPA ignores saves on the inverse side, so SDR must reject such requests rather than silently returning 204 with no
	 * database effect.
	 */
	@Test
	void rejectsPutOnInverseSideAssociationWithMethodNotAllowed() {

		Long authorId = idOf(authors.findAll().iterator().next());
		Long bookId = books.findAll().iterator().next().id;

		// Attempt to PUT to /authors/{id}/books — the inverse (mappedBy) side
		CollectionModel<Object> payload = CollectionModel.empty(Link.of("/books/" + bookId));

		assertThatExceptionOfType(RepositoryPropertyReferenceController.HttpRequestMethodNotSupportedException.class)
				.isThrownBy(() -> controller.createPropertyReference(authorInformation, HttpMethod.PUT, payload, authorId,
						"books"));
	}

	/**
	 * GH-1810: POST to the inverse (mappedBy) side of a JPA association must also be rejected with HTTP 405.
	 */
	@Test
	void rejectsPostOnInverseSideAssociationWithMethodNotAllowed() {

		Long authorId = idOf(authors.findAll().iterator().next());
		Long bookId = books.findAll().iterator().next().id;

		CollectionModel<Object> payload = CollectionModel.empty(Link.of("/books/" + bookId));

		assertThatExceptionOfType(RepositoryPropertyReferenceController.HttpRequestMethodNotSupportedException.class)
				.isThrownBy(() -> controller.createPropertyReference(authorInformation, HttpMethod.POST, payload, authorId,
						"books"));
	}

	/**
	 * GH-1810: GET on the inverse (mappedBy) side must still be allowed — only writes are rejected.
	 */
	@Test
	void allowsGetOnInverseSideAssociation() throws Exception {

		Long authorId = idOf(authors.findAll().iterator().next());

		assertThat(controller.followPropertyReference(authorInformation, authorId, "books", assembler).getStatusCode())
				.isEqualTo(HttpStatus.OK);
	}

	/**
	 * GH-1810: Writing to the owning side of a JPA association must still work normally.
	 *
	 * <p>{@code Book.authors} (exposed as "creators") is the owning side of the {@code @ManyToMany} relationship and
	 * must continue to accept PUT/POST requests.
	 */
	@Test
	void allowsPutOnOwningSideAssociation() throws Exception {

		Long bookId = books.findAll().iterator().next().id;
		Long authorId = idOf(authors.findAll().iterator().next());

		CollectionModel<Object> payload = CollectionModel.empty(Link.of("/authors/" + authorId));

		// Should not throw — owning side writes are valid
		assertThatCode(() -> controller.createPropertyReference(bookInformation, HttpMethod.PUT, payload, bookId,
				"creators")).doesNotThrowAnyException();
	}

	/**
	 * Returns the identifier of the given entity using the Spring Data {@link Repositories} infrastructure, avoiding
	 * direct field access on package-private fields.
	 */
	private Long idOf(Object entity) {
		return (Long) repos.getPersistentEntity(entity.getClass())
				.getIdentifierAccessor(entity)
				.getRequiredIdentifier();
	}
}
