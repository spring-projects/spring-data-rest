/*
 * Copyright 2015-2020 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.EntityLookupRegistrar.LookupRegistrar.Lookup;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;

/**
 * Configuration instance to implement {@link EntityLookupRegistrar}. Exposed via
 * {@link RepositoryRestConfiguration#withEntityLookup()}.
 *
 * @author Oliver Gierke
 * @since 2.5
 */
class EntityLookupConfiguration implements EntityLookupRegistrar {

	private final List<LookupInformation<Object, Object, Repository<? extends Object, ?>>> lookupInformation = new ArrayList<>();
	private final List<Class<?>> lookupTypes = new ArrayList<>();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.config.EntityLookupRegistrar#forRepository(java.lang.Class, org.springframework.core.convert.converter.Converter, org.springframework.data.rest.core.config.EntityLookupRegistrar.LookupRegistrar.Lookup)
	 */
	@Override
	public <T, ID, R extends Repository<T, ?>> EntityLookupRegistrar forRepository(Class<R> repositoryType,
			Converter<T, ID> converter, Lookup<R, ID> lookup) {

		new MappingBuilder<T, ID, R>(repositoryType).withIdMapping(converter).withLookup(lookup);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.config.EntityLookupRegistrar#forValueRepository(java.lang.Class)
	 */
	@Override
	public <T, ID, R extends Repository<T, ?>> IdMappingRegistrar<T, R> forLookupRepository(Class<R> type) {
		this.lookupTypes.add(AbstractRepositoryMetadata.getMetadata(type).getDomainType());
		return forRepository(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.config.EntityLookupRegistrar#forRepository(java.lang.Class)
	 */
	@Override
	public <T, ID, R extends Repository<T, ?>> IdMappingRegistrar<T, R> forRepository(Class<R> type) {
		return new MappingBuilder<T, ID, R>(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.config.EntityLookupRegistrar#forValueRepository(java.lang.Class, org.springframework.core.convert.converter.Converter, org.springframework.data.rest.core.config.EntityLookupRegistrar.LookupRegistrar.Lookup)
	 */
	@Override
	public <T, ID, R extends Repository<T, ?>> EntityLookupRegistrar forValueRepository(Class<R> type,
			Converter<T, ID> identifierMapping, Lookup<R, ID> lookup) {

		this.lookupTypes.add(AbstractRepositoryMetadata.getMetadata(type).getDomainType());
		return forRepository(type, identifierMapping, lookup);
	}

	/**
	 * Custom builder implementation to back {@link LookupRegistrar} and {@link IdMappingRegistrar}.
	 *
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	private class MappingBuilder<T, ID, R extends Repository<T, ?>>
			implements LookupRegistrar<T, ID, R>, IdMappingRegistrar<T, R> {

		private @NonNull final Class<R> repositoryType;
		private Converter<T, ID> idMapping;

		/**
		 * Creates a new {@link MappingBuilder} using the given repository type and identifier mapping.
		 *
		 * @param repositoryType must not be {@literal null}.
		 * @param mapping must not be {@literal null}.
		 */
		private MappingBuilder(Class<R> repositoryType, Converter<T, ID> mapping) {

			this(repositoryType);

			Assert.notNull(mapping, "Converter must not be null!");

			this.idMapping = mapping;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.config.EntityLookupRegistrar.LookupRegistrar#withLookup(org.springframework.data.rest.core.config.EntityLookupRegistrar.LookupRegistrar.Lookup)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public EntityLookupRegistrar withLookup(Lookup<R, ID> lookup) {

			EntityLookupConfiguration.this.lookupInformation
					.add((LookupInformation<Object, Object, Repository<? extends Object, ?>>) new LookupInformation<T, ID, R>(
							repositoryType, idMapping, lookup));

			return EntityLookupConfiguration.this;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.config.EntityLookupRegistrar.IdMappingRegistrar#withIdMapping(org.springframework.core.convert.converter.Converter)
		 */
		@Override
		public <ID2> LookupRegistrar<T, ID2, R> withIdMapping(Converter<T, ID2> idMapping) {
			return new MappingBuilder<T, ID2, R>(repositoryType, idMapping);
		}
	}

	/**
	 * Returns the {@link EntityLookup}s registered on this configuration.
	 *
	 * @param repositories must not be {@literal null}.
	 * @return
	 */
	public List<EntityLookup<?>> getEntityLookups(Repositories repositories) {

		Assert.notNull(repositories, "Repositories must not be null!");

		return lookupInformation.stream()//
				.map(it -> new RepositoriesEntityLookup<>(repositories, it))//
				.collect(StreamUtils.toUnmodifiableList());
	}

	public boolean isLookupType(Class<?> type) {
		return this.lookupTypes.contains(type);
	}

	/**
	 * An {@link EntityLookup} backed by a repository instance and a {@link LookupInformation}.
	 *
	 * @author Oliver Gierke
	 */
	private static class RepositoriesEntityLookup<T> implements EntityLookup<T> {

		private final LookupInformation<Object, Object, Repository<? extends T, ?>> lookupInfo;
		private final Repository<? extends T, ?> repository;
		private final Class<?> domainType;

		/**
		 * Creates a new {@link RepositoriesEntityLookup} for the given {@link Repositories} and {@link LookupInformation}.
		 *
		 * @param repositories must not be {@literal null}.
		 * @param lookupInformation must not be {@literal null}.
		 */
		@SuppressWarnings("unchecked")
		public RepositoriesEntityLookup(Repositories repositories,
				LookupInformation<Object, Object, Repository<? extends T, ?>> lookupInformation) {

			Assert.notNull(repositories, "Repositories must not be null!");
			Assert.notNull(lookupInformation, "LookupInformation must not be null!");

			RepositoryInformation information = repositories.getRepositoryInformation(lookupInformation.repositoryType)//
					.orElseThrow(() -> new IllegalStateException(
							"No repository found for type " + lookupInformation.repositoryType.getName() + "!"));

			this.domainType = information.getDomainType();
			this.lookupInfo = lookupInformation;
			this.repository = (Repository<? extends T, ?>) repositories.getRepositoryFor(information.getDomainType())//
					.orElseThrow(() -> new IllegalStateException(
							"No repository found for type " + information.getDomainType().getName() + "!"));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.support.EntityLookup#getResourceIdentifier(java.lang.Object)
		 */
		@Override
		public Object getResourceIdentifier(T entity) {
			return lookupInfo.getIdentifierMapping().convert(entity);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.support.EntityLookup#lookupEntity(java.io.Serializable)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public Optional<T> lookupEntity(Object id) {

			Object result = lookupInfo.getLookup().lookup(repository, id);

			return Optional.class.isInstance(result) ? (Optional<T>) result : Optional.ofNullable((T) result);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
		 */
		@Override
		public boolean supports(Class<?> delimiter) {
			return domainType.isAssignableFrom(delimiter);
		}
	}

	@Value
	private static class LookupInformation<T, ID, R extends Repository<? extends T, ?>> {

		private final Class<R> repositoryType;
		private final Converter<T, ID> identifierMapping;
		private final Lookup<R, ID> lookup;
	}
}
