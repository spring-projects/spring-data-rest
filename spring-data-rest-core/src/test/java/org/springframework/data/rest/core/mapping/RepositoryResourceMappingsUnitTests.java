/*
 * Copyright 2024-present the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;

/**
 * Unit tests for {@link RepositoryResourceMappings}.
 *
 * @author Christopher Smith
 * @author Steve Rutherford
 */
class RepositoryResourceMappingsUnitTests {

	/**
	 * Tests that when multiple repository interfaces exist for the same domain type, the one annotated with
	 * {@link RepositoryRestResource} is correctly detected and the domain type is exported, regardless of which
	 * repository is returned first by {@link Repositories#getRequiredRepositoryInformation(Class)}.
	 *
	 * @see <a href="https://github.com/spring-projects/spring-data-rest/issues/1169">DATAREST-917</a>
	 */
	@Test // DATAREST-917
	void detectsExportedRepositoryWhenMultipleRepositoriesExistForSameDomainType() {

		// Set up a mapping context with the domain type
		KeyValueMappingContext<?, ?> mappingContext = new KeyValueMappingContext<>();
		mappingContext.getPersistentEntity(MultiRepoEntity.class);

		PersistentEntities entities = new PersistentEntities(Arrays.asList(mappingContext));

		// Use DefaultRepositoryMetadata (real implementation) to avoid Mockito wildcard issues
		DefaultRepositoryMetadata plainMetadata = new DefaultRepositoryMetadata(PlainMultiRepoEntityRepository.class);
		DefaultRepositoryMetadata annotatedMetadata = new DefaultRepositoryMetadata(
				AnnotatedMultiRepoEntityRepository.class);

		// Create RepositoryFactoryInformation stubs using the real metadata
		RepositoryInformation plainRepoInfo = stubRepositoryInformation(plainMetadata);
		RepositoryInformation annotatedRepoInfo = stubRepositoryInformation(annotatedMetadata);

		@SuppressWarnings("unchecked")
		RepositoryFactoryInformation<MultiRepoEntity, UUID> plainFactoryInfo = mock(RepositoryFactoryInformation.class);
		when(plainFactoryInfo.getRepositoryInformation()).thenReturn(plainRepoInfo);

		@SuppressWarnings("unchecked")
		RepositoryFactoryInformation<MultiRepoEntity, UUID> annotatedFactoryInfo = mock(
				RepositoryFactoryInformation.class);
		when(annotatedFactoryInfo.getRepositoryInformation()).thenReturn(annotatedRepoInfo);

		// Register both factory infos in a StaticListableBeanFactory.
		// The plain (non-annotated) repository is registered first to simulate the scenario where
		// Repositories.getRequiredRepositoryInformation() would return the non-annotated one.
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
		beanFactory.addBean("plainMultiRepoEntityRepository", plainFactoryInfo);
		beanFactory.addBean("annotatedMultiRepoEntityRepository", annotatedFactoryInfo);

		// Set up a Repositories mock that returns the plain (non-annotated) repository for the domain type,
		// simulating the bug scenario where the annotated repository is not the primary one.
		Repositories repositories = mock(Repositories.class);
		when(repositories.hasRepositoryFor(MultiRepoEntity.class)).thenReturn(true);
		when(repositories.getRequiredRepositoryInformation(MultiRepoEntity.class)).thenReturn(plainRepoInfo);

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration(
				new ProjectionDefinitionConfiguration(), new MetadataConfiguration(),
				mock(EnumTranslationConfiguration.class));

		// Use the new constructor that takes a ListableBeanFactory - this is the fix for DATAREST-917
		RepositoryResourceMappings mappings = new RepositoryResourceMappings(repositories, entities, configuration,
				beanFactory);

		// The domain type should be exported because the annotated repository was found via the BeanFactory
		ResourceMetadata metadata = mappings.getMetadataFor(MultiRepoEntity.class);

		assertThat(metadata).isNotNull();
		assertThat(metadata.isExported()) //
				.as("Domain type should be exported because an @RepositoryRestResource-annotated repository exists") //
				.isTrue();
	}

