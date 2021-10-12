/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.rest.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.domain.Profile;
import org.springframework.data.rest.core.domain.ProfileRepository;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.http.MediaType;

/**
 * Unit tests for {@link RepositoryRestConfiguration}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Adam F - Circles (Colors)
 */
class RepositoryRestConfigurationUnitTests {

	RepositoryRestConfiguration configuration;

	@BeforeEach
	void setUp() {

		this.configuration = new RepositoryRestConfiguration(new ProjectionDefinitionConfiguration(),
				new MetadataConfiguration(), mock(EnumTranslationConfiguration.class));
	}

	@Test // DATAREST-34
	void returnsBodiesIfAcceptHeaderPresentByDefault() {

		assertThat(configuration.returnBodyOnCreate(MediaType.APPLICATION_JSON_VALUE)).isTrue();
		assertThat(configuration.returnBodyOnUpdate(MediaType.APPLICATION_JSON_VALUE)).isTrue();
	}

	@Test // DATAREST-34
	void doesNotReturnBodiesIfNoAcceptHeaderPresentByDefault() {

		assertThat(configuration.returnBodyOnCreate(null)).isFalse();
		assertThat(configuration.returnBodyOnUpdate(null)).isFalse();
	}

	@Test // DATAREST-34
	void doesNotReturnBodiesIfEmptyAcceptHeaderPresentByDefault() {

		assertThat(configuration.returnBodyOnCreate("")).isFalse();
		assertThat(configuration.returnBodyOnUpdate("")).isFalse();
	}

	@Test // DATAREST-34
	void doesNotReturnBodyForUpdateIfExplicitlyDeactivated() {

		configuration.setReturnBodyOnUpdate(false);

		assertThat(configuration.returnBodyOnUpdate(null)).isFalse();
		assertThat(configuration.returnBodyOnUpdate("")).isFalse();
		assertThat(configuration.returnBodyOnUpdate(MediaType.APPLICATION_JSON_VALUE)).isFalse();
	}

	@Test // DATAREST-34
	void doesNotReturnBodyForCreateIfExplicitlyDeactivated() {

		configuration.setReturnBodyOnCreate(false);

		assertThat(configuration.returnBodyOnCreate(null)).isFalse();
		assertThat(configuration.returnBodyOnCreate("")).isFalse();
		assertThat(configuration.returnBodyOnCreate(MediaType.APPLICATION_JSON_VALUE)).isFalse();
	}

	@Test // DATAREST-34
	void returnsBodyForUpdateIfExplicitlyActivated() {

		configuration.setReturnBodyOnUpdate(true);

		assertThat(configuration.returnBodyOnUpdate(null)).isTrue();
		assertThat(configuration.returnBodyOnUpdate("")).isTrue();
		assertThat(configuration.returnBodyOnUpdate(MediaType.APPLICATION_JSON_VALUE)).isTrue();
	}

	@Test // DATAREST-34
	void returnsBodyForCreateIfExplicitlyActivated() {

		configuration.setReturnBodyOnCreate(true);

		assertThat(configuration.returnBodyOnCreate(null)).isTrue();
		assertThat(configuration.returnBodyOnCreate("")).isTrue();
		assertThat(configuration.returnBodyOnCreate(MediaType.APPLICATION_JSON_VALUE)).isTrue();
	}

	@Test // DATAREST-776
	void considersDomainTypeOfValueRepositoryLookupTypes() {

		configuration.withEntityLookup().forLookupRepository(ProfileRepository.class);

		assertThat(configuration.isLookupType(Profile.class)).isTrue();
	}

	@Test // DATAREST-1076
	void rejectsNullRelProvider() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> configuration.setLinkRelationProvider(null));
	}

	@Test // #1974
	void considersAtRelationOnTypesByDefault() {
		assertThat(configuration.getLinkRelationProvider().getItemResourceRelFor(Sample.class))
				.isEqualTo(LinkRelation.of("something"));
	}

	@Relation("something")
	static class Sample {

	}
}
