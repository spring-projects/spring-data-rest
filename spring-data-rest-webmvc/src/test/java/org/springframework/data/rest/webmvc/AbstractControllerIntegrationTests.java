/*
 * Copyright 2013 the original author or authors.
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

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.invoke.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Base class to write integration tests for controllers.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RepositoryRestMvcConfiguration.class })
public abstract class AbstractControllerIntegrationTests {

	public static final Path BASE = new Path("http://localhost");

	@Autowired Repositories repositories;
	@Autowired RepositoryInvokerFactory invokerFactory;
	@Autowired ResourceMappings mappings;

	@Before
	public void initWebInfrastructure() {
		WebTestUtils.initWebTest();
	}

	/**
	 * Returns a {@link RepositoryRestRequest} for the given domain type.
	 * 
	 * @param domainType must not be {@literal null}.
	 * @return
	 */
	protected RepositoryRestRequest getRequest(Class<?> domainType) {
		return getRequest(domainType, RequestParameters.NONE);
	}

	protected RepositoryRestRequest getRequest(Class<?> domainType, RequestParameters parameters) {

		Assert.notNull(domainType, "Domain type must not be null!");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameters(parameters.asMap());

		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainType);

		return new RepositoryRestRequest(entity, new ServletWebRequest(request), mappings.getMappingFor(domainType),
				invokerFactory.getInvokerFor(domainType));
	}

	protected ResourceMetadata getMetadata(Class<?> domainType) {
		return mappings.getMappingFor(domainType);
	}
}
