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

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link ResourceDescription} that is customized based on a {@link Description} annotation. Allows to fall back on
 * another {@link ResourceDescription} to provide defaults.
 *
 * @author Oliver Gierke
 */
public class AnnotationBasedResourceDescription extends ResolvableResourceDescriptionSupport {

	private final String message;
	private final ResourceDescription fallback;

	/**
	 * Creates a new {@link AnnotationBasedResourceDescription} for the given {@link Description} and fallback.
	 *
	 * @param description must not be {@literal null}.
	 * @param fallback must not be {@literal null}.
	 */
	public AnnotationBasedResourceDescription(Description description, ResourceDescription fallback) {

		Assert.notNull(description, "Description must not be null");
		Assert.notNull(fallback, "Fallback resource description must not be null");

		this.message = description.value();
		this.fallback = fallback;
	}

	public AnnotationBasedResourceDescription(Class<?> type, ResourceDescription fallback) {

		Description description = AnnotationUtils.findAnnotation(type, Description.class);

		this.message = description == null ? null : description.value();
		this.fallback = fallback;
	}

	@Override
	public String[] getCodes() {
		return fallback.getCodes();
	}

	@Override
	public String getMessage() {
		return StringUtils.hasText(message) ? message : fallback.getMessage();
	}

	/**
	 * @return the mediaType
	 */
	public MediaType getType() {
		return null;
	}

	@Override
	public boolean isDefault() {
		return !StringUtils.hasText(message);
	}
}
