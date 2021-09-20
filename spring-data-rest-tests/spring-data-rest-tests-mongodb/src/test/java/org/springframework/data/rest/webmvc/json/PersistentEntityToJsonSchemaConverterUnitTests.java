/*
 * Copyright 2014-2021 the original author or authors.
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
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.config.JsonSchemaFormat;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.tests.TestMvcClient;
import org.springframework.data.rest.tests.mongodb.MongoDbRepositoryConfig;
import org.springframework.data.rest.tests.mongodb.Profile;
import org.springframework.data.rest.tests.mongodb.User;
import org.springframework.data.rest.tests.mongodb.User.EmailAddress;
import org.springframework.data.rest.tests.mongodb.User.TypeWithPattern;
import org.springframework.data.rest.tests.mongodb.groovy.SimulatedGroovyDomainClass;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverter.ValueTypeSchemaPropertyCustomizerFactory;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverterUnitTests.TestConfiguration;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MongoDbRepositoryConfig.class, TestConfiguration.class })
class PersistentEntityToJsonSchemaConverterUnitTests {

	@Autowired MessageResolver resolver;
	@Autowired RepositoryRestConfiguration configuration;
	@Autowired PersistentEntities entities;
	@Autowired Associations associations;

	ObjectMapper objectMapper = new ObjectMapper();

	@Configuration
	@Import(RepositoryRestMvcConfiguration.class)
	static class TestConfiguration {

		@Bean
		public RepositoryRestConfigurer customizations() {

			return RepositoryRestConfigurer.withConfig(config -> {

				config.getMetadataConfiguration().registerJsonSchemaFormat(JsonSchemaFormat.EMAIL, EmailAddress.class);
				config.getMetadataConfiguration().registerFormattingPatternFor("[A-Z]+", TypeWithPattern.class);

				config.exposeIdsFor(Profile.class);
			});
		}
	}

	PersistentEntityToJsonSchemaConverter converter;

	@BeforeEach
	void setUp() {

		TestMvcClient.initWebTest();

		ValueTypeSchemaPropertyCustomizerFactory customizerFactory = mock(ValueTypeSchemaPropertyCustomizerFactory.class);

		converter = new PersistentEntityToJsonSchemaConverter(entities, associations, resolver, objectMapper, configuration,
				customizerFactory);
	}

	@Test // DATAREST-631, DATAREST-632
	void fulfillsConstraintsForProfile() {

		List<Constraint> constraints = new ArrayList<Constraint>();
		constraints.add(new Constraint("$.properties.id", is(notNullValue()), "Has descriptor for id property"));
		constraints.add(new Constraint("$.description", is("Profile description"), "Adds description to schema root"));
		constraints.add(new Constraint("$.properties.renamed", is(notNullValue()), "Has descriptor for renamed property"));
		constraints.add(
				new Constraint("$.properties.aliased", is(nullValue()), "No descriptor for original name of renamed property"));

		assertConstraints(Profile.class, constraints);
	}

	@Test // DATAREST-632
	void fulfillsConstraintsForUser() throws Exception {

		List<Constraint> constraints = new ArrayList<Constraint>();
		constraints.add(new Constraint("$.properties.id", is(nullValue()), "Does NOT have descriptor for id property"));
		constraints.add(new Constraint("$.properties.firstname.type", is("string"), "Exposes firstname as String"));
		constraints
				.add(new Constraint("$.definitions.address", is(notNullValue()), "Exposes nested objects as definitions."));
		constraints.add(new Constraint("$.definitions.address.type", is("object"), "Nested entity is of type 'object'"));
		constraints.add(
				new Constraint("$.definitions.address.properties.zipCode", is(notNullValue()), "Exposes nested properties"));
		constraints.add(
				new Constraint("$.definitions.address.requiredProperties[0]", is("zipCode"), "Lists nested required property"));
		constraints.add(new Constraint("$.properties.gender.type", is("string"), "Enums are strings."));
		constraints.add(new Constraint("$.properties.gender.enum", is(notNullValue()), "Exposes enum values."));
		constraints
				.add(new Constraint("$.properties.java8DateTime.format", is("date-time"), "Exposes Java 8 dates in format."));
		constraints.add(new Constraint("$.properties.nicknames.type", is("array"), "Exposes collection of simple types."));
		constraints.add(new Constraint("$.properties.nicknames.items.type", is("string"),
				"Exposes element type of collection of simple types."));
		constraints.add(new Constraint("$.properties.email.format", is("email"), "Uses manually configured format."));
		constraints.add(new Constraint("$.properties.email.type", is("string"), "Treats types with format as String."));

		constraints.add(
				new Constraint("$.properties.shippingAddresses.type", is("array"), "Exposes collection of complex types."));
		constraints
				.add(new Constraint("$.properties.shippingAddresses.uniqueItems", is(true), "Exposes uniqueness for Sets."));
		constraints.add(new Constraint("$.properties.shippingAddresses.items['$ref']", is("#/definitions/address"),
				"References definition of complex element type."));

		// DATAREST-531
		constraints.add(new Constraint("$.properties.email.readOnly", is(true), "Email is read-only property"));

		// DATAREST-644
		constraints.add(new Constraint("$.properties.shippingAddresses.title", is("Shipping addresses"),
				"Defaults titles correctly (split at camel case)"));

		// DATAREST-665
		constraints.add(new Constraint("$.properties.address.title", is("Adresse"), "I18n from simple property"));
		constraints.add(new Constraint("$.properties.gender.title", is("Geschlecht"), "I18n from property on local type"));
		constraints.add(
				new Constraint("$.properties.firstname.title", is("Vorname"), "I18n from property on fully-qualified type"));

		// DATAREST-690
		constraints.add(new Constraint("$.properties.colleagues.items", is(nullValue()),
				"Items must not appear for collection associations."));

		assertConstraints(User.class, constraints);
	}

	@Test // DATAREST-754
	void handlesGroovyDomainObjects() {

		List<Constraint> constraints = new ArrayList<Constraint>();
		constraints.add(new Constraint("$.properties.name", is(notNullValue()), "Has descriptor for name property"));

		assertConstraints(SimulatedGroovyDomainClass.class, constraints);
	}

	@SuppressWarnings("unchecked")
	private void assertConstraints(Class<?> type, Iterable<Constraint> constraints) {

		String writeSchemaFor = writeSchemaFor(type);

		for (Constraint constraint : constraints) {

			try {
				MatcherAssert.assertThat(constraint.description, JsonPath.read(writeSchemaFor, constraint.selector),
						constraint.matcher);
			} catch (PathNotFoundException e) {
				assertThat(constraint.matcher.matches(null)).isTrue();
			} catch (RuntimeException e) {
				MatcherAssert.assertThat(e, constraint.matcher);
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
