/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import java.util.Iterator;

import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.AssociationUriResolvingDeserializerModifier.ValueInstantiatorCustomizer;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.CollectionValueInstantiator;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.UriStringDeserializer;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionLikeType;

/**
 * Jackson module to enable aggregate reference resolution for non-aggregate root types. This is primarily useful for
 * any kind of payload mapping DTO that is supposed to be able to map URIs to aggregate roots.
 *
 * @author Oliver Drotbohm
 * @since 3.5
 */
public class AggregateReferenceResolvingModule extends SimpleModule {

	private static final long serialVersionUID = 6002883434719869173L;

	/**
	 * Creates a new {@link AggregateReferenceResolvingModule} using the given {@link UriToEntityConverter} and
	 * {@link ResourceMappings}.
	 *
	 * @param converter must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 */
	public AggregateReferenceResolvingModule(UriToEntityConverter converter, ResourceMappings mappings) {
		setDeserializerModifier(new AggregateReferenceDeserializerModifier(converter, mappings));
	}

	/**
	 * {@link BeanDeserializerModifier} implementation to support URI deserialization into aggregate roots
	 *
	 * @author Oliver Drotbohm
	 */
	static class AggregateReferenceDeserializerModifier extends BeanDeserializerModifier {

		private final UriToEntityConverter converter;
		private final ResourceMappings mappings;

		/**
		 * Creates a new {@link AggregateReferenceDeserializerModifier} for the given {@link UriToEntityConverter} and
		 * {@link ResourceMappings}.
		 *
		 * @param converter must not be {@literal null}.
		 * @param mappings must not be {@literal null}.
		 */
		public AggregateReferenceDeserializerModifier(UriToEntityConverter converter, ResourceMappings mappings) {

			Assert.notNull(converter, "UriToEntityConverter must not be null!");
			Assert.notNull(mappings, "ResourceMappings must not be null!");

			this.converter = converter;
			this.mappings = mappings;
		}

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.deser.BeanDeserializerModifier#updateBuilder(com.fasterxml.jackson.databind.DeserializationConfig, com.fasterxml.jackson.databind.BeanDescription, com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder)
		 */
		@Override
		public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription description,
				BeanDeserializerBuilder builder) {

			// Type is aggregate itself, already handled by AssociationUriResolvingDeserializerModifier
			if (mappings.hasMappingFor(description.getBeanClass())) {
				return builder;
			}

			TypeInformation<?> type = ClassTypeInformation.from(description.getBeanClass());
			ValueInstantiatorCustomizer customizer = new ValueInstantiatorCustomizer(builder.getValueInstantiator(), config);
			Iterator<SettableBeanProperty> properties = builder.getProperties();

			while (properties.hasNext()) {

				SettableBeanProperty property = properties.next();
				String originalPropertyName = coerceOriginalPropertyName(property, description);
				TypeInformation<?> propertyType = type.getProperty(originalPropertyName);

				if (propertyType == null) {
					continue;
				}

				TypeInformation<?> actualType = propertyType.getActualType();

				if (!mappings.exportsMappingFor(actualType.getType())) {
					continue;
				}

				UriStringDeserializer uriStringDeserializer = new UriStringDeserializer(actualType.getType(), converter);
				JsonDeserializer<?> deserializer = wrapIfCollection(propertyType, uriStringDeserializer, config);

				customizer.replacePropertyIfNeeded(builder, property.withValueDeserializer(deserializer));
			}

			return builder;
		}

		private static JsonDeserializer<?> wrapIfCollection(TypeInformation<?> type,
				JsonDeserializer<Object> elementDeserializer, DeserializationConfig config) {

			if (!type.isCollectionLike()) {
				return elementDeserializer;
			}

			CollectionLikeType collectionType = config.getTypeFactory() //
					.constructCollectionLikeType(type.getType(), type.getActualType().getType());
			CollectionValueInstantiator instantiator = new CollectionValueInstantiator(type);

			return new CollectionDeserializer(collectionType, elementDeserializer, null, instantiator);
		}

		/**
		 * Tries to find the internal property name for a {@link SettableBeanProperty} that unfortunately does not allow
		 * accessing the original name anymore.
		 *
		 * @param property must not be {@literal null}.
		 * @param description must not be {@literal null}.
		 * @return
		 */
		private static String coerceOriginalPropertyName(SettableBeanProperty property, BeanDescription description) {

			for (BeanPropertyDefinition properties : description.findProperties()) {
				if (properties.hasName(property.getFullName())) {
					return properties.getInternalName();
				}
			}

			return property.getName();
		}
	}
}
