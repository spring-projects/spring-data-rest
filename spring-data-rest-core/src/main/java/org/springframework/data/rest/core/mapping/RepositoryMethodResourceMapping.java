/*
 * Copyright 2013-2014 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.hateoas.core.AnnotationAttribute;
import org.springframework.hateoas.core.MethodParameters;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link RepositoryMethodResourceMapping} created from a {@link Method}.
 * 
 * @author Oliver Gierke
 */
class RepositoryMethodResourceMapping implements MethodResourceMapping {

	private static final AnnotationAttribute PARAM_VALUE = new AnnotationAttribute(Param.class);

	private final boolean isExported;
	private final String rel;
	private final Path path;
	private final Method method;
	private final boolean paging;
	private final boolean sorting;

	private final List<ParameterMetadata> parameterMetadata;

	/**
	 * Creates a new {@link RepositoryMethodResourceMapping} for the given {@link Method}.
	 * 
	 * @param method must not be {@literal null}.
	 * @param resourceMapping must not be {@literal null}.
	 */
	public RepositoryMethodResourceMapping(Method method, ResourceMapping resourceMapping) {

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(resourceMapping, "ResourceMapping must not be null!");

		RestResource annotation = AnnotationUtils.findAnnotation(method, RestResource.class);
		String resourceRel = resourceMapping.getRel();

		this.isExported = annotation != null ? annotation.exported() : true;
		this.rel = annotation == null || !StringUtils.hasText(annotation.rel()) ? method.getName() : annotation.rel();
		this.path = annotation == null || !StringUtils.hasText(annotation.path()) ? new Path(method.getName()) : new Path(
				annotation.path());
		this.method = method;
		this.parameterMetadata = discoverParameterMetadata(method, resourceRel.concat(".").concat(rel));

		List<Class<?>> parameterTypes = Arrays.asList(method.getParameterTypes());

		this.paging = parameterTypes.contains(Pageable.class);
		this.sorting = parameterTypes.contains(Sort.class);
	}

	private static final List<ParameterMetadata> discoverParameterMetadata(Method method, String baseRel) {

		List<ParameterMetadata> result = new ArrayList<ParameterMetadata>();

		for (MethodParameter parameter : new MethodParameters(method, PARAM_VALUE).getParameters()) {
			if (StringUtils.hasText(parameter.getParameterName())) {
				result.add(new ParameterMetadata(parameter, baseRel));
			}
		}

		return Collections.unmodifiableList(result);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#isExported()
	 */
	@Override
	public boolean isExported() {
		return isExported;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getRel()
	 */
	@Override
	public String getRel() {
		return rel;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.MethodResourceMapping#getMethod()
	 */
	@Override
	public Method getMethod() {
		return method;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.MethodResourceMapping#getParameterMetadata()
	 */
	@Override
	public ParametersMetadata getParametersMetadata() {
		return new ParametersMetadata(parameterMetadata);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#isPagingResource()
	 */
	@Override
	public boolean isPagingResource() {
		return paging;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.MethodResourceMapping#isSortableResource()
	 */
	@Override
	public boolean isSortableResource() {
		return sorting;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getDescription()
	 */
	@Override
	public ResourceDescription getDescription() {
		return null;
	}
}
