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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.hateoas.Resource;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class to render JSON schema documents.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class JsonSchema extends Resource<Map<String, JsonSchema.Property>> {

	private final String name;
	private final String description;

	public JsonSchema(String name, String description) {
		super(new HashMap<String, Property>());
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	@JsonProperty("properties")
	@Override
	public Map<String, JsonSchema.Property> getContent() {
		return super.getContent();
	}

	public JsonSchema addProperty(String name, Property property) {
		getContent().put(name, property);
		return this;
	}

	public boolean isArrayProperty(String name) {
		return (getContent().containsKey(name) && getContent().get(name) instanceof ArrayProperty);
	}

	public ArrayProperty getArrayProperty(String name) {
		return (ArrayProperty) getContent().get(name);
	}

	public static class Property {

		private final String type;
		private final String description;
		private final boolean required;

		public Property(String type, String description, boolean required) {
			this.type = type;
			this.description = description;
			this.required = required;
		}

		public String getType() {
			return type;
		}

		public String getDescription() {
			return description;
		}

		public boolean isRequired() {
			return required;
		}
	}

	public static class ArrayProperty extends Property {
		private List<Property> items = new ArrayList<Property>();

		public ArrayProperty(String type, String description, boolean required) {
			super(type, description, required);
		}

		public List<? extends Property> getItems() {
			return items;
		}

		public ArrayProperty setItems(List<Property> items) {
			this.items = items;
			return this;
		}

		public <P extends Property> ArrayProperty addItem(P item) {
			this.items.add(item);
			return this;
		}
	}

}
