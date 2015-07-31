/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.config.JsonSchemaFormat;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.webmvc.TestMvcClient;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverterUnitTests.TestConfiguration;
import org.springframework.data.rest.webmvc.mongodb.MongoDbRepositoryConfig;
import org.springframework.data.rest.webmvc.mongodb.Profile;
import org.springframework.data.rest.webmvc.mongodb.User;
import org.springframework.data.rest.webmvc.mongodb.User.EmailAddress;
import org.springframework.data.rest.webmvc.mongodb.User.TypeWithPattern;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MongoDbRepositoryConfig.class, TestConfiguration.class })
public class PersistentEntityToJsonSchemaConverterUnitTests {

	@Autowired MessageSourceAccessor accessor;
	@Autowired RepositoryResourceMappings mappings;
	@Autowired RepositoryRestConfiguration configuration;
	@Autowired PersistentEntities entities;
	@Autowired @Qualifier("objectMapper") ObjectMapper objectMapper;

	@Configuration
	static class TestConfiguration extends RepositoryRestMvcConfiguration {

		@Override
		protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {

			config.metadataConfiguration().registerJsonSchemaFormat(JsonSchemaFormat.EMAIL, EmailAddress.class);
			config.metadataConfiguration().registerFormattingPatternFor("[A-Z]+", TypeWithPattern.class);

			config.exposeIdsFor(Profile.class);
		}
	}

	PersistentEntityToJsonSchemaConverter converter;

	@Before
	public void setUp() {

		TestMvcClient.initWebTest();

		converter = new PersistentEntityToJsonSchemaConverter(entities, mappings, accessor, objectMapper, configuration);
	}

	@Test
	public void fulfillsConstraintsForProfile() {

		List<Constraint> constraints = new ArrayList<Constraint>();
		constraints.add(new Constraint("$.description", is("Profile description"), "Adds description to schema root"));
		constraints.add(new Constraint("$.properties.renamed", is(notNullValue()), "Has descriptor for renamed property"));
		constraints.add(new Constraint("$.properties.aliased", is(nullValue()),
				"No descriptor for original name of renamed property"));

		assertConstraints(Profile.class, constraints);
	}

	@Test
	public void fulfilsConstraintsForUser() throws Exception {

		List<Constraint> constraints = new ArrayList<Constraint>();
		constraints.add(new Constraint("$.properties.firstname.type", is("string"), "Exposes firstname as String"));
		constraints.add(new Constraint("$.descriptors.address", is(notNullValue()),
				"Exposes nested objects as descriptors."));
		constraints.add(new Constraint("$.descriptors.address.type", is("object"), "Nested entity is of type 'object'"));
		constraints.add(new Constraint("$.descriptors.address.properties.zipCode", is(notNullValue()),
				"Exposes nested properties"));
		constraints.add(new Constraint("$.descriptors.address.requiredProperties[0]", is("zipCode"),
				"Lists nested required property"));
		constraints.add(new Constraint("$.properties.gender.type", is("string"), "Enums are strings."));
		constraints.add(new Constraint("$.properties.gender.enum", is(notNullValue()), "Exposes enum values."));
		constraints.add(new Constraint("$.properties.jodaDateTime.format", is("date-time"),
				"Exposes JodaTime dates in format."));
		constraints.add(new Constraint("$.properties.java8DateTime.format", is("date-time"),
				"Exposes Java 8 dates in format."));
		constraints.add(new Constraint("$.properties.nicknames.type", is("array"), "Exposes collection of simple types."));
		constraints.add(new Constraint("$.properties.nicknames.items.type", is("string"),
				"Exposes element type of collection of simple types."));
		constraints.add(new Constraint("$.properties.email.format", is("email"), "Uses manually configured format."));
		constraints.add(new Constraint("$.properties.email.type", is("string"), "Treats types with format as String."));

		constraints.add(new Constraint("$.properties.shippingAddresses.type", is("array"),
				"Exposes collection of complex types."));
		constraints.add(new Constraint("$.properties.shippingAddresses.uniqueItems", is(true),
				"Exposes uniqueness for Sets."));
		constraints.add(new Constraint("$.properties.shippingAddresses.items['$ref']", is("#/descriptors/address"),
				"References descriptor of complex element type."));

		// DATAREST-531
		constraints.add(new Constraint("$.properties.email.readOnly", is(true), "Email is read-only property"));

		assertConstraints(User.class, constraints);
	}

	/**
	 * @see DATAREST-631
	 */
	@Test
	public void fulfilsConstraintsForProfile() {

		List<Constraint> constraints = new ArrayList<Constraint>();
		constraints.add(new Constraint("$.properties.id", is(notNullValue()), "Has descriptor for id property"));

		assertConstraints(Profile.class, constraints);
	}

	@SuppressWarnings("unchecked")
	private void assertConstraints(Class<?> type, Iterable<Constraint> constraints) {

		String writeSchemaFor = writeSchemaFor(type);

		for (Constraint constraint : constraints) {

			try {
				assertThat(constraint.description, JsonPath.read(writeSchemaFor, constraint.selector), constraint.matcher);
			} catch (RuntimeException e) {
				assertThat(e, constraint.matcher);
			}
		}
	}

	private String writeSchemaFor(Class<?> type) {

		try {
			return objectMapper.writeValueAsString(converter.convert(type));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("rawtypes")
	private static class Constraint {

		String selector;
		Matcher matcher;
		String description;

		public Constraint(String selector, Matcher matcher, String description) {
			this.selector = selector;
			this.matcher = matcher;
			this.description = description;
		}
	}
}
