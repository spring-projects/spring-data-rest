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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.mongodb.MongoDbRepositoryConfig;
import org.springframework.data.rest.webmvc.mongodb.Profile;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = MongoDbRepositoryConfig.class)
public class PersistentEntityToJsonSchemaConverterUnitTests extends AbstractControllerIntegrationTests {

	@Autowired PersistentEntityToJsonSchemaConverter converter;

	@Test
	public void addsDescriptionToSchemaRoot() {

		JsonSchema schema = converter.convert(Profile.class);

		assertThat(schema.getDescription(), is("Profile description"));
	}
}
