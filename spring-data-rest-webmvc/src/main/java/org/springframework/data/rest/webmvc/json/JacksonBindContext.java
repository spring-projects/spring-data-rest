/*
 * Copyright 2022-2024 the original author or authors.
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

import java.util.Optional;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.webmvc.json.patch.BindContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link BindContext} that uses a Jackson {@link ObjectMapper} to inspect its metadata to decide whether segments are
 * exposed or not.
 *
 * @author Oliver Drotbohm
 */
class JacksonBindContext implements BindContext {

	private final PersistentEntities entities;
	private final ObjectMapper mapper;
	private final EvaluationContext context;

	/**
	 * Creates a new {@link JacksonBindContext} for the given {@link PersistentEntities} and {@link ObjectMapper}.
	 *
	 * @param entities must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 * @param mapper must not be {@literal null}.
	 */
	public JacksonBindContext(PersistentEntities entities, ConversionService conversionService, ObjectMapper mapper) {

		Assert.notNull(entities, "PersistentEntities must not be null");
		Assert.notNull(mapper, "ObjectMapper must not be null");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.entities = entities;
		this.mapper = mapper;
		this.context = SimpleEvaluationContext.forReadWriteDataBinding().withConversionService(conversionService).build();
	}

	@Override
	public Optional<String> getReadableProperty(String segment, Class<?> type) {

		return getProperty(entities.getPersistentEntity(type)
				.map(it -> MappedProperties.forSerialization(it, mapper))
				.filter(it -> it.isReadableField(segment)), segment);
	}

	@Override
	public Optional<String> getWritableProperty(String segment, Class<?> type) {

		return getProperty(entities.getPersistentEntity(type)
				.map(it -> MappedProperties.forDeserialization(it, mapper))
				.filter(it -> it.isWritableField(segment)), segment);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.json.patch.BindContext#getEvaluationContext()
	 */
	@Override
	public EvaluationContext getEvaluationContext() {
		return context;
	}

	private static Optional<String> getProperty(Optional<MappedProperties> properties, String segment) {

		return properties.map(it -> it.getPersistentProperty(segment))
				.map(PersistentProperty::getName);
	}
}