	/**
	 * Tests that when only a non-annotated, package-protected repository exists for a domain type, the domain type is
	 * NOT exported (existing behavior is preserved).
	 */
	@Test // DATAREST-917
	void doesNotExportDomainTypeWhenOnlyPackageProtectedRepositoryExists() {

		KeyValueMappingContext<?, ?> mappingContext = new KeyValueMappingContext<>();
		mappingContext.getPersistentEntity(MultiRepoEntity.class);

		PersistentEntities entities = new PersistentEntities(Arrays.asList(mappingContext));

		DefaultRepositoryMetadata plainMetadata = new DefaultRepositoryMetadata(PlainMultiRepoEntityRepository.class);
		RepositoryInformation plainRepoInfo = stubRepositoryInformation(plainMetadata);

		@SuppressWarnings("unchecked")
		RepositoryFactoryInformation<MultiRepoEntity, UUID> plainFactoryInfo = mock(RepositoryFactoryInformation.class);
		when(plainFactoryInfo.getRepositoryInformation()).thenReturn(plainRepoInfo);

		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
		beanFactory.addBean("plainMultiRepoEntityRepository", plainFactoryInfo);

		Repositories repositories = mock(Repositories.class);
		when(repositories.hasRepositoryFor(MultiRepoEntity.class)).thenReturn(true);
		when(repositories.getRequiredRepositoryInformation(MultiRepoEntity.class)).thenReturn(plainRepoInfo);

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration(
				new ProjectionDefinitionConfiguration(), new MetadataConfiguration(),
				mock(EnumTranslationConfiguration.class));

		RepositoryResourceMappings mappings = new RepositoryResourceMappings(repositories, entities, configuration,
				beanFactory);

		ResourceMetadata metadata = mappings.getMetadataFor(MultiRepoEntity.class);

		assertThat(metadata).isNotNull();
		// Package-protected repository without annotation should NOT be exported by DEFAULT strategy
		assertThat(metadata.isExported()) //
				.as("Domain type should NOT be exported when only a package-protected, non-annotated repository exists") //
				.isFalse();
	}

	/**
	 * Verifies that {@link RepositoryResourceMappings#getSearchResourceMappings(Class)} uses the query methods from the
	 * exported (annotated) repository, not from the non-exported one that {@link Repositories} happens to return for
	 * the domain type.
	 * <p>
	 * This is the companion fix to the cache-population fix: even after the correct repository is selected for
	 * exposure, {@code getSearchResourceMappings} must also use that same repository's query methods rather than
	 * falling back to {@code repositories.getRequiredRepositoryInformation(domainType)}, which would return the
	 * non-exported repository and expose none of the annotated repository's search methods.
	 *
	 * @see <a href="https://github.com/spring-projects/spring-data-rest/issues/465">DATAREST-80 / GH-465</a>
	 */
	@Test // GH-465
	void getSearchResourceMappingsUsesExportedRepositoryQueryMethods() throws Exception {

		KeyValueMappingContext<?, ?> mappingContext = new KeyValueMappingContext<>();
		mappingContext.getPersistentEntity(MultiRepoEntity.class);

		PersistentEntities entities = new PersistentEntities(Arrays.asList(mappingContext));

		DefaultRepositoryMetadata plainMetadata = new DefaultRepositoryMetadata(PlainMultiRepoEntityRepository.class);
		DefaultRepositoryMetadata annotatedMetadata = new DefaultRepositoryMetadata(
				AnnotatedMultiRepoEntityRepositoryWithSearch.class);

		// Plain repository has no query methods
		RepositoryInformation plainRepoInfo = stubRepositoryInformation(plainMetadata, Collections.emptyList());

		// The annotated repository exposes a findByName query method
		Method findByNameMethod = AnnotatedMultiRepoEntityRepositoryWithSearch.class.getMethod("findByName",
				String.class);
		RepositoryInformation annotatedRepoInfo = stubRepositoryInformation(annotatedMetadata,
				Collections.singletonList(findByNameMethod));

		@SuppressWarnings("unchecked")
		RepositoryFactoryInformation<MultiRepoEntity, UUID> plainFactoryInfo = mock(RepositoryFactoryInformation.class);
		when(plainFactoryInfo.getRepositoryInformation()).thenReturn(plainRepoInfo);

		@SuppressWarnings("unchecked")
		RepositoryFactoryInformation<MultiRepoEntity, UUID> annotatedFactoryInfo = mock(
				RepositoryFactoryInformation.class);
		when(annotatedFactoryInfo.getRepositoryInformation()).thenReturn(annotatedRepoInfo);

		// Plain (non-annotated) repository registered first — simulates the bug scenario where
		// Repositories.getRequiredRepositoryInformation() returns the wrong (non-exported) repository.
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
		beanFactory.addBean("plainMultiRepoEntityRepository", plainFactoryInfo);
		beanFactory.addBean("annotatedMultiRepoEntityRepositoryWithSearch", annotatedFactoryInfo);

		// Repositories returns the plain (non-exported) repository for the domain type.
		Repositories repositories = mock(Repositories.class);
		when(repositories.hasRepositoryFor(MultiRepoEntity.class)).thenReturn(true);
		when(repositories.getRequiredRepositoryInformation(MultiRepoEntity.class)).thenReturn(plainRepoInfo);

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration(
				new ProjectionDefinitionConfiguration(), new MetadataConfiguration(),
				mock(EnumTranslationConfiguration.class));

		RepositoryResourceMappings mappings = new RepositoryResourceMappings(repositories, entities, configuration,
				beanFactory);

		SearchResourceMappings searchMappings = mappings.getSearchResourceMappings(MultiRepoEntity.class);

		assertThat(searchMappings).isNotNull();
		assertThat(searchMappings.isExported()) //
				.as("Search resource should be exported because the annotated repository has query methods") //
				.isTrue();
		assertThat(searchMappings.getMappedMethod("findByName")) //
				.as("findByName query method from the annotated repository should be discoverable") //
				.isNotNull();
	}

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------

