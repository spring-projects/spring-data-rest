/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.tests.TestMvcClient;
import org.springframework.data.rest.tests.mongodb.Profile;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.hateoas.Link;

/**
 * Unit tests for {@link RepositoryLinkBuilder}.
 *
 * @author Oliver Gierke
 */
public class RepositoryLinkBuildUnitTests {

	MongoMappingContext context = new MongoMappingContext();

	@Test // DATAREST-292
	public void usesCurrentRequestsUriBaseForRelativeBaseUri() {

		TestMvcClient.initWebTest();

		assertRootUriFor("api", "http://localhost/api/profile");
	}

	@Test // DATAREST-292, DATAREST-296
	public void usesBaseUriOnlyIfItIsAbsolute() {
		assertRootUriFor("http://foobar/api", "http://foobar/api/profile");
	}

	private void assertRootUriFor(String baseUri, String expectedUri) {

		context.getPersistentEntity(Profile.class);
		ResourceMappings mappings = new PersistentEntitiesResourceMappings(new PersistentEntities(Arrays.asList(context)));

		RepositoryLinkBuilder builder = new RepositoryLinkBuilder(mappings.getMetadataFor(Profile.class),
				new BaseUri(baseUri));
		Link link = builder.withSelfRel();

		assertThat(link.getHref()).isEqualTo(expectedUri);
	}
}
