/*
 * Copyright 2012-2024 the original author or authors.
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
package org.springframework.data.rest.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@link org.springframework.data.repository.Repository} with this to influence how it is exported and what
 * the value of the {@literal rel} attribute will be in links.
 * <p>
 * As of Spring Data REST 2.0, prefer using {@link RepositoryRestResource} to also be able to customize the relation
 * type and description for the item resources exposed by the repository.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RestResource {

	/**
	 * Flag indicating whether this resource is exported at all.
	 *
	 * @return {@literal true} if the resource is to be exported, {@literal false} otherwise.
	 */
	boolean exported() default true;

	/**
	 * The path segment under which this resource is to be exported.
	 *
	 * @return A valid path segment.
	 */
	String path() default "";

	/**
	 * The rel value to use when generating links to this resource.
	 *
	 * @return A valid rel value.
	 */
	String rel() default "";

	/**
	 * The description of the collection resource.
	 *
	 * @return
	 */
	Description description() default @Description(value = "");
}
