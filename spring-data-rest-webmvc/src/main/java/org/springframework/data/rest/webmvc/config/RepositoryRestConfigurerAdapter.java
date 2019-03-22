/*
 * Copyright 2015-2019 the original author or authors.
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
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Convenience adapter for {@link RepositoryRestConfigurer} to allow selective override of configuration methods.
 *
 * @author Oliver Gierke
 * @since 2.4
 * @soundtrack Florian Reichelt & Max Ender - Abschlusskonzert (https://www.youtube.com/watch?v=5WP0P-ndinY)
 * @deprecated since 3.1, implement {@link RepositoryRestConfigurer} directly.
 */
@Deprecated
public class RepositoryRestConfigurerAdapter implements RepositoryRestConfigurer {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer#configureRepositoryRestConfiguration(org.springframework.data.rest.core.config.RepositoryRestConfiguration)
	 */
	@Override
	public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer#configureConversionService(org.springframework.core.convert.support.ConfigurableConversionService)
	 */
	@Override
	public void configureConversionService(ConfigurableConversionService conversionService) {}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer#configureValidatingRepositoryEventListener(org.springframework.data.rest.core.event.ValidatingRepositoryEventListener)
	 */
	@Override
	public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer#configureExceptionHandlerExceptionResolver(org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver)
	 */
	@Override
	public void configureExceptionHandlerExceptionResolver(ExceptionHandlerExceptionResolver exceptionResolver) {}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer#configureHttpMessageConverters(java.util.List)
	 */
	@Override
	public void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer#configureJacksonObjectMapper(com.fasterxml.jackson.databind.ObjectMapper)
	 */
	@Override
	public void configureJacksonObjectMapper(ObjectMapper objectMapper) {}
}
