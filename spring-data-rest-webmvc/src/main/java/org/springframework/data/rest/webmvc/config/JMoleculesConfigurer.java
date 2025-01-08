/*
 * Copyright 2021-2025 the original author or authors.
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

import java.io.Serializable;
import java.util.function.Supplier;

import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.spring.AssociationToPrimitivesConverter;
import org.jmolecules.spring.IdentifierToPrimitivesConverter;
import org.jmolecules.spring.PrimitivesToAssociationConverter;
import org.jmolecules.spring.PrimitivesToIdentifierConverter;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for JMolecules integration. Registers the following components:
 * <ul>
 * <li>{@link IdentifierToPrimitivesConverter} and {@link PrimitivesToIdentifierConverter} in case not already present
 * in the Spring MVC {@link ConversionService}</li>
 * <li>the same converters on the {@link ConversionService} exposed to {@link RepositoryRestConfigurer}.</li>
 * <li>a {@link BackendIdConverter} using those converters</li>
 * </ol>
 *
 * @author Oliver Drotbohm
 * @since 3.5
 */
@Configuration(proxyBeanMethods = false)
class JMoleculesConfigurer implements WebMvcConfigurer, RepositoryRestConfigurer {

	@Override
	public void addFormatters(FormatterRegistry registry) {

		ConversionService conversionService = (ConversionService) registry;
		Supplier<ConversionService> supplier = () -> conversionService;

		if (!conversionService.canConvert(Identifier.class, String.class)) {
			registry.addConverter(new IdentifierToPrimitivesConverter(supplier));
		}

		if (!conversionService.canConvert(String.class, Identifier.class)) {
			registry.addConverter(new PrimitivesToIdentifierConverter(supplier));
		}
	}

	@Override
	public void configureConversionService(ConfigurableConversionService conversionService) {

		Supplier<ConversionService> supplier = () -> conversionService;

		var primitivesToIdentifierConverter = new PrimitivesToIdentifierConverter(supplier);
		var identifierToPrimitivesConverter = new IdentifierToPrimitivesConverter(supplier);

		conversionService.addConverter(primitivesToIdentifierConverter);
		conversionService.addConverter(identifierToPrimitivesConverter);
		conversionService.addConverter(new AssociationToPrimitivesConverter<>(identifierToPrimitivesConverter));
		conversionService.addConverter(new PrimitivesToAssociationConverter<>(primitivesToIdentifierConverter));
	}

	@Lazy
	@Bean
	BackendIdConverter jMoleculesEntitiesBackendIdConverter(PersistentEntities entities,
			@Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService) {
		return new JMoleculesBackendIdentifierConverter(entities, () -> conversionService.getObject());
	}

	/**
	 * A {@link BackendIdConverter} to convert from and to JMolecules' Identifier
	 *
	 * @author Oliver Drotbohm
	 */
	private static final class JMoleculesBackendIdentifierConverter implements BackendIdConverter {

		private static final TypeDescriptor STRING_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
		private static final ResolvableType IDENTIFIER_TYPE = ResolvableType.forClass(Identifier.class);

		private final PrimitivesToIdentifierConverter identifierConverter;
		private final IdentifierToPrimitivesConverter primitivesConverter;
		private final PersistentEntities entities;

		/**
		 * Creates a new {@link JMoleculesBackendIdentifierConverter} for the given {@link PersistentEntities} and
		 * {@link ConversionService}.
		 *
		 * @param entities must not be {@literal null}.
		 * @param conversionService must not be {@literal null}.
		 */
		public JMoleculesBackendIdentifierConverter(PersistentEntities entities,
				Supplier<? extends ConversionService> conversionService) {

			this.identifierConverter = new PrimitivesToIdentifierConverter(conversionService);
			this.primitivesConverter = new IdentifierToPrimitivesConverter(conversionService);
			this.entities = entities;
		}

		@Override
		public Serializable fromRequestId(String id, Class<?> entityType) {

			PersistentEntity<?, ? extends PersistentProperty<?>> entity = entities.getRequiredPersistentEntity(entityType);
			Class<?> idType = entity.getRequiredIdProperty().getType();

			return (Serializable) identifierConverter.convert(id, TypeDescriptor.forObject(id),
					TypeDescriptor.valueOf(idType));
		}

		@Override
		public String toRequestId(Serializable id, Class<?> entityType) {
			return (String) primitivesConverter.convert(id, TypeDescriptor.forObject(id), STRING_DESCRIPTOR);
		}

		@Override
		public boolean supports(Class<?> delimiter) {

			if (!Entity.class.isAssignableFrom(delimiter)) {
				return false;
			}

			return IDENTIFIER_TYPE.isAssignableFrom(ResolvableType.forClass(delimiter)
					.as(Entity.class)
					.getGeneric(1));
		}
	}
}
