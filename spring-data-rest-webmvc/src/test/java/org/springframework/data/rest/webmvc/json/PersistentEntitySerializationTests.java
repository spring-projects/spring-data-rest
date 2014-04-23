/*
 * Copyright 2012-2014 the original author or authors.
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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.WebTestUtils;
import org.springframework.data.rest.webmvc.jpa.LineItem;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.OrderRepository;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.PersonRepository;
import org.springframework.data.rest.webmvc.mongodb.Address;
import org.springframework.data.rest.webmvc.mongodb.User;
import org.springframework.data.rest.webmvc.util.TestUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.hal.HalLinkDiscoverer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for entity (de)serialization.
 * 
 * @author Jon Brisbin
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoryTestsConfig.class)
@Transactional
public class PersistentEntitySerializationTests {

	private static final String PERSON_JSON_IN = "{\"firstName\": \"John\",\"lastName\": \"Doe\"}";

	@Autowired ObjectMapper mapper;
	@Autowired Repositories repositories;
	@Autowired PersonRepository people;
	@Autowired OrderRepository orders;

	LinkDiscoverer linkDiscoverer;

	@Before
	public void setUp() {
		WebTestUtils.initWebTest();
		linkDiscoverer = new HalLinkDiscoverer();
	}

	@Test
	public void deserializesPersonEntity() throws IOException {

		Person p = mapper.readValue(PERSON_JSON_IN, Person.class);

		assertThat(p.getFirstName(), is("John"));
		assertThat(p.getLastName(), is("Doe"));
		assertThat(p.getSiblings(), is(Collections.EMPTY_LIST));
	}

	/**
	 * @see DATAREST-238
	 */
	@Test
	public void deserializePersonWithLinks() throws IOException {

		String bilbo = "{\n" + "  \"_links\" : {\n" + "    \"self\" : {\n"
				+ "      \"href\" : \"http://localhost/people/4\"\n" + "    },\n" + "    \"siblings\" : {\n"
				+ "      \"href\" : \"http://localhost/people/4/siblings\"\n" + "    },\n" + "    \"father\" : {\n"
				+ "      \"href\" : \"http://localhost/people/4/father\"\n" + "    }\n" + "  },\n"
				+ "  \"firstName\" : \"Bilbo\",\n" + "  \"lastName\" : \"Baggins\",\n"
				+ "  \"created\" : \"2014-01-31T21:07:45.574+0000\"\n" + "}\n";

		Person p = mapper.readValue(bilbo, Person.class);
		assertThat(p.getFirstName(), equalTo("Bilbo"));
		assertThat(p.getLastName(), equalTo("Baggins"));
	}

	/**
	 * @see DATAREST-238
	 */
	@Test
	public void serializesPersonEntity() throws IOException, InterruptedException {

		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(Person.class);
		Person person = people.save(new Person("John", "Doe"));

		StringWriter writer = new StringWriter();
		mapper.writeValue(writer, PersistentEntityResource.wrap(persistentEntity, person));

		String s = writer.toString();

		Link fatherLink = linkDiscoverer.findLinkWithRel("father", s);
		assertThat(fatherLink.getHref(), endsWith(new UriTemplate("/{id}/father").expand(person.getId()).toString()));

		Link siblingLink = linkDiscoverer.findLinkWithRel("siblings", s);
		assertThat(siblingLink.getHref(), endsWith(new UriTemplate("/{id}/siblings").expand(person.getId()).toString()));
	}

	/**
	 * @see DATAREST-248
	 */
	@Test
	public void deserializesPersonWithLinkToOtherPersonCorrectly() throws Exception {

		Person father = people.save(new Person("John", "Doe"));

		String child = String.format("{ \"firstName\" : \"Bilbo\", \"father\" : \"/persons/%s\"}", father.getId());
		Person result = mapper.readValue(child, Person.class);

		assertThat(result.getFather(), is(father));
	}

	/**
	 * @see DATAREST-248
	 */
	@Test
	public void deserializesPersonWithLinkToOtherPersonsCorrectly() throws Exception {

		Person firstSibling = people.save(new Person("John", "Doe"));
		Person secondSibling = people.save(new Person("Dave", "Doe"));

		String child = String.format("{ \"firstName\" : \"Bilbo\", \"siblings\" : [\"/persons/%s\", \"/persons/%s\"]}",
				firstSibling.getId(), secondSibling.getId());
		Person result = mapper.readValue(child, Person.class);

		assertThat(result.getSiblings(), hasItems(firstSibling, secondSibling));
	}

	/**
	 * @see DATAREST-248
	 */
	@Test
	public void deserializesEmbeddedAssociationsCorrectly() throws Exception {

		String content = TestUtils.readFileFromClasspath("order.json");

		Order order = mapper.readValue(content, Order.class);
		assertThat(order.getLineItems(), hasSize(2));
	}

	/**
	 * @see DATAREST-250
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void serializesEmbeddedReferencesCorrectly() throws Exception {

		User user = new User();
		user.address = new Address();
		user.address.street = "Street";

		PersistentEntityResource<User> userResource = new PersistentEntityResource<User>(
				repositories.getPersistentEntity(User.class), user);

		PagedResources<PersistentEntityResource<User>> persistentEntityResource = new PagedResources<PersistentEntityResource<User>>(
				Arrays.asList(userResource), new PageMetadata(1, 0, 10));

		assertThat(
				mapper.writeValueAsString(persistentEntityResource),
				is("{\"_embedded\":{\"users\":[{\"address\":{\"street\":\"Street\"}}]},\"page\":{\"size\":1,\"totalElements\":10,\"totalPages\":10,\"number\":0}}"));
	}

	/**
	 * @see DATAREST-250
	 */
	@Test
	public void serializesReferencesWithinPagedResourceCorrectly() throws Exception {

		Person creator = new Person("Dave", "Matthews");

		Order order = new Order(creator);
		ReflectionTestUtils.setField(order, "id", 1L);
		order.add(new LineItem("first"));
		order.add(new LineItem("second"));

		PersistentEntityResource<Order> orderResource = new PersistentEntityResource<Order>(
				repositories.getPersistentEntity(Order.class), order);

		@SuppressWarnings("unchecked")
		PagedResources<PersistentEntityResource<Order>> persistentEntityResource = new PagedResources<PersistentEntityResource<Order>>(
				Arrays.asList(orderResource), new PageMetadata(1, 0, 10));

		assertThat(mapper.writeValueAsString(persistentEntityResource),
				is("{\"_embedded\":{\"orders\":[{\"lineItems\":[{\"name\":\"first\"},{\"name\":\"second\"}],\"price\":2.5"
						+ ",\"_links\":{\"creator\":{\"href\":\"http://localhost/orders/1/creator\"}}}]},\""
						+ "page\":{\"size\":1,\"totalElements\":10,\"totalPages\":10,\"number\":0}}"));
	}
}
