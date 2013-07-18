/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.core.support;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AnnotationRepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author Oliver Gierke
 */
public class RepositoriesUtils {

	/**
	 * Resolves the domain type from the given type. Will resolve the repository domain type if the given type is a
	 * repository or return the type as is if not.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static Class<?> getDomainType(Class<?> type) {

		if (!isRepositoryInterface(type)) {
			return type;
		}

		return getMetadataFor(type).getDomainType();
	}

	public static boolean isRepositoryInterface(Class<?> type) {
		return Repository.class.isAssignableFrom(type)
				|| AnnotationUtils.findAnnotation(type, RepositoryDefinition.class) != null;
	}

	private static RepositoryMetadata getMetadataFor(Class<?> type) {
		return Repository.class.isAssignableFrom(type) ? new DefaultRepositoryMetadata(type)
				: new AnnotationRepositoryMetadata(type);
	}
}
