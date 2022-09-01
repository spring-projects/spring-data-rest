/*
 * Copyright 2022 the original author or authors.
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

import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.webmvc.json.patch.BindContext;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link BindContextFactory} based on {@link PersistentEntities}.
 *
 * @author Oliver Drotbohm
 */
public class PersistentEntitiesBindContextFactory implements BindContextFactory {

	private final PersistentEntities entities;

	/**
	 * Creates a new {@link PersistentEntitiesBindContextFactory} for the given {@link PersistentEntities}.
	 *
	 * @param entities must not be {@literal null}.
	 */
	public PersistentEntitiesBindContextFactory(PersistentEntities entities) {

		Assert.notNull(entities, "PersistentEntities must not be null!");

		this.entities = entities;
	}

	@Override
	public BindContext getBindContextFor(ObjectMapper mapper) {
		return new JacksonBindContext(entities, mapper);
	}
}
