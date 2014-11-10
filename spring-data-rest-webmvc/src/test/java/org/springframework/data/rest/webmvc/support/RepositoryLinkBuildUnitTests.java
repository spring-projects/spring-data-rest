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
package org.springframework.data.rest.webmvc.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.rest.core.mapping.MappingResourceMetadata;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.TestMvcClient;
import org.springframework.data.rest.webmvc.mongodb.Profile;
import org.springframework.hateoas.Link;

/**
 * Unit tests for {@link RepositoryLinkBuilder}.
 * 
 * @author Oliver Gierke
 */
public class RepositoryLinkBuildUnitTests {

	MongoMappingContext context = new MongoMappingContext();

	/**
	 * @see DATAREST-292
	 */
	@Test
	public void usesCurrentRequestsUriBaseForRelativeBaseUri() {

		TestMvcClient.initWebTest();

		assertRootUriFor("api", "http://localhost/api/profile");
	}

	/**
	 * @see DATAREST-292, DATAREST-296
	 */
	@Test
	public void usesBaseUriOnlyIfItIsAbsolute() {
		assertRootUriFor("http://foobar/api", "http://foobar/api/profile");
	}

	private void assertRootUriFor(String baseUri, String expectedUri) {

		MongoPersistentEntity<?> entity = context.getPersistentEntity(Profile.class);
		ResourceMetadata metadata = new MappingResourceMetadata(entity);

		RepositoryLinkBuilder builder = new RepositoryLinkBuilder(metadata, new BaseUri(baseUri));
		Link link = builder.withSelfRel();

		assertThat(link.getHref(), is(expectedUri));
	}
}
