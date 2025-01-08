/*
 * Copyright 2015-2025 the original author or authors.
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

import java.util.List;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Delegating implementation of {@link RepositoryRestConfigurer} that will forward all calls to configuration methods to
 * all registered {@link RepositoryRestConfigurer}.
 *
 * @author Oliver Gierke
 * @soundtrack Florian Reichelt & Max Ender - Abschlusskonzert (https://www.youtube.com/watch?v=5WP0P-ndinY)
 */
class RepositoryRestConfigurerDelegate implements RepositoryRestConfigurer {

	private final Iterable<RepositoryRestConfigurer> delegates;

	/**
	 * Creates a new {@link RepositoryRestConfigurerDelegate} for the given {@link RepositoryRestConfigurer}s.
	 *
	 * @param delegates must not be {@literal null}.
	 */
	public RepositoryRestConfigurerDelegate(Iterable<RepositoryRestConfigurer> delegates) {

		Assert.notNull(delegates, "RepositoryRestConfigurers must not be null");

		this.delegates = delegates;
	}

	@Override
	public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {

		for (RepositoryRestConfigurer configurer : delegates) {
			configurer.configureRepositoryRestConfiguration(config, cors);
		}
	}

	@Override
	public void configureConversionService(ConfigurableConversionService conversionService) {

		for (RepositoryRestConfigurer configurer : delegates) {
			configurer.configureConversionService(conversionService);
		}
	}

	@Override
	public void configureExceptionHandlerExceptionResolver(ExceptionHandlerExceptionResolver exceptionResolver) {

		for (RepositoryRestConfigurer configurer : delegates) {
			configurer.configureExceptionHandlerExceptionResolver(exceptionResolver);
		}
	}

	@Override
	public void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {

		for (RepositoryRestConfigurer configurer : delegates) {
			configurer.configureHttpMessageConverters(messageConverters);
		}
	}

	@Override
	public void configureJacksonObjectMapper(ObjectMapper objectMapper) {

		for (RepositoryRestConfigurer configurer : delegates) {
			configurer.configureJacksonObjectMapper(objectMapper);
		}
	}

	@Override
	public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {

		for (RepositoryRestConfigurer configurer : delegates) {
			configurer.configureValidatingRepositoryEventListener(validatingListener);
		}
	}

	@Override
	public AuditableBeanWrapperFactory customizeAuditableBeanWrapperFactory(AuditableBeanWrapperFactory factory) {

		for (RepositoryRestConfigurer configurer : delegates) {
			factory = configurer.customizeAuditableBeanWrapperFactory(factory);
		}

		return factory;
	}

	@Override
	public LinkCollector customizeLinkCollector(LinkCollector collector) {

		for (RepositoryRestConfigurer configurer : delegates) {
			collector = configurer.customizeLinkCollector(collector);
		}

		return collector;
	}
}
