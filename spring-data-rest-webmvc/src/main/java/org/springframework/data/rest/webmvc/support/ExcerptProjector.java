/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

/**
 * Interface for a component that can provide excerpt projections.
 * 
 * @author Oliver Gierke
 * @since 2.5
 */
public interface ExcerptProjector {

	/**
	 * Creates a excerpt projection for the given source. If no excerpt projection is available, the call will fall back
	 * to the behavior of {@link #project(Object)}. If you completely wish to skip handling the object, check for the
	 * presence of an excerpt projection using {@link #hasExcerptProjection(Class)}.
	 * 
	 * @param source must not be {@literal null}.
	 * @return
	 */
	Object projectExcerpt(Object source);

	/**
	 * Returns whether an excerpt projection has been registered for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	boolean hasExcerptProjection(Class<?> type);
}
