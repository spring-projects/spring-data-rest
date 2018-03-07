/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.mapping.PersistentProperty;

/**
 * Adapter for a {@link SupportedHttpMethods} instance that applies settings made through {@link ExposureConfiguration}
 * to the calculated {@link ConfigurableHttpMethods}
 * 
 * @author Oliver Gierke
 * @see ExposureConfiguration
 * @since 3.1
 */
@RequiredArgsConstructor
public class ConfigurationApplyingSupportedHttpMethodsAdapter implements SupportedHttpMethods {

	private final @NonNull ExposureConfiguration configuration;
	private final @NonNull ResourceMetadata resourceMetadata;
	private final @NonNull SupportedHttpMethods delegate;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.SupportedHttpMethods#getMethodsFor(org.springframework.data.rest.core.mapping.ResourceType)
	 */
	@Override
	public HttpMethods getMethodsFor(ResourceType type) {

		HttpMethods methods = delegate.getMethodsFor(type);

		return configuration.filter(ConfigurableHttpMethods.of(methods), type, resourceMetadata);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.SupportedHttpMethods#getMethodsFor(org.springframework.data.mapping.PersistentProperty)
	 */
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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.SupportedHttpMethods#allowsPutForCreation()
	 */
	@Override
	public boolean allowsPutForCreation() {
		return configuration.allowsPutForCreation(resourceMetadata);
	}
}
