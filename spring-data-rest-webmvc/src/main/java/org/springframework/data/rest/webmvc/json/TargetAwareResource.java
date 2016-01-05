/*
 * Copyright 2012-2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import org.springframework.data.projection.TargetAware;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

/**
 * Resource that delegates implementation of {@link TargetAware} interface to the projection it contains.
 *
 * @author Anton Koscejev
 */
class TargetAwareResource<T> extends Resource<T> implements TargetAware {

	private final Object target;
	private final Class<?> targetClass;

	TargetAwareResource(T content, Object target, Class<?> targetClass, Iterable<Link> links) {

		super(content, links);
		this.target = target;
		this.targetClass = targetClass;
	}

	static TargetAwareResource<ProjectionResourceContent> forProjection(TargetAware projection, Iterable<Link> links) {

		Class<?> projectionType = projection.getClass().getInterfaces()[0];
		ProjectionResourceContent content = new ProjectionResourceContent(projection, projectionType);
		return new TargetAwareResource<ProjectionResourceContent>(
				content, projection.getTarget(), projection.getTargetClass(), links);
	}

	public Object getTarget() {
		return target;
	}

	@Override
	public Class<?> getDecoratedClass() {
		return targetClass;
	}

	@Override
	public Class<?> getTargetClass() {
		return targetClass;
	}
}
