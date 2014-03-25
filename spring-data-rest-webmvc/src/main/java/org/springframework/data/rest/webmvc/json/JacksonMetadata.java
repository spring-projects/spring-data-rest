/*
 * Copyright 2014 the original author or authors.
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

import java.util.Iterator;
import java.util.List;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * Value object to abstract Jackson based bean metadata of a given type.
 * 
 * @author Oliver Gierke
 */
public class JacksonMetadata implements Iterable<BeanPropertyDefinition> {

	private final List<BeanPropertyDefinition> definitions;

	/**
	 * Creates a new {@link JacksonMetadata} instance for the given {@link ObjectMapper} and type.
	 * 
	 * @param mapper must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 */
	public JacksonMetadata(ObjectMapper mapper, Class<?> type) {

		Assert.notNull(mapper, "ObjectMapper must not be null!");
		Assert.notNull(type, "Type must not be null!");

		SerializationConfig serializationConfig = mapper.getSerializationConfig();
		JavaType javaType = serializationConfig.constructType(type);
		BeanDescription description = serializationConfig.introspect(javaType);

		this.definitions = description.findProperties();
	}

	/**
	 * Returns the {@link BeanPropertyDefinition} for the given {@link PersistentProperty}.
	 * 
	 * @param property must not be {@literal null}.
	 * @return can be {@literal null} in case there's no Jackson property to be exposed for the given
	 *         {@link PersistentProperty}.
	 */
	public BeanPropertyDefinition getDefinitionFor(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		for (BeanPropertyDefinition definition : definitions) {
			if (definition.getInternalName().equals(property.getName())) {
				return definition;
			}
		}

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<BeanPropertyDefinition> iterator() {
		return definitions.iterator();
	}
}
