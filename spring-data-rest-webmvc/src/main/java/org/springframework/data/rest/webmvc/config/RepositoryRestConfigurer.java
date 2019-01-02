/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import java.util.List;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Component to configure and customize the setup of Spring Data REST.
 * 
 * @author Oliver Gierke
 * @since 2.4
 * @soundtrack Florian Reichelt & Max Ender - Abschlusskonzert (https://www.youtube.com/watch?v=5WP0P-ndinY)
 */
public interface RepositoryRestConfigurer {

	/**
	 * Override this method to add additional configuration.
	 * 
	 * @param config Main configuration bean.
	 */
	void configureRepositoryRestConfiguration(RepositoryRestConfiguration config);

	/**
	 * Override this method to add your own converters.
	 * 
	 * @param conversionService Default ConversionService bean.
	 */
	void configureConversionService(ConfigurableConversionService conversionService);

	/**
	 * Override this method to add validators manually.
	 * 
	 * @param validatingListener The {@link org.springframework.context.ApplicationListener} responsible for invoking
	 *          {@link org.springframework.validation.Validator} instances.
	 */
	void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener);

	/**
	 * Configure the {@link ExceptionHandlerExceptionResolver}.
	 * 
	 * @param exceptionResolver The default exception resolver on which you can add custom argument resolvers.
	 */
	void configureExceptionHandlerExceptionResolver(ExceptionHandlerExceptionResolver exceptionResolver);

	/**
	 * Configure the available {@link HttpMessageConverter}s by adding your own.
	 * 
	 * @param messageConverters The converters to be used by the system.
	 */
	void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters);

	/**
	 * Configure the Jackson {@link ObjectMapper} directly.
	 * 
	 * @param objectMapper The {@literal ObjectMapper} to be used by the system.
	 */
	void configureJacksonObjectMapper(ObjectMapper objectMapper);
}
