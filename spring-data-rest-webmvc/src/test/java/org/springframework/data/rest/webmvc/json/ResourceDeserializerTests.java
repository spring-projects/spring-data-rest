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
package org.springframework.data.rest.webmvc.json;


import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.jpa.Author;
import org.springframework.data.rest.webmvc.jpa.Book;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.support.RepositoryUriResolver;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Unit tests for {@link ResourceDeserializer} registered by {@link PersistentEntityJackson2Module}. 
 * 
 * @author Nick Weedon
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoryTestsConfig.class)
@DirtiesContext
public class ResourceDeserializerTests {
	private SimpleModule module; 
	private static final TypeDescriptor URI_TYPE = TypeDescriptor.valueOf(URI.class);
	private ConditionalGenericConverter mockUriConverter; 

	@Autowired private Repositories repositories;
	@Autowired private ResourceMappings mappings;

	
	@Before
	public void setUp() {
		mockUriConverter = mock(ConditionalGenericConverter.class);
		
		module = new SimpleModule("LongDeserializerModule",
					new Version(1, 0, 0, null, null, null));
		
	}

	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * @throws Exception
	 */
	@Test(expected=HttpMessageNotReadableException.class)
	public void deserializeBookLinkAuthorNoExportFails() throws Exception {
		
		addUrlObject("http://localhost:8080/authors/57", new Author((long)57, "Joshua Bloch"));
		addDerserializer(Book.class);
		deserializeJsonFile("bookBadLinkAuthor.json", Book.class);
	}
	
	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * @throws Exception
	 */
	@Test(expected=HttpMessageNotReadableException.class)
	public void deserializeBookLinkAuthorNoUriObjectFails() throws Exception {

		addDerserializer(Book.class);
		deserializeJsonFile("bookLinkAuthor.json", Book.class);
	}

	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * The emphasis of this test is to ensure that a 'collection' type link
	 * with only one element can be successfully retrieved.
	 * 
	 * @throws Exception
	 */
	@Test
	public void deserializeBookLinkAuthor() throws Exception {
		Author author = new Author((long)57, "Joshua Bloch");
		addUrlObject("http://localhost:8080/authors/57", author);

		addDerserializer(Book.class);
		Book book = deserializeJsonFile("bookLinkAuthor.json", Book.class);

		assertThat(book, is(notNullValue()));
		assertEquals("Effective Java", book.getTitle());
		assertEquals("978-0321356680", book.getIsbn());

		assertThat(book.getAuthors(), is(notNullValue()));
		assertTrue(book.getAuthors().contains(author));
	}
	
	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * The emphasis of this test is to ensure that a 'non-collection' type link
	 * can be successfully retrieved.
	 * 
	 * @throws Exception
	 */
	@Test 
	public void deserializesOrderLinkCreator() throws Exception {
		addDerserializer(Order.class);

		Person person = new Person("Ronald", "McDonald");
		
		addUrlObject("http://localhost:8080/people/2", person);
		
		Order order = deserializeJsonFile("orderLinkCreator.json", Order.class);
		
		assertThat(order, is(notNullValue()));
		// Assert that the ID not was set
		assertThat(order.getId(), is(nullValue()));

		assertEquals("Junk food", order.getOrderName());
		
		// Assert that the 'creator' was set via the '_links' section of the Json request
		assertThat(order.getCreator(), is(notNullValue()));
		assertEquals(person.getFirstName(), order.getCreator().getFirstName());
		assertEquals(person.getLastName(), order.getCreator().getLastName());
	}

	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void deserializeBookLinkAuthors() throws Exception {
		Author joshua = new Author((long)57, "Joshua Bloch");
		addUrlObject("http://localhost:8080/authors/57", joshua);

		Author brian = new Author((long)57, "Brian Goetz");
		addUrlObject("http://localhost:8080/authors/28", brian);

		Author tim = new Author((long)5, "Tim Peierls");
		addUrlObject("http://localhost:8080/authors/5", tim);
		
		addDerserializer(Book.class);
		Book book = deserializeJsonFile("bookLinkAuthors.json", Book.class);

		assertThat(book, is(notNullValue()));
		assertEquals("Java Concurrency in Practice", book.getTitle());
		assertEquals("978-0321349606", book.getIsbn());

		assertThat(book.getAuthors(), is(notNullValue()));
		assertTrue(book.getAuthors().containsAll(Arrays.asList(joshua, brian, tim)));
	}
	
	
	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * This test should pass even if the standard Jackson serializer was used,
	 * its purpose is to guard against regression.
	 * 
	 * @throws Exception
	 */
	@Test
	public void deserializesBookInlineAuthor() throws Exception {
		addDerserializer(Book.class);
		
		Book book = deserializeJsonFile("bookInlineAuthor.json", Book.class);
		
		assertThat(book, is(notNullValue()));
		assertEquals("Thinking in Java", book.getTitle());
		assertEquals("978-0-13-187248-6", book.getIsbn());

		assertThat(book.getAuthors(), is(notNullValue()));
		assertTrue(book.getAuthors().contains(new Author(null, "Bruce Eckel")));
	}

	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * @throws Exception
	 */
	@Test(expected=HttpMessageNotReadableException.class)
	public void deserializeBookLinkAuthorAndInlineAuthorFails() throws Exception {
		
		addUrlObject("http://localhost:8080/authors/57", new Author((long)57, "Joshua Bloch"));
		addDerserializer(Book.class);
		deserializeJsonFile("bookLinkAuthorAndInlineAuthor.json", Book.class);
	}
	
	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void deserializesBookInlineAuthorAndSelfLink() throws Exception {
		addDerserializer(Book.class);
		
		Book book = deserializeJsonFile("bookInlineAuthorAndSelfLink.json", Book.class);
		
		assertThat(book, is(notNullValue()));
		assertEquals("Effective Java", book.getTitle());
		assertEquals("978-0321356680", book.getIsbn());

		assertThat(book.getAuthors(), is(notNullValue()));
		assertTrue(book.getAuthors().contains(new Author(null, "Joshua Bloch")));
	}

