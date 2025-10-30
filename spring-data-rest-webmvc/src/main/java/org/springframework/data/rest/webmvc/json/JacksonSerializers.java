/*
 * Copyright 2025 the original author or authors.
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

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.module.SimpleSerializers;
import tools.jackson.databind.ser.std.StdSerializer;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.rest.webmvc.json.JsonSchema.EnumProperty;
import org.springframework.data.rest.webmvc.json.JsonSchema.JsonSchemaProperty;
import org.springframework.util.Assert;

/**
 * Custom Spring Data REST Jackson serializers.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 5.0
 * @soundtrack Wallis Bird - I Could Be Your Man (Yeah! Wallis Bird Live 2007-2014)
 */
public class JacksonSerializers extends SimpleModule {

	private static final @Serial long serialVersionUID = 4396776390917947147L;

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
		public void serialize(Enum value, JsonGenerator gen, SerializationContext provider) {
			gen.writeString(translator.asText(value));
		}

		@Override
		public JsonSchemaProperty customize(JsonSchemaProperty property, TypeInformation<?> type) {

			List<String> values = new ArrayList<String>();

			for (Object value : type.getType().getEnumConstants()) {
				values.add(translator.asText((Enum<?>) value));
			}

			return ((EnumProperty) property).withValues(values);
		}
	}

	/**
	 * Enum deserializer that uses a resource bundle to resolve enum values.
	 *
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("rawtypes")
	public static class EnumTranslatingDeserializer extends StdDeserializer<Enum> {

		private final EnumTranslator translator;
		private final @Nullable BeanProperty property;

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
		public EnumTranslatingDeserializer(EnumTranslator translator, @Nullable BeanProperty property) {

			super(Enum.class);

			Assert.notNull(translator, "EnumTranslator must not be null");

			this.translator = translator;
			this.property = property;
		}

		@Override
		public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
			return new EnumTranslatingDeserializer(translator, property);
		}

		@Override
		@SuppressWarnings("unchecked")
		public @Nullable Enum deserialize(JsonParser p, DeserializationContext ctxt) {

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
