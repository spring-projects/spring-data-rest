/*
 * Copyright 2012-2014 the original author or authors.
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

	private final String title;
	private final String description;
	private final SchemaContainer container;
	private final Descriptors descriptors;

	public JsonSchema(String title, String description, Collection<JsonSchemaProperty> properties, Descriptors descriptors) {

		this.title = title;
		this.description = description;
		this.container = new SchemaContainer(properties);
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
	public SchemaContainer getContainer() {
		return container;
	}

	/**
	 * @return the descriptors
	 */
	@JsonUnwrapped
	public Descriptors getDescriptors() {
		return descriptors;
	}

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

	@JsonInclude(Include.NON_EMPTY)
	abstract static class JsonSchemaProperty {

		private final String name;
		private final boolean required;

		protected JsonSchemaProperty(String name, boolean required) {
			this.name = name;
			this.required = required;
		}

		@JsonIgnore
		public String getName() {
			return name;
		}

		private boolean isRequired() {
			return required;
		}
	}

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

	static class Property extends JsonSchemaProperty {

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

			if (type.isCollectionLike()) {

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

	private static List<Class<?>> INTEGER_TYPES = Arrays.<Class<?>> asList(Long.class, long.class, Integer.class,
			int.class, Short.class, short.class);

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
		} else if (Number.class.isAssignableFrom(type)) {
			return "number";
		} else {
			return "object";
		}
	}

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

	static class Item {

		private final String type;
		private final SchemaContainer properties;

		/**
		 * @param type
		 * @param properties
		 */
		public Item(TypeInformation<?> type, Collection<JsonSchemaProperty> properties) {
			this.type = toJsonSchemaType(type);
			this.properties = new SchemaContainer(properties);
		}

		public String getType() {
			return type;
		}

		@JsonUnwrapped
		public SchemaContainer getProperties() {
			return properties;
		}
	}

	@JsonInclude(Include.NON_EMPTY)
	static class SchemaContainer {

		public final Map<String, JsonSchemaProperty> properties;
		public final Collection<String> requiredProperties;

		/**
		 * @param properties
		 */
		public SchemaContainer(Collection<JsonSchemaProperty> properties) {

			this.properties = new HashMap<String, JsonSchema.JsonSchemaProperty>();
			this.requiredProperties = new ArrayList<String>();

			for (JsonSchemaProperty property : properties) {
				this.properties.put(property.getName(), property);

				if (property.isRequired()) {
					this.requiredProperties.add(property.name);
				}
			}
		}
	}
}
