/*
 * Copyright 2015-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.EntityLookupRegistrar.LookupRegistrar.Lookup;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.util.MethodInvocationRecorder;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;
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

	@Override
	public <T, ID, R extends Repository<T, ?>> EntityLookupRegistrar forRepository(Class<R> repositoryType,
			Converter<T, ID> converter, Lookup<R, ID> lookup) {

		new MappingBuilder<T, ID, R>(repositoryType).withIdMapping(converter).withLookup(lookup);

		return this;
	}

	@Override
	public <T, ID, R extends Repository<T, ?>> IdMappingRegistrar<T, R> forLookupRepository(Class<R> type) {

		this.lookupTypes.add(AbstractRepositoryMetadata.getMetadata(type).getDomainType());

		return forRepository(type);
	}

	@Override
	public <T, ID, R extends Repository<T, ?>> IdMappingRegistrar<T, R> forRepository(Class<R> type) {
		return new MappingBuilder<T, ID, R>(type);
	}

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
	private class MappingBuilder<T, ID, R extends Repository<T, ?>>
			implements LookupRegistrar<T, ID, R>, IdMappingRegistrar<T, R> {

		private final Class<R> repositoryType;
		private Converter<T, ID> idMapping;

		/**
		 * Creates a new {@link MappingBuilder} for the given repository type.
		 *
		 * @param type must not be {@literal null}.
		 */
		public MappingBuilder(Class<R> type) {

			Assert.notNull(type, "Repository type must not be null");

			this.repositoryType = type;
		}

		/**
		 * Creates a new {@link MappingBuilder} using the given repository type and identifier mapping.
		 *
		 * @param repositoryType must not be {@literal null}.
		 * @param mapping must not be {@literal null}.
		 */
		private MappingBuilder(Class<R> repositoryType, Converter<T, ID> mapping) {
			this(repositoryType);
			Assert.notNull(mapping, "Converter must not be null");
			this.idMapping = mapping;
		}

		@Override
		@SuppressWarnings("unchecked")
		public EntityLookupRegistrar withLookup(Lookup<R, ID> lookup) {

			EntityLookupConfiguration.this.lookupInformation
					.add((LookupInformation<Object, Object, Repository<? extends Object, ?>>) new LookupInformation<T, ID, R>(
							repositoryType, idMapping, lookup));

			return EntityLookupConfiguration.this;
		}

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

		Assert.notNull(repositories, "Repositories must not be null");

		return lookupInformation.stream() //
				.map(it -> new RepositoriesEntityLookup<>(repositories, it)) //
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
		private final Optional<String> lookupProperty;

		/**
		 * Creates a new {@link RepositoriesEntityLookup} for the given {@link Repositories} and {@link LookupInformation}.
		 *
		 * @param repositories must not be {@literal null}.
		 * @param lookupInformation must not be {@literal null}.
		 */
		@SuppressWarnings("unchecked")
		public RepositoriesEntityLookup(Repositories repositories,
				LookupInformation<Object, Object, Repository<? extends T, ?>> lookupInformation) {
			Assert.notNull(repositories, "Repositories must not be null");
			Assert.notNull(lookupInformation, "LookupInformation must not be null");
			RepositoryInformation information = //
					repositories.getRepositoryInformation(lookupInformation.repositoryType)
							.orElseThrow(() -> new IllegalStateException(
									"No repository found for type " + lookupInformation.repositoryType.getName()));
			this.domainType = information.getDomainType();
			this.lookupInfo = lookupInformation;
			this.repository = (Repository<? extends T, ?>) //
			repositories.getRepositoryFor(information.getDomainType()).orElseThrow(() -> new IllegalStateException(
					"No repository found for type " + information.getDomainType().getName()));
			this.lookupProperty = //
					Optional.of(domainType).flatMap(it -> //
					//
					MethodInvocationRecorder.forProxyOf(it).record(lookupInfo.identifierMapping::convert).getPropertyPath());
		}

		@Override
		public Object getResourceIdentifier(T entity) {
			return lookupInfo.getIdentifierMapping().convert(entity);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Optional<T> lookupEntity(Object id) {

			Object result = lookupInfo.getLookup().lookup(repository, id);

			return Optional.class.isInstance(result) ? (Optional<T>) result : Optional.ofNullable((T) result);
		}

		@Override
		public boolean supports(Class<?> delimiter) {
			return domainType.isAssignableFrom(delimiter);
		}

		public Optional<String> getLookupProperty() {
			return this.lookupProperty;
		}
	}

	private static final class LookupInformation<T, ID, R extends Repository<? extends T, ?>> {

		private final Class<R> repositoryType;
		private final Converter<T, ID> identifierMapping;
		private final Lookup<R, ID> lookup;

		public LookupInformation(Class<R> repositoryType, Converter<T, ID> identifierMapping,
				Lookup<R, ID> lookup) {

			Assert.notNull(repositoryType, "Repository type must not be null");
			Assert.notNull(identifierMapping, "Identifier mapping must not be null");
			Assert.notNull(lookup, "Lookup must not be null");

			this.repositoryType = repositoryType;
			this.identifierMapping = identifierMapping;
			this.lookup = lookup;
		}

		public Class<R> getRepositoryType() {
			return this.repositoryType;
		}

		public Converter<T, ID> getIdentifierMapping() {
			return this.identifierMapping;
		}

		public Lookup<R, ID> getLookup() {
			return this.lookup;
		}

		@Override
		public boolean equals(@Nullable final java.lang.Object o) {

			if (o == this) {
				return true;
			}

			if (!(o instanceof EntityLookupConfiguration.LookupInformation)) {
				return false;
			}

			LookupInformation<?, ?, ?> that = (LookupInformation<?, ?, ?>) o;

			return Objects.equals(getRepositoryType(), that.getRepositoryType()) //
					&& Objects.equals(getIdentifierMapping(), that.getIdentifierMapping()) //
					&& Objects.equals(getLookup(), that.getLookup());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getRepositoryType(), getIdentifierMapping(), getLookup());
		}

		@Override
		public java.lang.String toString() {
			return "EntityLookupConfiguration.LookupInformation(repositoryType=" + this.getRepositoryType()
					+ ", identifierMapping=" + this.getIdentifierMapping() + ", lookup=" + this.getLookup() + ")";
		}
	}
}
