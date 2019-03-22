package org.springframework.data.rest.tests;
/*
 * Copyright 2013-2016 the original author or authors.
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

import java.util.Collections;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.support.DefaultSelfLinkProvider;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.support.Projector;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public abstract class AbstractControllerIntegrationTests {

	public static final Path BASE = new Path("http://localhost");

	@Configuration
	public static class TestConfiguration extends RepositoryRestMvcConfiguration {

		public TestConfiguration(ApplicationContext context, ObjectFactory<ConversionService> conversionService) {
			super(context, conversionService);
		}

		@Bean
		public PersistentEntityResourceAssembler persistentEntityResourceAssembler() {

			SelfLinkProvider selfLinkProvider = new DefaultSelfLinkProvider(persistentEntities(), entityLinks(),
					Collections.<EntityLookup<?>> emptyList());

			return new PersistentEntityResourceAssembler(persistentEntities(), StubProjector.INSTANCE, associationLinks(),
					selfLinkProvider);
		}
	}

	@Autowired Repositories repositories;
	@Autowired RepositoryInvokerFactory invokerFactory;
	@Autowired ResourceMappings mappings;

	@Before
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
