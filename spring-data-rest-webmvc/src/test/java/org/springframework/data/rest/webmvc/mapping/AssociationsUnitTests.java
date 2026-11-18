/*
 * Copyright 2016-present the original author or authors.
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
package org.springframework.data.rest.webmvc.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.annotation.Reference;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.hateoas.Link;

/**
 * @author Oliver Gierke
 * @author Steve Rutherford
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssociationsUnitTests {

	@Mock RepositoryRestConfiguration configuration;
	@Mock ProjectionDefinitionConfiguration projectionDefinitionConfiguration;

	@Mock PersistentEntity<?, ?> entity;
	@Mock PersistentProperty<?> property;

	Associations associations;

	KeyValueMappingContext<?, ?> mappingContext;
	ResourceMappings mappings;

	@BeforeEach
	void setUp() {
		doReturn(projectionDefinitionConfiguration).when(configuration).getProjectionConfiguration();

		this.mappingContext = new KeyValueMappingContext<>();
		this.mappingContext.getPersistentEntity(Root.class);

		this.mappings = new PersistentEntitiesResourceMappings(new PersistentEntities(Arrays.asList(mappingContext)));

		this.associations = new Associations(mappings, configuration);
	}

	@Test
	void rejectsNullMappings() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new Associations(null, configuration));
	}

	@Test
	void rejectsNullConfiguration() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new Associations(mappings, null));
	}

	@Test
	void handlesNullPropertyForLookupTypeCheck() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> associations.isLookupType(null));
	}

	@Test
	void forwardsLookupTypeCheckToConfiguration() {

		doReturn(Root.class).when(property).getActualType();
		assertThat(associations.isLookupType(property)).isFalse();

		doReturn(true).when(configuration).isLookupType(Root.class);
		assertThat(associations.isLookupType(property)).isTrue();
	}

	@Test
	void forwardsIdExposureCheckToConfiguration() {

		doReturn(Root.class).when(entity).getType();
		assertThat(associations.isIdExposed(entity)).isFalse();

		doReturn(true).when(configuration).isIdExposedFor(Root.class);
		assertThat(associations.isIdExposed(entity)).isTrue();
	}

	@Test
	void exposesConfiguredMapping() {
		assertThat(associations.getMappings()).isEqualTo(mappings);
	}

	@Test
	void forwardsMetadataLookupToMappings() {
		assertThat(associations.getMetadataFor(Root.class)).isNotNull();
	}

	@Test
	void detectsAssociationLinks() {

		List<Link> links = associations.getLinksFor(getAssociation(Root.class, "relatedAndExported"), new Path(""));

		assertThat(links).hasSize(1);
		assertThat(links).contains(Link.of("/relatedAndExported", "relatedAndExported"));
	}

	@Test
	void doesNotCreateAssociationLinkIfTargetIsNotExported() {

		List<Link> links = associations.getLinksFor(getAssociation(Root.class, "relatedButNotExported"), new Path(""));

		assertThat(links).hasSize(0);
	}

	@Test // DATAREST-1105
	void detectsProjectionsForAssociationLinks() {

		String projectionParameterName = "projection";

		doReturn(true).when(projectionDefinitionConfiguration).hasProjectionFor(RelatedAndExported.class);
		doReturn(projectionParameterName).when(projectionDefinitionConfiguration).getParameterName();

		List<Link> links = associations.getLinksFor(getAssociation(Root.class, "relatedAndExported"), new Path(""));

		assertThat(links).hasSize(1);
		assertThat(links).contains(Link.of("/relatedAndExported{?" + projectionParameterName + "}", "relatedAndExported"));
	}

	// ---------------------------------------------------------------------------
	// Tests for isUriResolvableAssociation (DATAREST-1195 / issue #1515)
	// ---------------------------------------------------------------------------

	/**
	 * Verifies that {@code isUriResolvableAssociation} returns {@code true} for an association whose target type has a
	 * repository that is exported (the common case — same result as {@code isLinkableAssociation}).
	 */
	@Test // GH-1515
	void isUriResolvableAssociationReturnsTrueWhenTargetRepositoryIsExported() {

		KeyValuePersistentEntity<?, ? extends KeyValuePersistentProperty<?>> rootEntity = mappingContext
				.getRequiredPersistentEntity(Root.class);
		KeyValuePersistentProperty<?> prop = rootEntity.getRequiredPersistentProperty("relatedAndExported");

		assertThat(associations.isUriResolvableAssociation(prop)).isTrue();
	}

	/**
	 * Verifies that {@code isUriResolvableAssociation} returns {@code false} when no repository (and therefore no
	 * {@link org.springframework.data.rest.core.mapping.ResourceMetadata}) exists for the target type. This mirrors the
	 * behaviour of {@code isLinkableAssociation} in the same scenario.
	 * <p>
	 * Note: we use a mock {@link ResourceMappings} that returns {@code null} for {@link RelatedButNotExported} to
	 * simulate the real {@link org.springframework.data.rest.core.mapping.RepositoryResourceMappings} behaviour, which
	 * only adds entities to its cache when a repository exists for them.
	 */
	@Test // GH-1515
	void isUriResolvableAssociationReturnsFalseWhenNoRepositoryExistsForTargetType() {

		KeyValuePersistentEntity<?, ? extends KeyValuePersistentProperty<?>> rootEntity = mappingContext
				.getRequiredPersistentEntity(Root.class);
		KeyValuePersistentProperty<?> prop = rootEntity.getRequiredPersistentProperty("relatedButNotExported");

		// Use a mock ResourceMappings that returns null for RelatedButNotExported,
		// simulating RepositoryResourceMappings when no repository exists for the target type.
		ResourceMappings noRepoMappings = mock(ResourceMappings.class);
		doReturn(null).when(noRepoMappings).getMetadataFor(RelatedButNotExported.class);
		Associations noRepoAssociations = new Associations(noRepoMappings, configuration);

		assertThat(noRepoAssociations.isUriResolvableAssociation(prop)).isFalse();
	}

	/**
	 * Core regression test for DATAREST-1195 / GH-1515.
	 * <p>
	 * When the {@code ANNOTATED} repository detection strategy is used, a repository that is <em>not</em> annotated with
	 * {@code @RepositoryRestResource} is not exported as an HTTP endpoint ({@code isExported() == false}). Before the
	 * fix, {@code isLinkableAssociation} returned {@code false} in this case, which prevented the
	 * {@code UriStringDeserializer} from being registered and caused a {@code JsonMappingException} when a URI string
	 * was submitted for the association property.
	 * <p>
	 * After the fix, {@code isUriResolvableAssociation} returns {@code true} as long as a repository exists for the
	 * target type — regardless of whether that repository is exported as an HTTP endpoint.
	 */
	@Test // GH-1515
	void isUriResolvableAssociationReturnsTrueEvenWhenTargetRepositoryIsNotExportedViaAnnotatedStrategy() {

		// Build a mapping context that knows about both AnnotatedStrategyOwner and its
		// association target UnexportedTarget (which has a repository but is NOT annotated).
		KeyValueMappingContext<?, ?> ctx = new KeyValueMappingContext<>();
		ctx.getPersistentEntity(AnnotatedStrategyOwner.class);
		ctx.getPersistentEntity(UnexportedTarget.class);

		// Use a mock ResourceMappings that:
		//   - returns a non-null but NOT-exported ResourceMetadata for UnexportedTarget,
		//     simulating the ANNOTATED strategy for an un-annotated repository.
		// Note: the owner-level check in isUriResolvableAssociation only looks at the
		// @RestResource(exported = false) annotation on the property itself, NOT at
		// ownerMetadata.isExported(property) (which would transitively check the target type's
		// export status and defeat the purpose of the fix).
		org.springframework.data.rest.core.mapping.ResourceMetadata unexportedMetadata =
				mock(org.springframework.data.rest.core.mapping.ResourceMetadata.class);
		doReturn(false).when(unexportedMetadata).isExported();

		ResourceMappings annotatedMappings = mock(ResourceMappings.class);
		doReturn(unexportedMetadata).when(annotatedMappings).getMetadataFor(UnexportedTarget.class);

		Associations annotatedAssociations = new Associations(annotatedMappings, configuration);

		KeyValuePersistentEntity<?, ? extends KeyValuePersistentProperty<?>> ownerEntity = ctx
				.getRequiredPersistentEntity(AnnotatedStrategyOwner.class);
		KeyValuePersistentProperty<?> prop = ownerEntity.getRequiredPersistentProperty("target");

		// isLinkableAssociation must still return false (no HTTP link should be rendered)
		assertThat(annotatedAssociations.isLinkableAssociation(prop)).isFalse();

		// isUriResolvableAssociation must return true (URI deserialization must still work)
		assertThat(annotatedAssociations.isUriResolvableAssociation(prop)).isTrue();
	}

	/**
	 * Verifies that {@code isUriResolvableAssociation} rejects a {@code null} argument.
	 */
	@Test // GH-1515
	void isUriResolvableAssociationRejectsNullProperty() {
		assertThatIllegalArgumentException().isThrownBy(() -> associations.isUriResolvableAssociation(null));
	}

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Association<? extends PersistentProperty<?>> getAssociation(Class<?> type, String name) {

		KeyValuePersistentEntity<?, ? extends KeyValuePersistentProperty<?>> rootEntity = mappingContext
				.getRequiredPersistentEntity(type);
		KeyValuePersistentProperty<?> property = rootEntity.getRequiredPersistentProperty(name);

		return new Association(property, null);
	}

	// ---------------------------------------------------------------------------
	// Domain model for existing tests
	// ---------------------------------------------------------------------------

	static class Root {
		@Reference RelatedAndExported relatedAndExported;
		@Reference RelatedButNotExported relatedButNotExported;
	}

	@RestResource(exported = true)
	static class RelatedAndExported {}

	static class RelatedButNotExported {}

	// ---------------------------------------------------------------------------
	// Domain model for ANNOTATED-strategy regression tests (GH-1515)
	// ---------------------------------------------------------------------------

	/** Owner entity whose {@code target} association points to an un-annotated (not HTTP-exported) type. */
	static class AnnotatedStrategyOwner {
		@Reference UnexportedTarget target;
	}

	/**
	 * Target entity that has a repository ({@link UnexportedTargetRepository}) but is NOT annotated with
	 * {@code @RepositoryRestResource}, so under the {@code ANNOTATED} strategy it is not exported as an HTTP endpoint.
	 */
	static class UnexportedTarget {}

	/** A repository for {@link UnexportedTarget} — intentionally NOT annotated with {@code @RepositoryRestResource}. */
	interface UnexportedTargetRepository extends CrudRepository<UnexportedTarget, Long> {}

	/**
	 * A {@link ResourceMappings} implementation that reports {@link UnexportedTarget} as having metadata (i.e. a
	 * repository exists) but with {@code isExported() == false}, reproducing the effect of the {@code ANNOTATED}
	 * detection strategy on an un-annotated repository.
	 */
	static class NotExportedTargetResourceMappings extends PersistentEntitiesResourceMappings {

		NotExportedTargetResourceMappings(PersistentEntities entities) {
			super(entities);
		}

		@Override
		public org.springframework.data.rest.core.mapping.ResourceMetadata getMetadataFor(Class<?> type) {

			org.springframework.data.rest.core.mapping.ResourceMetadata base = super.getMetadataFor(type);

			if (base == null || !UnexportedTarget.class.equals(type)) {
				return base;
			}

			// Wrap the metadata to report isExported() == false, simulating the ANNOTATED strategy
			// for a repository that carries no @RepositoryRestResource annotation.
			return new org.springframework.data.rest.core.mapping.ResourceMetadata() {

				@Override public boolean isExported() { return false; }

				@Override public Class<?> getDomainType() { return base.getDomainType(); }

				@Override public org.springframework.hateoas.LinkRelation getRel() { return base.getRel(); }

				@Override public org.springframework.hateoas.LinkRelation getItemResourceRel() { return base.getItemResourceRel(); }

				@Override public org.springframework.data.rest.core.Path getPath() { return base.getPath(); }

				@Override public boolean isPagingResource() { return base.isPagingResource(); }

				@Override public org.springframework.data.rest.core.mapping.ResourceDescription getDescription() { return base.getDescription(); }

				@Override public org.springframework.data.rest.core.mapping.ResourceDescription getItemResourceDescription() { return base.getItemResourceDescription(); }

				@Override public java.util.Optional<Class<?>> getExcerptProjection() { return base.getExcerptProjection(); }

				@Override public org.springframework.data.rest.core.mapping.SearchResourceMappings getSearchResourceMappings() { return base.getSearchResourceMappings(); }

				@Override public org.springframework.data.rest.core.mapping.SupportedHttpMethods getSupportedHttpMethods() { return base.getSupportedHttpMethods(); }

				@Override public org.springframework.data.rest.core.mapping.ResourceMapping getMappingFor(org.springframework.data.mapping.PersistentProperty<?> property) { return base.getMappingFor(property); }

				@Override public boolean isExported(org.springframework.data.mapping.PersistentProperty<?> property) { return base.isExported(property); }

				@Override public org.springframework.data.rest.core.mapping.PropertyAwareResourceMapping getProperty(String mappedPath) { return base.getProperty(mappedPath); }
			};
		}
	}
}
