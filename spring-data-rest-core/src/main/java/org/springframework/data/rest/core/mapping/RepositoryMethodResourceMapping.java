/*
 * Copyright 2013-2025 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.core.AnnotationAttribute;
import org.springframework.hateoas.server.core.MethodParameters;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link RepositoryMethodResourceMapping} created from a {@link Method}.
 *
 * @author Oliver Gierke
 */
class RepositoryMethodResourceMapping implements MethodResourceMapping {

	private static final Collection<Class<?>> IMPLICIT_PARAMETER_TYPES = Arrays.asList(Pageable.class, Sort.class);
	private static final AnnotationAttribute PARAM_VALUE = new AnnotationAttribute(Param.class);

	private final boolean isExported;
	private final LinkRelation rel;
	private final Path path;
	private final Method method;
	private final boolean paging;
	private final boolean sorting;
	private final RepositoryMetadata metadata;

	private final List<ParameterMetadata> parameterMetadata;

	/**
	 * Creates a new {@link RepositoryMethodResourceMapping} for the given {@link Method}.
	 *
	 * @param method must not be {@literal null}.
	 * @param resourceMapping must not be {@literal null}.
	 * @param metadata can be {@literal null}.
	 * @param whether the methods are supposed to be exported by default.
	 */
	public RepositoryMethodResourceMapping(Method method, ResourceMapping resourceMapping, RepositoryMetadata metadata,
			boolean exposeMethodsByDefault) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(resourceMapping, "ResourceMapping must not be null");

		RestResource annotation = AnnotationUtils.findAnnotation(method, RestResource.class);
		LinkRelation resourceRel = resourceMapping.getRel();

		this.isExported = annotation != null ? annotation.exported() : exposeMethodsByDefault;
		this.rel = LinkRelation
				.of(annotation == null || !StringUtils.hasText(annotation.rel()) ? method.getName() : annotation.rel());
		this.path = annotation == null || !StringUtils.hasText(annotation.path()) ? new Path(method.getName())
				: new Path(annotation.path());
		this.method = method;
		this.parameterMetadata = discoverParameterMetadata(method, resourceRel.value().concat(".").concat(rel.value()));

		List<Class<?>> parameterTypes = Arrays.asList(method.getParameterTypes());

		this.paging = parameterTypes.contains(Pageable.class);
		this.sorting = parameterTypes.contains(Sort.class);
		this.metadata = metadata;
	}

	private static final List<ParameterMetadata> discoverParameterMetadata(Method method, String baseRel) {

		List<ParameterMetadata> result = new ArrayList<ParameterMetadata>();

		for (MethodParameter parameter : new MethodParameters(method, PARAM_VALUE).getParameters()) {
			if (!IMPLICIT_PARAMETER_TYPES.contains(parameter.getParameterType())
					&& StringUtils.hasText(parameter.getParameterName())) {
				result.add(new ParameterMetadata(parameter, baseRel));
			}
		}

		return Collections.unmodifiableList(result);
	}

	@Override
	public boolean isExported() {
		return isExported;
	}

	@Override
	public LinkRelation getRel() {
		return rel;
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public Method getMethod() {
		return method;
	}

	@Override
	public ParametersMetadata getParametersMetadata() {
		return new ParametersMetadata(parameterMetadata);
	}

	@Override
	public boolean isPagingResource() {
		return paging;
	}

	@Override
	public boolean isSortableResource() {
		return sorting;
	}

	@Override
	public ResourceDescription getDescription() {
		return null;
	}

	@Override
	public Class<?> getReturnedDomainType() {
		return metadata.getReturnedDomainClass(method);
	}
}
