/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.util.Collections;

import org.junit.Test;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.support.DefaultSelfLinkProvider;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests.TestConfiguration;
import org.springframework.data.rest.tests.mongodb.MongoDbRepositoryConfig;
import org.springframework.data.rest.tests.mongodb.User;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.Projector;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Links;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link PersistentEntityResourceAssembler}.
 *
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = { TestConfiguration.class, MongoDbRepositoryConfig.class })
public class PersistentEntityResourceAssemblerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired PersistentEntities entities;
	@Autowired EntityLinks entityLinks;
	@Autowired Associations associations;

	@Test // DATAREST-609
	public void addsSelfAndSingleResourceLinkToResourceByDefault() throws Exception {

		Projector projector = mock(Projector.class);

		when(projector.projectExcerpt(any())).thenAnswer(new ReturnsArgumentAt(0));

		PersistentEntityResourceAssembler assembler = new PersistentEntityResourceAssembler(entities, projector,
				associations, new DefaultSelfLinkProvider(entities, entityLinks, Collections.<EntityLookup<?>> emptyList()));

		User user = new User();
		user.id = BigInteger.valueOf(4711);

		PersistentEntityResource resource = assembler.toResource(user);

		Links links = new Links(resource.getLinks());

		assertThat(links).hasSize(2);
		assertThat(links.getLink("self").orElseThrow(() -> new RuntimeException("Unable to find 'self' link")).getVariables()).isEmpty();
		assertThat(links.getLink("user").orElseThrow(() -> new RuntimeException("Unable to find 'user' link")).getVariableNames()).contains("projection");
	}
}
