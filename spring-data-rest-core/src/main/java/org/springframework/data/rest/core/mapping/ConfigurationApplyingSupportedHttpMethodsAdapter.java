// Generated by delombok at Tue Aug 11 12:16:38 CEST 2020
/*
 * Copyright 2018-2024 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * Adapter for a {@link SupportedHttpMethods} instance that applies settings made through {@link ExposureConfiguration}
 * to the calculated {@link ConfigurableHttpMethods}
 *
 * @author Oliver Gierke
 * @see ExposureConfiguration
 * @since 3.1
 */
class ConfigurationApplyingSupportedHttpMethodsAdapter implements SupportedHttpMethods {

	private final ExposureConfiguration configuration;
	private final ResourceMetadata resourceMetadata;
	private final SupportedHttpMethods delegate;

	ConfigurationApplyingSupportedHttpMethodsAdapter(ExposureConfiguration configuration,
			ResourceMetadata resourceMetadata, SupportedHttpMethods delegate) {

		Assert.notNull(configuration, "Configuration must not be null");
		Assert.notNull(resourceMetadata, "ResourceMetadata must not be null");
		Assert.notNull(delegate, "SupportedHttpMethods must not be null");

		this.configuration = configuration;
		this.resourceMetadata = resourceMetadata;
		this.delegate = delegate;
	}

	@Override
	public HttpMethods getMethodsFor(ResourceType type) {

		HttpMethods methods = delegate.getMethodsFor(type);

		return configuration.filter(ConfigurableHttpMethods.of(methods), type, resourceMetadata);
	}

	@Override
	public HttpMethods getMethodsFor(PersistentProperty<?> property) {

		HttpMethods methodsFor = delegate.getMethodsFor(property);
		ResourceMapping mapping = resourceMetadata.getMappingFor(property);

		if (!PropertyAwareResourceMapping.class.isInstance(mapping)) {
			return methodsFor;
		}

		ConfigurableHttpMethods methods = ConfigurableHttpMethods.of(methodsFor);

		return configuration.filter(methods, PropertyAwareResourceMapping.class.cast(mapping));
	}

	@Override
	public boolean allowsPutForCreation() {
		return configuration.allowsPutForCreation(resourceMetadata);
	}
}
