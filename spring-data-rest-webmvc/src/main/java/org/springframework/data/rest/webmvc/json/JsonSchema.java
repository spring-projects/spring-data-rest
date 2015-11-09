/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.rest.core.config.JsonSchemaFormat;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Model class to render JSON schema documents.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@JsonInclude(Include.NON_EMPTY)
public class JsonSchema {

	private static List<Class<?>> INTEGER_TYPES = Arrays.<Class<?>> asList(Long.class, long.class, Integer.class,
			int.class, Short.class, short.class);

	private final String title;
	private final String description;
	private final PropertiesContainer container;
	private final Descriptors descriptors;

	/**
	 * Creates a new {@link JsonSchema} instance for the given title, description, {@link JsonSchemaProperty}s and
	 * {@link Descriptors}.
	 * 
	 * @param title must not be {@literal null} or empty.
	 * @param description can be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @param descriptors must not be {@literal null}.
	 */
	public JsonSchema(String title, String description, Collection<JsonSchemaProperty<?>> properties,
			Descriptors descriptors) {

		Assert.hasText(title, "Title must not be null or empty!");
		Assert.notNull(properties, "JsonSchemaProperties must not be null!");
		Assert.notNull(descriptors, "Desciptors must not be null!");

		this.title = title;
		this.description = description;
		this.container = new PropertiesContainer(properties);
		this.descriptors = descriptors;
	}

	@JsonProperty("$schema")
	public String getSchema() {
		return "http://json-schema.org/draft-04/schema#";
	}

	public String getType() {
		return "object";
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	@JsonUnwrapped
	public PropertiesContainer getContainer() {
		return container;
	}

	/**
	 * @return the descriptors
	 */
	@JsonUnwrapped
	public Descriptors getDescriptors() {
		return descriptors;
	}

	/**
	 * Turns the given {@link TypeInformation} into a JSON Schema type string.
	 * 
	 * @param typeInformation
	 * @return
	 * @see http://json-schema.org/latest/json-schema-core.html#anchor8
	 */
	private static String toJsonSchemaType(TypeInformation<?> typeInformation) {

		Class<?> type = typeInformation.getType();

		if (type == null) {
			return null;
		} else if (typeInformation.isCollectionLike()) {
			return "array";
		} else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
			return "boolean";
		} else if (String.class.equals(type) || isDate(typeInformation) || type.isEnum()) {
			return "string";
		} else if (INTEGER_TYPES.contains(type)) {
			return "integer";
		} else if (ClassUtils.isAssignable(Number.class, type)) {
			return "number";
		} else {
			return "object";
		}
	}

	/**
	 * Returns whether the given {@link TypeInformation} represents a date.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static boolean isDate(TypeInformation<?> type) {

		Class<?> rawType = type.getType();

		if (Date.class.equals(rawType)) {
			return true;
		}

		for (String datePackage : Arrays.asList("java.time", "org.threeten.bp", "org.joda.time")) {
			if (rawType.getName().startsWith(datePackage)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * A JSON Schema item.
	 *
	 * @author Oliver Gierke
	 */
	static class Item {

		private final String type;
		private final PropertiesContainer properties;

		/**
		 * Creates a new {@link Item} for the given {@link TypeInformation} and properties.
		 * 
		 * @param type must not be {@literal null}.
		 * @param properties must not be {@literal null}.
		 */
		public Item(TypeInformation<?> type, Collection<JsonSchemaProperty<?>> properties) {

			this.type = toJsonSchemaType(type);
			this.properties = new PropertiesContainer(properties);
		}

		public String getType() {
			return type;
		}

		@JsonUnwrapped
		public PropertiesContainer getProperties() {
			return properties;
		}
	}

	/**
	 * Value object to represent a generic container of properties.
	 *
	 * @author Oliver Gierke
	 * @since 2.3
	 */
	@JsonInclude(Include.NON_EMPTY)
	static class PropertiesContainer {

		public final Map<String, JsonSchemaProperty<?>> properties;
		public final Collection<String> requiredProperties;

		/**
		 * Creates a new {@link PropertiesContainer} for the given {@link JsonSchemaProperty}s.
		 * 
		 * @param properties must not be {@literal null}.
		 */
		public PropertiesContainer(Collection<JsonSchemaProperty<?>> properties) {

			Assert.notNull(properties, "JsonSchemaPropertys must not be null!");

			this.properties = new HashMap<String, JsonSchema.JsonSchemaProperty<?>>();
			this.requiredProperties = new ArrayList<String>();

			for (JsonSchemaProperty<?> property : properties) {
				this.properties.put(property.getName(), property);

				if (property.isRequired()) {
					this.requiredProperties.add(property.name);
				}
			}
		}
	}

