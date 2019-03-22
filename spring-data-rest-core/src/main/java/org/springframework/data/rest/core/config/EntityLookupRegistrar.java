/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.rest.core.config;

import java.io.Serializable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.Repository;
import org.springframework.data.rest.core.config.EntityLookupRegistrar.LookupRegistrar.Lookup;
import org.springframework.data.rest.core.support.EntityLookup;

/**
 * Configuration interfaces to ease the configuration of custom {@link EntityLookup}s for repositories.
 * 
 * @author Oliver Gierke
 * @since 2.5
 */
public interface EntityLookupRegistrar {

	/**
	 * Starts building a custom {@link EntityLookup} for the given repository type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	<T, ID extends Serializable, R extends Repository<T, ?>> IdMappingRegistrar<T, R> forRepository(Class<R> type);

	/**
	 * Starts building a custom {@link EntityLookup} for the given repository type and registers the domain type of the
	 * given repository as lookup type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	<T, ID extends Serializable, R extends Repository<T, ?>> IdMappingRegistrar<T, R> forLookupRepository(Class<R> type);

	interface IdMappingRegistrar<T, R extends Repository<T, ?>> {

		/**
		 * Registers the given {@link Converter} to map the entity to its identifying property.
		 * 
		 * @param mapping must not be {@literal null}.
		 * @return
		 */
		<ID extends Serializable> LookupRegistrar<T, ID, R> withIdMapping(Converter<T, ID> mapping);
	}

	/**
	 * Registers an {@link EntityLookup} for the given repository type, identifier mapping and lookup operation.
	 * 
	 * @param type must not be {@literal null}.
	 * @param identifierMapping must not be {@literal null}.
	 * @param lookup must not be {@literal null}.
	 */
	<T, ID extends Serializable, R extends Repository<T, ?>> EntityLookupRegistrar forRepository(Class<R> type,
			Converter<T, ID> identifierMapping, Lookup<R, ID> lookup);

	/**
	 * Registers an {@link EntityLookup} for the given repository type, identifier mapping and lookup operation and
	 * registers the domain type managed by the given repository as lookup type.
	 * 
	 * @param type must not be {@literal null}.
	 * @param identifierMapping must not be {@literal null}.
	 * @param lookup must not be {@literal null}.
	 */
	<T, ID extends Serializable, R extends Repository<T, ?>> EntityLookupRegistrar forValueRepository(Class<R> type,
			Converter<T, ID> identifierMapping, Lookup<R, ID> lookup);

	interface LookupRegistrar<T, ID extends Serializable, R extends Repository<T, ?>> {

		/**
		 * Registers the given {@link Lookup} to obtain entity instances.
		 * 
		 * @param lookup must not be {@literal null}.
		 */
		EntityLookupRegistrar withLookup(Lookup<R, ID> lookup);

		interface Lookup<R extends Repository<? extends Object, ?>, ID> {

			/**
			 * Looks up the entity using the given {@link Repository} and identifier.
			 * 
			 * @param repository
			 * @param identifier
			 * @return
			 */
			Object lookup(R repository, ID identifier);
		}
	}
}
