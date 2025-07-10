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

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

/**
 * Component to configure and customize the setup of Spring Data REST.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 2.4
 * @soundtrack Florian Reichelt & Max Ender - Abschlusskonzert (https://www.youtube.com/watch?v=5WP0P-ndinY)
 */
public interface RepositoryRestConfigurer {

	/**
	 * Convenience method to easily create simple {@link RepositoryRestConfigurer} instances that solely want to tweak the
	 * {@link RepositoryRestConfiguration}.
	 *
	 * @param consumer must not be {@literal null}.
	 * @return
	 * @since 3.1
	 */
	static RepositoryRestConfigurer withConfig(Consumer<RepositoryRestConfiguration> consumer) {

		Assert.notNull(consumer, "Consumer must not be null");

		return new RepositoryRestConfigurer() {

			@Override
			public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
				consumer.accept(config);
			}
		};
	}

	/**
	 * Convenience method to easily create simple {@link RepositoryRestConfigurer} instances that solely want to tweak the
	 * {@link RepositoryRestConfiguration}.
	 *
	 * @param consumer must not be {@literal null}.
	 * @return
	 * @since 3.4
	 */
	static RepositoryRestConfigurer withConfig(BiConsumer<RepositoryRestConfiguration, CorsRegistry> consumer) {

		Assert.notNull(consumer, "Consumer must not be null");

		return new RepositoryRestConfigurer() {

			@Override
			public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
				consumer.accept(config, cors);
			}
		};
	}

	/**
	 * Override this method to add additional configuration.
	 *
	 * @param config Main configuration bean.
	 * @param cors CORS configuration.
	 * @since 3.4
	 */
	default void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {}

	/**
	 * Override this method to add your own converters.
	 *
	 * @param conversionService Default ConversionService bean.
	 */
	default void configureConversionService(ConfigurableConversionService conversionService) {}

	/**
	 * Override this method to add validators manually.
	 *
	 * @param validatingListener the {@link org.springframework.context.ApplicationListener} responsible for invoking
	 *          {@link org.springframework.validation.Validator} instances.
	 */
	default void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {}

	/**
	 * Configure the {@link ExceptionHandlerExceptionResolver}.
	 *
	 * @param exceptionResolver the default exception resolver on which you can add custom argument resolvers.
	 */
	default void configureExceptionHandlerExceptionResolver(ExceptionHandlerExceptionResolver exceptionResolver) {}

	/**
	 * Configure the available {@link HttpMessageConverter}s by adding your own.
	 *
	 * @param messageConverters the converters to be used by the system.
	 */
	default void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {}

	/**
	 * Configure the Jackson {@link MapperBuilder} directly.
	 *
	 * @param mapperBuilder the mapper builder to configure.
	 */
	default void configureJacksonObjectMapper(
			MapperBuilder<? extends ObjectMapper, ? extends MapperBuilder<?, ?>> mapperBuilder) {}

	/**
	 * Customize the {@link AuditableBeanWrapperFactory} to be used.
	 *
	 * @param factory will never be {@literal null}.
	 * @return must not be {@literal null}.
	 * @since 3.5
	 */
	default AuditableBeanWrapperFactory customizeAuditableBeanWrapperFactory(AuditableBeanWrapperFactory factory) {
		return factory;
	}

	/**
	 * Customize the {@link LinkCollector} to be used.
	 *
	 * @param collector will never be {@literal null}.
	 * @return must not be {@literal null}.
	 * @since 3.5
	 */
	default LinkCollector customizeLinkCollector(LinkCollector collector) {
		return collector;
	}

}
