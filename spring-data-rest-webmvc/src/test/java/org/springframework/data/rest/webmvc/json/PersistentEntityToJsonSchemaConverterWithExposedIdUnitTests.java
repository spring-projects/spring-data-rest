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
import org.springframework.context.annotation.Import;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.webmvc.TestMvcClient;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverterWithExposedIdUnitTests.TestConfiguration;
import org.springframework.data.rest.webmvc.mongodb.MongoDbRepositoryConfig;
import org.springframework.data.rest.webmvc.mongodb.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Separate test case for {@link PersistentEntityToJsonSchemaConverter} where ids are exposed via
 * {@link RepositoryRestConfigurerAdapter}
 * 
 * @author Greg Turnquist
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MongoDbRepositoryConfig.class, TestConfiguration.class })
public class PersistentEntityToJsonSchemaConverterWithExposedIdUnitTests {

	@Autowired MessageSourceAccessor accessor;
	@Autowired RepositoryResourceMappings mappings;
	@Autowired RepositoryRestConfiguration configuration;
	@Autowired PersistentEntities entities;
	@Autowired @Qualifier("objectMapper") ObjectMapper objectMapper;

	@Configuration
	@Import(RepositoryRestMvcConfiguration.class)
	static class TestConfiguration extends RepositoryRestConfigurerAdapter {

		@Override
		public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.exposeIdsFor(clazzForTesting);
		}
	}

	PersistentEntityToJsonSchemaConverter converter;

	private static Class<?> clazzForTesting = Profile.class;

	@Before
	public void setUp() {

		TestMvcClient.initWebTest();

		converter = new PersistentEntityToJsonSchemaConverter(entities, mappings, accessor, objectMapper, configuration);
	}

	@Test
	public void fulfilsConstraintsForProfile() {

		List<Constraint> constraints = new ArrayList<Constraint>();
		constraints.add(new Constraint("$.properties.id", is(notNullValue()), "Has descriptor for id property"));

		assertConstraints(clazzForTesting, constraints);
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
