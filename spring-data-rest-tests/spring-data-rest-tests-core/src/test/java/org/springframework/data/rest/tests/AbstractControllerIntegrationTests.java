/*
 * Copyright 2013-2023 the original author or authors.
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
package org.springframework.data.rest.tests;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.support.DefaultSelfLinkProvider;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.Projector;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Base class to write integration tests for controllers.
 *
 * @author Oliver Gierke
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public abstract class AbstractControllerIntegrationTests {

	public static final Path BASE = new Path("http://localhost");

	@Configuration
	@Import(RepositoryRestMvcConfiguration.class)
	public static class TestConfiguration {

		@Bean
		public PersistentEntityResourceAssembler persistentEntityResourceAssembler(PersistentEntities entities,
				EntityLinks entityLinks, Associations associations) {

			SelfLinkProvider selfLinkProvider = new DefaultSelfLinkProvider(entities, entityLinks, Collections.emptyList(),
					new DefaultConversionService());

			return new PersistentEntityResourceAssembler(entities, StubProjector.INSTANCE, associations,
					selfLinkProvider);
		}
	}

	@Autowired Repositories repositories;
	@Autowired RepositoryInvokerFactory invokerFactory;
	@Autowired ResourceMappings mappings;

	@BeforeEach
	public void initWebInfrastructure() {
		TestMvcClient.initWebTest();
	}

	/**
	 * Returns a {@link RootResourceInformation} for the given domain type.
	 *
	 * @param domainType must not be {@literal null}.
	 * @return
	 */
	protected RootResourceInformation getResourceInformation(Class<?> domainType) {

		Assert.notNull(domainType, "Domain type must not be null!");

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainType);

		return new RootResourceInformation(mappings.getMetadataFor(domainType), entity,
				invokerFactory.getInvokerFor(domainType));
	}

	protected WebRequest getRequest(RequestParameters parameters) {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameters(parameters.asMap());

		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		return new ServletWebRequest(request);
	}

	protected ResourceMetadata getMetadata(Class<?> domainType) {
		return mappings.getMetadataFor(domainType);
	}

	private static enum StubProjector implements Projector {

		INSTANCE;

		@Override
		public Object project(Object source) {
			return source;
		}

		@Override
		public Object projectExcerpt(Object source) {
			return source;
		}

		@Override
		public boolean hasExcerptProjection(Class<?> type) {
			return false;
		}
	}
}
