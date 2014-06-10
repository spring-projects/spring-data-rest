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
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.projection.ProjectionFactory;

/**
 * Unit tests for {@link PersistentEntityProjector}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntityProjectorUnitTests {

	@Mock ProjectionFactory factory;
	@Mock ResourceMappings mappings;
	ProjectionDefinitionConfiguration configuration;

	@Before
	public void setUp() {
		configuration = new ProjectionDefinitionConfiguration();
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void returnsObjectAsIfNoProjectionTypeFound() {

		Projector projector = new PersistentEntityProjector(configuration, factory, "sample", mappings);

		Object object = new Object();
		assertThat(projector.project(object), is(object));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void invokesProjectionFactoryIfProjectionFound() {

		configuration.addProjection(Sample.class, Object.class);

		Projector projector = new PersistentEntityProjector(configuration, factory, "sample", mappings);
		Object source = new Object();
		projector.project(source);

		verify(factory, times(1)).createProjection(source, Sample.class);
	}

	interface Sample {

	}
}
