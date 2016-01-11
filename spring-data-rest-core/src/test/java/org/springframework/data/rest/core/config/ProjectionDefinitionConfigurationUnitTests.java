/*
 * Copyright 2014-2016 the original author or authors.
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

import java.util.Map;

import org.junit.Test;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration.ProjectionDefinition;

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
	public void definitionEquals() {

		ProjectionDefinition objectName = new ProjectionDefinition(Object.class, Object.class, "name");
		ProjectionDefinition sameObjectName = new ProjectionDefinition(Object.class, Object.class, "name");
		ProjectionDefinition stringName = new ProjectionDefinition(String.class, Object.class, "name");
		ProjectionDefinition objectOtherNameKey = new ProjectionDefinition(Object.class, Object.class, "otherName");

		assertThat(objectName, is(objectName));
		assertThat(objectName, is(sameObjectName));
		assertThat(sameObjectName, is(objectName));

		assertThat(objectName, is(not(stringName)));
		assertThat(stringName, is(not(objectName)));

		assertThat(objectName, is(not(objectOtherNameKey)));
		assertThat(objectOtherNameKey, is(not(objectName)));

		assertThat(objectName, is(not(new Object())));
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
		assertThat(configuration.getProjectionType(Child.class, "summary"), is(typeCompatibleWith(ParentProjection.class)));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void defaultsParamternameToProjection() {
		assertThat(new ProjectionDefinitionConfiguration().getParameterName(), is("projection"));
	}

	/**
	 * @see DATAREST-747
	 */
	@Test
	public void returnsMostConcreteProjectionForSourceType() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(ParentProjection.class);
		configuration.addProjection(ChildProjection.class);

		Map<String, Class<?>> projections = configuration.getProjectionsFor(Child.class);

		assertThat(projections.values(), hasSize(1));
		assertThat(projections.values(), hasItem(ChildProjection.class));
	}

	@Projection(name = "name", types = Integer.class)
	interface SampleProjection {}

	@Projection(types = Integer.class)
	interface Default {

	}

	class Parent {}

	class Child extends Parent {}

	@Projection(name = "summary", types = Parent.class)
	interface ParentProjection {}

	@Projection(name = "summary", types = Child.class)
	interface ChildProjection {}
}
