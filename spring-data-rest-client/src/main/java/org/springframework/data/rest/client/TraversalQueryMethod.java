/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.client;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * @author Oliver Gierke
 */
public class TraversalQueryMethod extends QueryMethod {

	private final Method method;
	private final Traversal traversal;

	/**
	 * @param method
	 * @param metadata
	 */
	public TraversalQueryMethod(Method method, RepositoryMetadata metadata) {

		super(method, metadata);

		this.traversal = AnnotationUtils.findAnnotation(method, Traversal.class);
		this.method = method;
	}

	TypeInformation<?> getReturnTypeInformation() {
		return ClassTypeInformation.fromReturnTypeOf(method);
	}

	Traversal getTraversal() {
		return traversal;
	}
}
