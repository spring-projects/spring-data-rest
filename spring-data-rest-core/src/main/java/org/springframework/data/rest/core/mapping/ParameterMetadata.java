/*
 * Copyright 2014-2025 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.data.rest.core.annotation.Description;
import org.springframework.hateoas.LinkRelation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object to capture metadata for query method parameters.
 *
 * @author Oliver Gierke
 */
public final class ParameterMetadata {

	private final String name;
	private final ResourceDescription description;

	/**
	 * Creates a new {@link ParameterMetadata} for the given {@link MethodParameter} and base rel.
	 *
	 * @param parameter must not be {@literal null} or empty.
	 * @param baseRel must not be {@literal null} or empty.
	 */
	public ParameterMetadata(MethodParameter parameter, String baseRel) {

		Assert.notNull(parameter, "MethodParameter must not be null");

		this.name = parameter.getParameterName();

		Assert.hasText(name, "Parameter name must not be null or empty");
		Assert.hasText(baseRel, "Method rel must not be null");

		ResourceDescription fallback = TypedResourceDescription
				.defaultFor(LinkRelation.of(baseRel.concat(".").concat(name)), parameter.getParameterType());
		Description annotation = parameter.getParameterAnnotation(Description.class);

		this.description = annotation == null ? fallback : new AnnotationBasedResourceDescription(annotation, fallback);
	}

	/**
	 * Return sthe name of the method parameter.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the description for the method parameter.
	 *
	 * @return the description
	 */
	public ResourceDescription getDescription() {
		return description;
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof ParameterMetadata)) {
			return false;
		}

		ParameterMetadata that = (ParameterMetadata) obj;
		return this.name.equals(that.name) && this.description.equals(that.description);
	}

	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * name.hashCode();
		result += 31 * description.hashCode();

		return result;
	}
}
