/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Annotation to demarcate Spring MVC controllers provided by Spring Data REST. Allows to easily detect them and exclude
 * them from standard Spring MVC handling.
 * <p>
 * Note, that this annotation should only be used by application controllers that map to URIs that are managed by Spring
 * Data REST as they get handled by a special {@link HandlerMapping} implementation that applies additional
 * functionality:
 * <ul>
 * <li>CORS configuration defined for the repository backing the path.</li>
 * <li>An {@link OpenEntityManagerInViewInterceptor} for JPA backed repositories so that properties can always be
 * accessed.</li>
 * </ul>
 *
 * @author Oliver Gierke
 */
@Documented
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@BasePathAwareController
public @interface RepositoryRestController {
    @AliasFor("path")
    String[] value() default {};

    @AliasFor("value")
    String[] path() default {};
}
