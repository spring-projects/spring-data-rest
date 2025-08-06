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
package org.springframework.data.rest.webmvc.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.rest.webmvc.json.JsonSchema.EnumProperty;
import org.springframework.data.rest.webmvc.json.JsonSchema.JsonSchemaProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Custom Spring Data REST Jackson serializers.
 *
 * @author Oliver Gierke
 * @author Doug Busley
 * @soundtrack Wallis Bird - I Could Be Your Man (Yeah! Wallis Bird Live 2007-2014)
 * @since 2.4
 */
public class JacksonSerializers extends SimpleModule {

	private static final long serialVersionUID = 4396776390917947147L;

	/**
	 * Creates a new {@link JacksonSerializers} with the given {@link EnumTranslator}.
	 *
	 * @param translator must not be {@literal null}.
	 */
	public JacksonSerializers(EnumTranslator translator) {

		Assert.notNull(translator, "EnumTranslator must not be null");

		SimpleSerializers serializers = new SimpleSerializers();
		serializers.addSerializer(Enum.class, new EnumTranslatingSerializer(translator));
		setSerializers(serializers);

		SimpleDeserializers deserializers = new SimpleDeserializers();
		deserializers.addDeserializer(Enum.class, new EnumTranslatingDeserializer(translator));
		setDeserializers(deserializers);
	}

	/**
	 * An enum serializer to translate raw enum values into values resolved through a resource bundle.
	 *
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("rawtypes")
	public static class EnumTranslatingSerializer extends StdSerializer<Enum> implements JsonSchemaPropertyCustomizer {

		private static final long serialVersionUID = -6706924011396258646L;

		private final EnumTranslator translator;

		/**
		 * Creates a new {@link EnumTranslatingSerializer} using the given {@link EnumTranslator}.
		 *
		 * @param translator must not be {@literal null}.
		 */
		public EnumTranslatingSerializer(EnumTranslator translator) {

			super(Enum.class);

			Assert.notNull(translator, "EnumTranslator must not be null");

			this.translator = translator;
		}

		@Override
		public void serialize(Enum value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeString(translator.asText(value));
		}

		@Override
		public JsonSchemaProperty customize(JsonSchemaProperty property, TypeInformation<?> type) {

			List<String> values = new ArrayList<String>();

			for (Object value : type.getType().getEnumConstants()) {
				values.add(translator.asText((Enum<?>) value));
			}

			if (property instanceof EnumProperty) {
				return ((EnumProperty) property).withValues(values);
			}

			property.items = Stream.of(new Object[][]{{"enum", values}, {"type", "string"}})
					.collect(Collectors.toMap(data -> (String) data[0], data -> data[1]));

			return property;
		}
	}

	/**
	 * Enum deserializer that uses a resource bundle to resolve enum values.
	 *
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("rawtypes")
	public static class EnumTranslatingDeserializer extends StdDeserializer<Enum> implements ContextualDeserializer {

		private static final long serialVersionUID = 5305284644923180079L;

		private final EnumTranslator translator;
		private final BeanProperty property;

		/**
		 * Creates a new {@link EnumTranslatingDeserializer} using the given {@link EnumTranslator}.
		 *
		 * @param translator must not be {@literal null}.
		 */
		public EnumTranslatingDeserializer(EnumTranslator translator) {
			this(translator, null);
		}

		/**
		 * Creates a new {@link EnumTranslatingDeserializer} using the given {@link EnumTranslator} and {@link BeanProperty}
		 * .
		 *
		 * @param translator must not be {@literal null}.
		 * @param property can be {@literal null}.
		 */
		public EnumTranslatingDeserializer(EnumTranslator translator, BeanProperty property) {

			super(Enum.class);

			Assert.notNull(translator, "EnumTranslator must not be null");

			this.translator = translator;
			this.property = property;
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
				throws JsonMappingException {
			return new EnumTranslatingDeserializer(translator, property);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Enum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			if (property == null) {
				throw new IllegalStateException("Can only translate enum with property information");
			}

			return translator.fromText((Class<? extends Enum<?>>) getActualType(property.getType()).getRawClass(),
					p.getText());
		}

		/**
		 * Returns the value types for containers or the original type otherwise.
		 *
		 * @param type must not be {@literal null}.
		 * @return
		 */
		private static JavaType getActualType(JavaType type) {
			return type.isContainerType() ? type.getContentType() : type;
		}
	}
}
