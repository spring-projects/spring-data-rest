/*
 * Copyright 2020-2025 the original author or authors.
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

/**
 * Components that are aware of CORS configuration.
 *
 * @author Oliver Drotbohm
 * @since 3.4
 * @soundtrack Elen - Blind über Rot (Blind über Rot)
 */
public interface CorsConfigurationAware {

	/**
	 * Return the registered {@link CorsConfiguration} objects, keyed by path pattern.
	 *
	 * @return will never be {@literal null}.
	 */
	Map<String, CorsConfiguration> getCorsConfigurations();
}