	/**
	 * Creates a stub {@link RepositoryInformation} that delegates metadata methods to the given
	 * {@link DefaultRepositoryMetadata} and returns an empty list of query methods.
	 */
	private static RepositoryInformation stubRepositoryInformation(DefaultRepositoryMetadata metadata) {
		return stubRepositoryInformation(metadata, Collections.emptyList());
	}

	/**
	 * Creates a stub {@link RepositoryInformation} that delegates metadata methods to the given
	 * {@link DefaultRepositoryMetadata} and returns the provided list of query methods.
	 */
	private static RepositoryInformation stubRepositoryInformation(DefaultRepositoryMetadata metadata,
			List<Method> queryMethods) {

		RepositoryInformation info = mock(RepositoryInformation.class);
		doReturn(metadata.getRepositoryInterface()).when(info).getRepositoryInterface();
		doReturn(metadata.getDomainType()).when(info).getDomainType();
		doReturn(metadata.getIdType()).when(info).getIdType();
		doReturn(metadata.getCrudMethods()).when(info).getCrudMethods();
		when(info.isPagingRepository()).thenReturn(metadata.isPagingRepository());
		when(info.getQueryMethods()).thenReturn(queryMethods);
		when(info.getAlternativeDomainTypes()).thenReturn(Collections.emptySet());
		when(info.isReactiveRepository()).thenReturn(false);
		when(info.getFragments()).thenReturn(RepositoryFragments.empty().toSet());
		return info;
	}

	// ---------------------------------------------------------------------------
	// Test domain types
	// ---------------------------------------------------------------------------

	static class MultiRepoEntity {}

	// Package-protected, non-annotated repository — would NOT be exported by DEFAULT strategy
	interface PlainMultiRepoEntityRepository extends CrudRepository<MultiRepoEntity, UUID> {}

	// Annotated public repository — SHOULD be exported
	@RepositoryRestResource
	public interface AnnotatedMultiRepoEntityRepository extends CrudRepository<MultiRepoEntity, UUID> {}

	// Annotated public repository with a search method — used to verify getSearchResourceMappings (GH-465)
	@RepositoryRestResource
	public interface AnnotatedMultiRepoEntityRepositoryWithSearch extends CrudRepository<MultiRepoEntity, UUID> {
		Iterable<MultiRepoEntity> findByName(String name);
	}
}
