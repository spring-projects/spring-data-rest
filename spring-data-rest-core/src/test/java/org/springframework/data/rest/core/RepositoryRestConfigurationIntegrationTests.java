/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.config.ResourceMapping;
import org.springframework.data.rest.core.domain.ConfiguredPersonRepository;

/**
 * Tests to check that {@link ResourceMapping}s are handled correctly.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@SuppressWarnings("deprecation")
public class RepositoryRestConfigurationIntegrationTests extends AbstractIntegrationTests {

	@Autowired RepositoryRestConfiguration config;

	@Test
	public void shouldProvideResourceMappingForConfiguredRepository() throws Exception {

		ResourceMapping mapping = config.getResourceMappingForRepository(ConfiguredPersonRepository.class);

		assertThat(mapping).isNotNull();
		assertThat(mapping.getRel()).isEqualTo("people");
		assertThat(mapping.getPath()).isEqualTo("people");
		assertThat(mapping.isExported()).isFalse();
	}
}
