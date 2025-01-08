/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.rest.core.config;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration.ProjectionDefinition;

/**
 * Unit tests for {@link ProjectionDefinitionConfiguration}.
 *
 * @author Oliver Gierke
 */
class ProjectionDefinitionConfigurationUnitTests {

	@Test // DATAREST-221
	void rejectsNullProjectionTypeForAutoConfiguration() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new ProjectionDefinitionConfiguration().addProjection(null));
	}

	@Test // DATAREST-221
	void rejectsUnannotatedClassForConfigurationShortcut() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new ProjectionDefinitionConfiguration().addProjection(String.class));
	}

	@Test // DATAREST-221
	void rejectsNullProjectionTypeForManualConfiguration() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new ProjectionDefinitionConfiguration().addProjection(null, "name", Object.class));
	}

	@Test // DATAREST-221
	void rejectsNullNameForManualConfiguration() {

		assertThatIllegalArgumentException() //
				.isThrownBy(
						() -> new ProjectionDefinitionConfiguration().addProjection(String.class, (String) null, Object.class));
	}

	@Test // DATAREST-221
	void rejectsEmptyNameForManualConfiguration() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new ProjectionDefinitionConfiguration().addProjection(String.class, "", Object.class));
	}

	@Test // DATAREST-221
	void rejectsEmptySourceTypes() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new ProjectionDefinitionConfiguration().addProjection(String.class, "name", new Class<?>[0]));
	}

	@Test // DATAREST-221
	void findsRegisteredProjection() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(Integer.class, "name", String.class);

		assertThat(configuration.getProjectionType(String.class, "name")).isEqualTo(Integer.class);
	}

	@Test // DATAREST-221
	void registersAnnotatedProjection() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(SampleProjection.class);

		assertThat(configuration.getProjectionType(Integer.class, "name")).isEqualTo(SampleProjection.class);
	}

	@Test // DATAREST-221
	void defaultsNameToSimpleClassNameIfNotAnnotated() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(Default.class);

		assertThat(configuration.getProjectionType(Integer.class, "default")).isEqualTo(Default.class);
	}

	@Test // DATAREST-221
	void definitionEquals() {

		ProjectionDefinition objectName = ProjectionDefinition.of(Object.class, Object.class, "name");
		ProjectionDefinition sameObjectName = ProjectionDefinition.of(Object.class, Object.class, "name");
		ProjectionDefinition stringName = ProjectionDefinition.of(String.class, Object.class, "name");
		ProjectionDefinition objectOtherNameKey = ProjectionDefinition.of(Object.class, Object.class, "otherName");

		assertThat(objectName).isEqualTo(objectName);
		assertThat(objectName).isEqualTo(sameObjectName);
		assertThat(sameObjectName).isEqualTo(objectName);

		assertThat(objectName).isNotEqualTo(stringName);
		assertThat(stringName).isNotEqualTo(objectName);

		assertThat(objectName).isNotEqualTo(objectOtherNameKey);
		assertThat(objectOtherNameKey).isNotEqualTo(objectName);

		assertThat(objectName).isNotEqualTo(new Object());
	}

	@Test // DATAREST-385
	void returnsProjectionForParentClass() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(ParentProjection.class);

		assertThat(configuration.hasProjectionFor(Child.class)).isTrue();
		assertThat(configuration.getProjectionsFor(Child.class).values()).contains(ParentProjection.class);
		assertThat(configuration.getProjectionType(Child.class, "summary")).isAssignableFrom(ParentProjection.class);
	}

	@Test // DATAREST-221
	void defaultsParamternameToProjection() {
		assertThat(new ProjectionDefinitionConfiguration().getParameterName()).isEqualTo("projection");
	}

	@Test // DATAREST-747
	void returnsMostConcreteProjectionForSourceType() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();
		configuration.addProjection(ParentProjection.class);
		configuration.addProjection(ChildProjection.class);

		Map<String, Class<?>> projections = configuration.getProjectionsFor(Child.class);

		assertThat(projections.values()).hasSize(1);
		assertThat(projections.values()).contains(ChildProjection.class);
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
