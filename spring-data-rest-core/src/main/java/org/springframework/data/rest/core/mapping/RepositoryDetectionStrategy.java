/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import java.lang.reflect.Modifier;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * The strategy to determine whether a given repository is to be exported by Spring Data REST.
 *
 * @author Oliver Gierke
 * @author Tobias Weiß
 * @since 2.5
 * @soundtrack Katinka - Ausverkauf
 */
public interface RepositoryDetectionStrategy {

	/**
	 * Returns whether the repository described by the given {@link RepositoryMetadata} is exported or not.
	 *
	 * @param metadata must not be {@literal null}.
	 * @return
	 */
	boolean isExported(RepositoryMetadata metadata);

	/**
	 * Returns whether to expose repository methods by default, i.e. without the need to explicitly annotate them with
	 * {@link RestResource}.
	 * 
	 * @return
	 * @since 3.1
	 */
	default boolean exposeMethodsByDefault() {
		return true;
	}

	/**
	 * A variety of strategies to determine repository exposure.
	 *
	 * @author Oliver Gierke
	 * @since 2.5
	 * @soundtrack Katinka - Ausverkauf
	 */
	public static enum RepositoryDetectionStrategies implements RepositoryDetectionStrategy {

		/**
		 * Considers all repositories.
		 */
		ALL {

			@Override
			public boolean isExported(RepositoryMetadata metadata) {
				return true;
			}
		},

		/**
		 * Exposes public interfaces or ones explicitly annotated with {@link RepositoryRestResource}.
		 *
		 * @see #VISIBILITY
		 * @see #ANNOTATED
		 */
		DEFAULT {

			@Override
			public boolean isExported(RepositoryMetadata metadata) {

				return isExplicitlyExported(metadata.getRepositoryInterface(),
						isExplicitlyExported(metadata.getDomainType(), VISIBILITY.isExported(metadata)));
			}
		},

		/**
		 * Considers the repository interface's visibility, which means only public interfaces will be exposed.
		 */
		VISIBILITY {

			@Override
			public boolean isExported(RepositoryMetadata metadata) {
				return Modifier.isPublic(metadata.getRepositoryInterface().getModifiers());
			};
		},

		/**
		 * Considers repositories that are annotated with {@link RepositoryRestResource} or {@link RestResource} and don't
		 * have the {@code exported} flag not set to {@literal false}.
		 */
		ANNOTATED {

			@Override
			public boolean isExported(RepositoryMetadata metadata) {
				return isExplicitlyExported(metadata.getRepositoryInterface(), false);
			}
		},

		/**
		 * Behaves like the {@link RepositoryDetectionStrategies#ANNOTATED} strategy on repository level, but only exports
		 * the methods of the repository that have been explicitly annotated with {@link RestResource}. CRUD methods need to
		 * be annotated, too, for the default collection resource exposure to be applied.
		 * 
		 * @since 3.1
		 */
		EXPLICITLY_ANNOTATED {

			@Override
			public boolean isExported(RepositoryMetadata metadata) {
				return isExplicitlyExported(metadata.getRepositoryInterface(), false);
			}

			/* 
			 * (non-Javadoc)
			 * @see org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy#exposeMethodsByDefault()
			 */
			@Override
			public boolean exposeMethodsByDefault() {
				return false;
			}
		};

		/**
		 * Returns whether the given type was explicitly exported using {@link RepositoryRestResource} or
		 * {@link RestResource}. In case no decision can be made based on the annotations, the fallback will be used.
		 *
		 * @param type must not be {@literal null}.
		 * @param fallback
		 * @return
		 */
		private static boolean isExplicitlyExported(Class<?> type, boolean fallback) {

			RepositoryRestResource restResource = AnnotationUtils.findAnnotation(type, RepositoryRestResource.class);

			if (restResource != null) {
				return restResource.exported();
			}

			RestResource resource = AnnotationUtils.findAnnotation(type, RestResource.class);

			if (resource != null) {
				return resource.exported();
			}

			return fallback;
		}
	}
}
