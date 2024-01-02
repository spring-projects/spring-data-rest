/*
 * Copyright 2012-2024 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.jpa.*;
import org.springframework.data.rest.webmvc.util.TestUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for entity (de)serialization.
 *
 * @author Jon Brisbin
 * @author Greg Turnquist
 * @author Oliver Gierke
 * @author Alex Leigh
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { JpaRepositoryConfig.class, PersistentEntitySerializationTests.TestConfig.class })
@Transactional
class PersistentEntitySerializationTests {

	private static final String PERSON_JSON_IN = "{\"firstName\": \"John\",\"lastName\": \"Doe\"}";

	@Autowired ObjectMapper mapper;
	@Autowired Repositories repositories;
	@Autowired PersonRepository people;
	@Autowired OrderRepository orders;
	@Autowired JpaMetamodelMappingContext context;

	@Configuration
	static class TestConfig extends RepositoryTestsConfig {

		@Bean
		@Override
		public ObjectMapper objectMapper() {

			ObjectMapper objectMapper = super.objectMapper();
			objectMapper.registerModule(new JacksonSerializers(new EnumTranslator(MessageResolver.DEFAULTS_ONLY)));
			return objectMapper;
		}
	}

	LinkDiscoverer discoverer;
	ProjectionFactory projectionFactory;

	@BeforeEach
	void setUp() {

		RequestContextHolder.setRequestAttributes(new ServletWebRequest(new MockHttpServletRequest()));

		this.discoverer = new HalLinkDiscoverer();
		this.projectionFactory = new SpelAwareProxyProjectionFactory();
	}

	@Test
	void deserializesPersonEntity() throws IOException {

		Person p = mapper.readValue(PERSON_JSON_IN, Person.class);

		assertThat(p.getFirstName()).isEqualTo("John");
		assertThat(p.getLastName()).isEqualTo("Doe");
		assertThat(p.getSiblings()).isEqualTo(Collections.EMPTY_LIST);
	}

	@Test // DATAREST-238
	void deserializePersonWithLinks() throws IOException {

		String bilbo = "{\n" + "  \"_links\" : {\n" + "    \"self\" : {\n"
				+ "      \"href\" : \"http://localhost/people/4\"\n" + "    },\n" + "    \"siblings\" : {\n"
				+ "      \"href\" : \"http://localhost/people/4/siblings\"\n" + "    },\n" + "    \"father\" : {\n"
				+ "      \"href\" : \"http://localhost/people/4/father\"\n" + "    }\n" + "  },\n"
				+ "  \"firstName\" : \"Bilbo\",\n" + "  \"lastName\" : \"Baggins\",\n"
				+ "  \"created\" : \"2014-01-31T21:07:45.574+0000\"\n" + "}\n";

		Person p = mapper.readValue(bilbo, Person.class);
		assertThat(p.getFirstName()).isEqualTo("Bilbo");
		assertThat(p.getLastName()).isEqualTo("Baggins");
	}

	@Test // DATAREST-238
	void serializesPersonEntity() throws IOException, InterruptedException {

		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(Person.class);
		Person person = people.save(new Person("John", "Doe"));

		PersistentEntityResource resource = PersistentEntityResource.build(person, persistentEntity).//
				withLink(Link.of("/person/" + person.getId())).build();

		StringWriter writer = new StringWriter();
		mapper.writeValue(writer, resource);

		String s = writer.toString();

		assertThat(discoverer.findLinkWithRel("father", s)) //
				.map(Link::getHref) //
				.hasValueSatisfying(
						it -> assertThat(it).endsWith(new UriTemplate("/{id}/father").expand(person.getId()).toString()));

		assertThat(discoverer.findLinkWithRel("siblings", s)) //
				.map(Link::getHref) //
				.hasValueSatisfying(
						it -> assertThat(it).endsWith(new UriTemplate("/{id}/siblings").expand(person.getId()).toString()));
	}

	@Test // DATAREST-248
	void deserializesPersonWithLinkToOtherPersonCorrectly() throws Exception {

		Person father = people.save(new Person("John", "Doe"));

		String child = String.format("{ \"firstName\" : \"Bilbo\", \"father\" : \"/persons/%s\"}", father.getId());
		Person result = mapper.readValue(child, Person.class);

		assertThat(result.getFather()).isEqualTo(father);
	}

	@Test // DATAREST-248
	void deserializesPersonWithLinkToOtherPersonsCorrectly() throws Exception {

		Person firstSibling = people.save(new Person("John", "Doe"));
		Person secondSibling = people.save(new Person("Dave", "Doe"));

		String child = String.format("{ \"firstName\" : \"Bilbo\", \"siblings\" : [\"/persons/%s\", \"/persons/%s\"]}",
				firstSibling.getId(), secondSibling.getId());
		Person result = mapper.readValue(child, Person.class);

		assertThat(result.getSiblings()).contains(firstSibling, secondSibling);
	}

	@Test // DATAREST-248
	void deserializesEmbeddedAssociationsCorrectly() throws Exception {

		String content = TestUtils.readFileFromClasspath("order.json");

		Order order = mapper.readValue(content, Order.class);
		assertThat(order.getLineItems()).hasSize(2);
	}

	@Test // DATAREST-250
	void serializesReferencesWithinPagedResourceCorrectly() throws Exception {

		Person creator = new Person("Dave", "Matthews");

		Order order = new Order(creator);
		order.add(new LineItem("first"));
		order.add(new LineItem("second"));

		PersistentEntityResource orderResource = PersistentEntityResource.//
				build(order, repositories.getPersistentEntity(Order.class)).//
				withLink(Link.of("/orders/1")).//
				build();

		PagedModel<PersistentEntityResource> persistentEntityResource = PagedModel.of(
				Arrays.asList(orderResource), new PageMetadata(1, 0, 10));

		String result = mapper.writeValueAsString(persistentEntityResource);

		assertThat(JsonPath.<Object> read(result, "$._embedded.orders[*].lineItems")).isNotNull();
	}

	@Test // DATAREST-521
	void serializesLinksForExcerpts() throws Exception {

		Person dave = new Person("Dave", "Matthews");
		dave.setId(1L);

		Person oliver = new Person("Oliver August", "Matthews");
		oliver.setId(2L);
		oliver.setFather(dave);

		UserExcerpt daveExcerpt = projectionFactory.createProjection(UserExcerpt.class, dave);
		EmbeddedWrapper wrapper = new EmbeddedWrappers(false).wrap(daveExcerpt, LinkRelation.of("father"));

		PersistentEntityResource resource = PersistentEntityResource.//
				build(oliver, repositories.getPersistentEntity(Person.class)).//
				withEmbedded(Arrays.asList(wrapper)).//
				build();

		String result = mapper.writeValueAsString(resource);

		assertThat(JsonPath.<Object> read(result, "$._embedded.father[*]._links.self")).isNotNull();
	}

	@Test // DATAREST-521
	void rendersAdditionalLinksRegisteredWithResource() throws Exception {

		Person dave = new Person("Dave", "Matthews");

		PersistentEntityResource resource = PersistentEntityResource.//
				build(dave, repositories.getPersistentEntity(Person.class)).//
				withLink(Link.of("/people/1")).//
				withLink(Link.of("/aditional", "processed")).//
				build();

		String result = mapper.writeValueAsString(resource);

		assertThat(JsonPath.<Object> read(result, "$._links.processed")).isNotNull();
	}

	@Test // DATAREST-697
	void rendersProjectionWithinSimpleResourceCorrectly() throws Exception {

		Person person = new Person("Dave", "Matthews");
		person.setId(1L);

		ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
		PersonSummary projection = factory.createProjection(PersonSummary.class, person);

		String result = mapper.writeValueAsString(EntityModel.of(projection));

		assertThat(JsonPath.<Object> read(result, "$._links.self")).isNotNull();
	}

	@Test // DATAREST-880
	void unwrapsNestedTypeCorrectly() throws Exception {

		CreditCard creditCard = new CreditCard(new CreditCard.CCN("1234123412341234"));

		assertThat(JsonPath.<Object> read(mapper.writeValueAsString(creditCard), "$.ccn")).isEqualTo("1234123412341234");
	}

	@Test // DATAREST-872
	void serializesInheritance() throws Exception {

		Suite suite = new Suite();
		suite.setSuiteCode("Suite 42");

		Dinner dinner = new Dinner();
		dinner.setDinnerCode("STD_DINNER");

		Guest guest = new Guest();
		guest.setRoom(suite);
		guest.addMeal(dinner);

		PersistentEntityResource resource = PersistentEntityResource//
				.build(guest, context.getRequiredPersistentEntity(Guest.class))//
				.withLink(Link.of("/guests/1")).build();

		String result = mapper.writeValueAsString(resource);

		assertThat(JsonPath.<Object> read(result, "$.room.type")).isEqualTo("suite");
		assertThat(JsonPath.<Object> read(result, "$.meals[0].type")).isEqualTo("dinner");
	}
}
