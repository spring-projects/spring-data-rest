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
package org.springframework.data.rest.repository.mapping;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.util.StringUtils;

/**
 * A {@link RepositoryMethodResourceMapping} created from a {@link Method}.
 * 
 * @author Oliver Gierke
 */
class RepositoryMethodResourceMapping implements ResourceMapping {

	private final boolean isExported;
	private final String rel;
	private final Path path;

	/**
	 * Creates a new {@link RepositoryMethodResourceMapping} for the given {@link Method}.
	 * 
	 * @param method must not be {@literal null}.
	 */
	public RepositoryMethodResourceMapping(Method method, ResourceMapping resourceMapping) {

		RestResource annotation = AnnotationUtils.findAnnotation(method, RestResource.class);

		this.isExported = annotation != null ? annotation.exported() : true;
		this.rel = annotation != null ? annotation.rel() : method.getName();

		Path resourcePath = resourceMapping.getPath();
		String toAppend = annotation == null || !StringUtils.hasText(annotation.path()) ? method.getName() : annotation
				.path();
		this.path = resourcePath.slash(toAppend);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#isExported()
	 */
	@Override
	public Boolean isExported() {
		return isExported;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#getRel()
	 */
	@Override
	public String getRel() {
		return rel;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}
}
