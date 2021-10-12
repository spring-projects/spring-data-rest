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
package org.springframework.data.rest.webmvc.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersistentEntityProjectorUnitTests {

	@Mock ResourceMappings mappings;

	Projector projector;
	ProjectionFactory factory;
	ProjectionDefinitionConfiguration configuration;

	@BeforeEach
	void setUp() {

		this.configuration = new ProjectionDefinitionConfiguration();
		this.factory = new SpelAwareProxyProjectionFactory();
		this.projector = new PersistentEntityProjector(configuration, factory, "sample", mappings);

		ResourceMetadata metadata = mock(ResourceMetadata.class);
		doReturn(metadata).when(mappings).getMetadataFor(Object.class);
		doReturn(Optional.of(Excerpt.class)).when(metadata).getExcerptProjection();
	}

	@Test // DATAREST-221
	void returnsObjectAsIsIfNoProjectionTypeFound() {

		Object object = new Object();

		assertThat(projector.project(object)).isEqualTo(object);
	}

	@Test // DATAREST-221
	void invokesProjectionFactoryIfProjectionFound() {

		configuration.addProjection(Sample.class, Object.class);

		assertThat(projector.project(new Object())).isInstanceOf(Sample.class);
	}

	@Test // DATAREST-806
	void favorsExplicitProjectionOverExcerpt() {

		configuration.addProjection(Sample.class, Object.class);

		assertThat(projector.projectExcerpt(new Object())).isInstanceOf(Sample.class);
	}

	@Test // DATAREST-806
	void excerptProjectionIsUsedForExcerpt() {
		assertThat(projector.projectExcerpt(new Object())).isInstanceOf(Excerpt.class);
	}

	@Test // DATAREST-806
	void usesExcerptProjectionIfNoExplicitProjectionWasRequested() {

		configuration.addProjection(Sample.class, Object.class);

		PersistentEntityProjector projector = new PersistentEntityProjector(configuration, factory, null, mappings);

		assertThat(projector.projectExcerpt(new Object())).isInstanceOf(Excerpt.class);
	}

	interface Sample {}

	interface Excerpt {}
}
