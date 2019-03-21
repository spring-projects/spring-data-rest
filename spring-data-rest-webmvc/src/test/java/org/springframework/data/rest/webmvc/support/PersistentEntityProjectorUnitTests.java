/*
 * Copyright 2014-2017 the original author or authors.
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
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;

/**
 * Unit tests for {@link PersistentEntityProjector}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntityProjectorUnitTests {

	@Mock ResourceMappings mappings;

	Projector projector;
	ProjectionFactory factory;
	ProjectionDefinitionConfiguration configuration;

	@Before
	public void setUp() {

		this.configuration = new ProjectionDefinitionConfiguration();
		this.factory = new SpelAwareProxyProjectionFactory();
		this.projector = new PersistentEntityProjector(configuration, factory, "sample", mappings);

		ResourceMetadata metadata = mock(ResourceMetadata.class);
		doReturn(metadata).when(mappings).getMetadataFor(Object.class);
		doReturn(Excerpt.class).when(metadata).getExcerptProjection();
	}

	@Test // DATAREST-221
	public void returnsObjectAsIsIfNoProjectionTypeFound() {

		Object object = new Object();

		assertThat(projector.project(object)).isEqualTo(object);
	}

	@Test // DATAREST-221
	public void invokesProjectionFactoryIfProjectionFound() {

		configuration.addProjection(Sample.class, Object.class);

		assertThat(projector.project(new Object())).isInstanceOf(Sample.class);
	}

	@Test // DATAREST-806
	public void favorsExplicitProjectionOverExcerpt() {

		configuration.addProjection(Sample.class, Object.class);

		assertThat(projector.projectExcerpt(new Object())).isInstanceOf(Sample.class);
	}

	@Test // DATAREST-806
	public void excerptProjectionIsUsedForExcerpt() {
		assertThat(projector.projectExcerpt(new Object())).isInstanceOf(Excerpt.class);
	}

	@Test // DATAREST-806
	public void usesExcerptProjectionIfNoExplicitProjectionWasRequested() {

		configuration.addProjection(Sample.class, Object.class);

		PersistentEntityProjector projector = new PersistentEntityProjector(configuration, factory, null, mappings);

		assertThat(projector.projectExcerpt(new Object())).isInstanceOf(Excerpt.class);
	}

	interface Sample {}

	interface Excerpt {}
}
