/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import java.util.Map;

import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * Internal variant of {@link RepositoryRestConfiguration} to also expose the {@link CorsRegistry} via
 * {@link CorsConfigurationAware}.
 *
 * @author Oliver Drotbohm
 * @since 3.4
 * @soundtrack Elen - Andre Arcaden (Blind über Rot)
 */
class WebMvcRepositoryRestConfiguration extends RepositoryRestConfiguration implements CorsConfigurationAware {

	/**
	 * Creates a new {@link WebMvcRepositoryRestConfiguration}.
	 *
	 * @param projectionConfiguration must not be {@literal null}.
	 * @param metadataConfiguration must not be {@literal null}.
	 * @param enumTranslationConfiguration must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 */
	public WebMvcRepositoryRestConfiguration(ProjectionDefinitionConfiguration projectionConfiguration, //
			MetadataConfiguration metadataConfiguration, //
			EnumTranslationConfiguration enumTranslationConfiguration, //
			RepositoryCorsRegistry registry) {

		super(projectionConfiguration, metadataConfiguration, enumTranslationConfiguration);

		Assert.notNull(registry, "CorsRegistry must not be null");

		this.registry = registry;
	}

	private final RepositoryCorsRegistry registry;

	@Override
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		return registry.getCorsConfigurations();
	}
}