	/**
	 * @see DATAREST-117
	 * 
	 * Covers new functionality added in DATAREST-117 as part of rewriting portions
	 * of {@link ResourceDeserializer}.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void deserializesBookInlineAuthorLinkBook() throws Exception {
		addDerserializer(Book.class);

		Book transitiveBook = new Book("978-0321349606", "Java Concurrency in Practice", Collections.EMPTY_LIST);
		addUrlObject("http://localhost:8080/books/7", transitiveBook);
		
		Book book = deserializeJsonFile("bookInlineAuthorLinkBook.json", Book.class);
		
		assertThat(book, is(notNullValue()));
		assertEquals("Effective Java", book.getTitle());
		assertEquals("978-0321356680", book.getIsbn());

		assertThat(book.getAuthors(), is(notNullValue()));
		assertTrue(book.getAuthors().contains(new Author(null, "Joshua Bloch")));

		Author resultAuthor = book.getAuthors().iterator().next();
		assertThat(resultAuthor.getBooks(), is(notNullValue()));
		assertTrue(resultAuthor.getBooks().contains(transitiveBook));
	}
	
	//////////////////////////////////// Helper methods ///////////////////////////////////////////////
	
	private void addUrlObject(String uri, Object object) {
		when(mockUriConverter.convert(URI.create(uri), 
				URI_TYPE, TypeDescriptor.valueOf(object.getClass())))
					.thenReturn(object);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addDerserializer(Class<?> objectType) {
		PersistentEntity<?, ?> pe = repositories.getPersistentEntity(objectType);
		if(pe == null) {
			throw new NullPointerException("No persistenty entity for type: " + objectType.getName());
		}
		module.addDeserializer(objectType, 
				new ResourceDeserializer(pe, mockUriConverter,
						new RepositoryUriResolver(repositories, mappings)));
	}
	
	private <T> T deserializeJsonFile(String file, Class<? extends T> objectType) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(module);		
		
		ClassPathResource classPathFile = new ClassPathResource(file, ResourceDeserializerTests.class);
		
		return (T) objectMapper.readValue(classPathFile.getFile(), objectType);
	}
}
