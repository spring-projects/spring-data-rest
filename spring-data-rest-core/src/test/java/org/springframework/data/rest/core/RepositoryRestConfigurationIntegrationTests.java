/*
 * Copyright 2012-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.config.ResourceMapping;
import org.springframework.data.rest.core.domain.ConfiguredPersonRepository;
import org.springframework.data.rest.core.domain.Profile;
import org.springframework.data.rest.core.domain.ProfileRepository;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Tests to check that {@link ResourceMapping}s are handled correctly.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@SuppressWarnings("deprecation")
class RepositoryRestConfigurationIntegrationTests extends AbstractIntegrationTests {

	@Autowired RepositoryRestConfiguration config;
	@Autowired Repositories repositories;

	@Test
	void shouldProvideResourceMappingForConfiguredRepository() throws Exception {

		ResourceMapping mapping = config.getResourceMappingForRepository(ConfiguredPersonRepository.class);

		assertThat(mapping).isNotNull();
		assertThat(mapping.getRel()).isEqualTo("people");
		assertThat(mapping.getPath()).isEqualTo("people");
		assertThat(mapping.isExported()).isFalse();
	}

	@Test // DATAREST-1304
	@DirtiesContext
	void exposesLookupPropertyFromLambda() {

		config.withEntityLookup() //
				.forRepository(ProfileRepository.class) //
				.withIdMapping(Profile::getName) //
				.withLookup(ProfileRepository::findByName);

		PluginRegistry<EntityLookup<?>, Class<?>> lookups = PluginRegistry.of(config.getEntityLookups(repositories));

		assertThat(lookups.getPluginFor(Profile.class)).hasValueSatisfying(it -> {
			assertThat(it.getLookupProperty()).hasValue("name");
		});
	}
}
