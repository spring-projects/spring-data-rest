/*
 * Copyright 2012-present the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Unit tests for {@link InverseSideAssociationDetector}.
 *
 * <p>Verifies that the detector correctly identifies the inverse (non-owning) side of JPA bidirectional associations
 * based on the presence of a non-empty {@code mappedBy} attribute on {@code @OneToMany}, {@code @ManyToMany}, or
 * {@code @OneToOne}.
 *
 * @author Steve Rutherford
 * @see <a href="https://github.com/spring-projects/spring-data-rest/issues/1810">GitHub Issue #1810</a>
 */
class InverseSideAssociationDetectorUnitTests {

	/**
	 * A {@code @OneToMany(mappedBy = "library")} property is the inverse side — writes must be rejected.
	 */
	@Test
	void detectsOneToManyMappedByAsInverseSide() {

		OneToMany annotation = oneToManyMappedBy("library");
		PersistentProperty<?> property = propertyWithAnnotation(OneToMany.class, annotation);

		assertThat(InverseSideAssociationDetector.isInverseSide(property)).isTrue();
	}

	/**
	 * A {@code @ManyToMany(mappedBy = "authors")} property is the inverse side — writes must be rejected.
	 */
	@Test
	void detectsManyToManyMappedByAsInverseSide() {

		ManyToMany annotation = manyToManyMappedBy("authors");
		PersistentProperty<?> property = propertyWithAnnotation(ManyToMany.class, annotation);

		assertThat(InverseSideAssociationDetector.isInverseSide(property)).isTrue();
	}

	/**
	 * A {@code @OneToOne(mappedBy = "passport")} property is the inverse side — writes must be rejected.
	 */
	@Test
	void detectsOneToOneMappedByAsInverseSide() {

		OneToOne annotation = oneToOneMappedBy("passport");
		PersistentProperty<?> property = propertyWithAnnotation(OneToOne.class, annotation);

		assertThat(InverseSideAssociationDetector.isInverseSide(property)).isTrue();
	}

	/**
	 * A {@code @ManyToOne} property is always the owning side — writes are allowed.
	 */
	@Test
	void doesNotDetectManyToOneAsInverseSide() {

		PersistentProperty<?> property = mock(PersistentProperty.class);
		// ManyToOne has no mappedBy attribute; findAnnotation returns null for all checked types
		when(property.findAnnotation(any())).thenReturn(null);

		assertThat(InverseSideAssociationDetector.isInverseSide(property)).isFalse();
	}

	/**
	 * A {@code @ManyToMany} without {@code mappedBy} (owning side) is not the inverse side — writes are allowed.
	 */
	@Test
	void doesNotDetectOwningManyToManyAsInverseSide() {

		ManyToMany annotation = manyToManyMappedBy(""); // empty mappedBy = owning side
		PersistentProperty<?> property = propertyWithAnnotation(ManyToMany.class, annotation);

		assertThat(InverseSideAssociationDetector.isInverseSide(property)).isFalse();
	}

	/**
	 * A {@code @OneToMany} without {@code mappedBy} (owning side via join table) is not the inverse side.
	 */
	@Test
	void doesNotDetectOneToManyWithoutMappedByAsInverseSide() {

		OneToMany annotation = oneToManyMappedBy(""); // empty mappedBy = owning side
		PersistentProperty<?> property = propertyWithAnnotation(OneToMany.class, annotation);

		assertThat(InverseSideAssociationDetector.isInverseSide(property)).isFalse();
	}

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static <A extends Annotation> PersistentProperty<?> propertyWithAnnotation(Class<A> type, A annotation) {

		PersistentProperty<?> property = mock(PersistentProperty.class);

		// Return the annotation only for the matching type; null for all others
		when(property.findAnnotation(any())).thenAnswer(invocation -> {
			Class<?> requested = invocation.getArgument(0);
			return requested.equals(type) ? annotation : null;
		});

		return property;
	}

	private static OneToMany oneToManyMappedBy(String mappedBy) {
		return new OneToMany() {
			@Override public Class<? extends Annotation> annotationType() { return OneToMany.class; }
			@Override public Class<?> targetEntity() { return void.class; }
			@Override public jakarta.persistence.CascadeType[] cascade() { return new jakarta.persistence.CascadeType[0]; }
			@Override public jakarta.persistence.FetchType fetch() { return jakarta.persistence.FetchType.LAZY; }
			@Override public String mappedBy() { return mappedBy; }
			@Override public boolean orphanRemoval() { return false; }
		};
	}

	private static ManyToMany manyToManyMappedBy(String mappedBy) {
		return new ManyToMany() {
			@Override public Class<? extends Annotation> annotationType() { return ManyToMany.class; }
			@Override public Class<?> targetEntity() { return void.class; }
			@Override public jakarta.persistence.CascadeType[] cascade() { return new jakarta.persistence.CascadeType[0]; }
			@Override public jakarta.persistence.FetchType fetch() { return jakarta.persistence.FetchType.LAZY; }
			@Override public String mappedBy() { return mappedBy; }
		};
	}

	private static OneToOne oneToOneMappedBy(String mappedBy) {
		return new OneToOne() {
			@Override public Class<? extends Annotation> annotationType() { return OneToOne.class; }
			@Override public Class<?> targetEntity() { return void.class; }
			@Override public jakarta.persistence.CascadeType[] cascade() { return new jakarta.persistence.CascadeType[0]; }
			@Override public jakarta.persistence.FetchType fetch() { return jakarta.persistence.FetchType.EAGER; }
			@Override public boolean optional() { return true; }
			@Override public String mappedBy() { return mappedBy; }
			@Override public boolean orphanRemoval() { return false; }
		};
	}
}
