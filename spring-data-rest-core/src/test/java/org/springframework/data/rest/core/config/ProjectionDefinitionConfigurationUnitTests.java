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
package org.springframework.data.rest.core.config;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration.ProjectionDefinitionKey;

/**
 * Unit tests for {@link ProjectionDefinitionConfiguration}.
 * 
 * @author Oliver Gierke
 */
@SuppressWarnings("rawtypes")
public class ProjectionDefinitionConfigurationUnitTests {

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProjectionTypeForAutoConfiguration() {
		new ProjectionDefinitionConfiguration().addProjection(null);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnannotatedClassForConfigurationShortcut() {
		new ProjectionDefinitionConfiguration().addProjection(String.class);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProjectionTypeForManualConfiguration() {
		new ProjectionDefinitionConfiguration().addProjection(null, "name", Object.class);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullNameForManualConfiguration() {
		new ProjectionDefinitionConfiguration().addProjection(String.class, (String) null, Object.class);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptyNameForManualConfiguration() {
		new ProjectionDefinitionConfiguration().addProjection(String.class, "", Object.class);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptySourceTypes() {
		new ProjectionDefinitionConfiguration().addProjection(String.class, "name", new Class<?>[0]);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void findsRegisteredProjection() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(Integer.class, "name", String.class);

		assertThat(configuration.getProjectionType(String.class, "name"), is(equalTo((Class) Integer.class)));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void registersAnnotatedProjection() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(SampleProjection.class);

		assertThat(configuration.getProjectionType(Integer.class, "name"), is(equalTo((Class) SampleProjection.class)));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void defaultsNameToSimpleClassNameIfNotAnnotated() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(Default.class);

		assertThat(configuration.getProjectionType(Integer.class, "default"), is(equalTo((Class) Default.class)));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void definitionKeyEquals() {

		ProjectionDefinitionKey objectNameKey = new ProjectionDefinitionKey(Object.class, "name");
		ProjectionDefinitionKey sameObjectNameKey = new ProjectionDefinitionKey(Object.class, "name");
		ProjectionDefinitionKey stringNameKey = new ProjectionDefinitionKey(String.class, "name");
		ProjectionDefinitionKey objectOtherNameKey = new ProjectionDefinitionKey(Object.class, "otherName");

		assertThat(objectNameKey, is(objectNameKey));
		assertThat(objectNameKey, is(sameObjectNameKey));
		assertThat(sameObjectNameKey, is(objectNameKey));

		assertThat(objectNameKey, is(not(stringNameKey)));
		assertThat(stringNameKey, is(not(objectNameKey)));

		assertThat(objectNameKey, is(not(objectOtherNameKey)));
		assertThat(objectOtherNameKey, is(not(objectNameKey)));
	}

	/**
	 * @see DATAREST-385
	 */
	@Test
	public void returnsProjectionForParentClass() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(ParentProjection.class);

		assertThat(configuration.hasProjectionFor(Child.class), is(true));
		assertThat(configuration.getProjectionsFor(Child.class).values(), hasItem(ParentProjection.class));
		assertThat(configuration.getProjectionType(Child.class, "parentProjection"),
				is(typeCompatibleWith(ParentProjection.class)));
	}

	@Projection(name = "name", types = Integer.class)
	interface SampleProjection {

	}

	@Projection(types = Integer.class)
	interface Default {

	}

	class Parent {}

	class Child extends Parent {}

	@Projection(types = Parent.class)
	interface ParentProjection {}
}
