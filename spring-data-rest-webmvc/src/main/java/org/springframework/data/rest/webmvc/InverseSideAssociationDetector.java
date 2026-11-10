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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Utility to detect whether a {@link PersistentProperty} represents the inverse (non-owning) side of a JPA
 * bidirectional association — i.e., one annotated with {@code mappedBy} on {@code @OneToMany}, {@code @ManyToMany},
 * or {@code @OneToOne}.
 *
 * <p>JPA only persists association changes from the <em>owning</em> side of a relationship. Attempting to modify an
 * association from the inverse side (the side carrying {@code mappedBy}) will silently produce no database changes,
 * even though the in-memory object graph is mutated and the entity is saved. This detector allows Spring Data REST to
 * identify such properties and reject write operations with a meaningful HTTP 405 response rather than a misleading
 * HTTP 204 that implies success.
 *
 * <p>Detection is performed via reflection so that this class compiles and runs correctly even when
 * {@code jakarta.persistence} is not on the classpath (it is an optional dependency of the webmvc module).
 *
 * @author Steve Rutherford
 * @see <a href="https://github.com/spring-projects/spring-data-rest/issues/1810">GitHub Issue #1810</a>
 */
class InverseSideAssociationDetector {

	/**
	 * Names of JPA association annotations that support a {@code mappedBy} attribute, indicating the inverse side of a
	 * bidirectional relationship.
	 */
	private static final List<String> MAPPED_BY_ANNOTATION_NAMES = Arrays.asList(
			"jakarta.persistence.OneToMany",
			"jakarta.persistence.ManyToMany",
			"jakarta.persistence.OneToOne");

	/**
	 * Returns {@code true} if the given {@link PersistentProperty} is the inverse (non-owning) side of a JPA
	 * bidirectional association, identified by a non-empty {@code mappedBy} attribute on one of the JPA relationship
	 * annotations ({@code @OneToMany}, {@code @ManyToMany}, or {@code @OneToOne}).
	 *
	 * <p>Returns {@code false} if JPA is not present on the classpath, if the property carries none of the relevant
	 * annotations, or if the {@code mappedBy} attribute is empty.
	 *
	 * @param property the persistent property to inspect; must not be {@literal null}.
	 * @return {@code true} if the property is the inverse side of a JPA association.
	 */
	static boolean isInverseSide(PersistentProperty<?> property) {

		for (String annotationName : MAPPED_BY_ANNOTATION_NAMES) {

			Class<? extends Annotation> annotationType = loadAnnotationType(annotationName);

			if (annotationType == null) {
				continue;
			}

			Annotation annotation = property.findAnnotation(annotationType);

			if (annotation == null) {
				continue;
			}

			String mappedBy = getMappedByValue(annotation);

			if (StringUtils.hasText(mappedBy)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Attempts to load the annotation class with the given name. Returns {@code null} if the class is not available on
	 * the classpath, allowing graceful degradation when JPA is absent.
	 */
	@SuppressWarnings("unchecked")
	private static Class<? extends Annotation> loadAnnotationType(String className) {

		try {
			Class<?> type = ClassUtils.forName(className, InverseSideAssociationDetector.class.getClassLoader());
			return type.isAnnotation() ? (Class<? extends Annotation>) type : null;
		} catch (ClassNotFoundException | LinkageError ex) {
			return null;
		}
	}

	/**
	 * Reflectively reads the {@code mappedBy} attribute from the given annotation instance. Returns {@code null} if the
	 * attribute does not exist or cannot be read.
	 */
	private static String getMappedByValue(Annotation annotation) {

		try {
			Method mappedByMethod = annotation.annotationType().getDeclaredMethod("mappedBy");
			Object value = mappedByMethod.invoke(annotation);
			return value instanceof String s ? s : null;
		} catch (Exception ex) {
			return null;
		}
	}
}