	/**
	 * Value object to abstract a {@link Map} of JSON Schema descriptors.
	 *
	 * @author Oliver Gierke
	 */
	static class Descriptors {

		private final Map<String, Item> descriptors;

		public Descriptors() {
			this.descriptors = new HashMap<String, Item>();
		}

		/**
		 * @return the descriptors
		 */
		public Map<String, Item> getDescriptors() {
			return descriptors;
		}

		boolean hasDescriptorFor(TypeInformation<?> type) {
			return this.descriptors.containsKey(typeKey(type));
		}

		String addDescriptor(TypeInformation<?> type, Item item) {

			String reference = typeKey(type);
			this.descriptors.put(reference, item);

			return reference;
		}

		static String getReference(TypeInformation<?> type) {
			return String.format("#/descriptors/%s", typeKey(type));
		}

		static String typeKey(TypeInformation<?> type) {
			return StringUtils.uncapitalize(type.getActualType().getType().getSimpleName());
		}
	}

	/**
	 * Base class for all property implementations.
	 *
	 * @author Oliver Gierke
	 * @since 2.3
	 */
	@JsonInclude(Include.NON_EMPTY)
	abstract static class JsonSchemaProperty<T extends JsonSchemaProperty<T>> {

		private final String name;
		private final boolean required;

		private boolean readOnly;

		protected JsonSchemaProperty(String name, boolean required) {

			this.name = name;
			this.required = required;
			this.readOnly = false;
		}

		@JsonIgnore
		public String getName() {
			return name;
		}

		private boolean isRequired() {
			return required;
		}

		public boolean isReadOnly() {
			return readOnly;
		}

		@SuppressWarnings("unchecked")
		protected T withReadOnly() {
			this.readOnly = true;
			return (T) this;
		}
	}

	/**
	 * A JSON Schema property
	 *
	 * @author Oliver Gierke
	 * @since 2.3
	 */
	static class Property extends JsonSchemaProperty<Property> {

		private static final TypeInformation<?> STRING_TYPE_INFORMATION = ClassTypeInformation.from(String.class);

		public String description;
		public String type;
		public JsonSchemaFormat format;
		public String pattern;
		public Boolean uniqueItems;
		public @JsonProperty("$ref") String reference;
		public Map<String, String> items;

		public Property(String name, String description, boolean required) {

			super(name, required);

			this.description = description;
		}

		Property with(TypeInformation<?> type) {

			this.type = toJsonSchemaType(type);

			if (isDate(type)) {
				return with(JsonSchemaFormat.DATE_TIME);
			}

			if (type.isCollectionLike() && !JsonSchemaFormat.URI.equals(format)) {

				if (Set.class.equals(type.getType())) {
					this.uniqueItems = true;
				}

				this.items = Collections.singletonMap("type", toJsonSchemaType(type.getActualType()));
			}

			return this;
		}

		Property with(JsonSchemaFormat format) {

			this.format = format;
			return with(STRING_TYPE_INFORMATION);
		}

		Property asAssociation() {

			this.items = null;
			this.uniqueItems = null;

			return with(JsonSchemaFormat.URI);
		}

		Property with(Pattern pattern) {
			this.pattern = pattern.toString();
			return with(STRING_TYPE_INFORMATION);
		}

		Property with(TypeInformation<?> type, String reference) {

			if (type.isCollectionLike()) {

				if (Set.class.equals(type.getType())) {
					this.uniqueItems = true;
				}

				this.type = toJsonSchemaType(type);
				this.items = Collections.singletonMap("$ref", reference);

				return this;

			} else {
				this.reference = reference;
				return this;
			}
		}
	}

	/**
	 * A {@link Property} representing enumerations. Will cause all valid values to be rendered in a nested
	 * {@literal enum} property.
	 *
	 * @author Oliver Gierke
	 * @since 2.3
	 */
	static class EnumProperty extends Property {

		private final List<String> values;

		public EnumProperty(String name, Class<?> type, String description, boolean required) {

			super(name, description, required);

			this.values = new ArrayList<String>();

			for (Object value : type.getEnumConstants()) {
				this.values.add(value.toString());
			}
		}

		@JsonProperty("enum")
		public List<String> getValues() {
			return values;
		}
	}
}
