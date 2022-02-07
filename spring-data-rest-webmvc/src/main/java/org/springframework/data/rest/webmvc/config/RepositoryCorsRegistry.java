/*
 * Copyright 2016-2022 the original author or authors.
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

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * Spring Data REST specific {@code CorsRegistry} implementation exposing {@link #getCorsConfigurations()}. Assists with
 * the registration of {@link CorsConfiguration} mapped to a path pattern.
 *
 * @author Mark Paluch
 * @since 2.6
 */
class RepositoryCorsRegistry extends CorsRegistry {

	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.CorsRegistry#getCorsConfigurations()
	 */
	@Override
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		return super.getCorsConfigurations();
	}
}
