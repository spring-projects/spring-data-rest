/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import java.util.Arrays;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A Spring HATEOAS {@link Resource} subclass that holds a reference to the entity's {@link PersistentEntity} metadata.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class PersistentEntityResource<T> extends Resource<T> {

	private final PersistentEntity<?, ?> entity;

	public static <T> PersistentEntityResource<T> wrap(PersistentEntity<?, ?> entity, T obj) {
		return new PersistentEntityResource<T>(entity, obj);
	}

	public PersistentEntityResource(PersistentEntity<?, ?> entity, T content, Link... links) {
		this(entity, content, Arrays.asList(links));
	}

	private PersistentEntityResource(PersistentEntity<?, ?> entity, T content, Iterable<Link> links) {
		super(content, links);
		this.entity = entity;
	}

	@JsonIgnore
	public PersistentEntity<?, ?> getPersistentEntity() {
		return entity;
	}
}
