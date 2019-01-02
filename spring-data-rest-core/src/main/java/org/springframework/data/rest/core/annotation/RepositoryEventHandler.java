/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.rest.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advertises classes annotated with this that they are event handlers.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RepositoryEventHandler {

	/**
	 * The list of {@link org.springframework.context.ApplicationEvent} classes this event handler cares about.
	 * 
	 * @deprecated the type the handler is interested in is determined by the type of the first parameter of a handler
	 *             method.
	 */
	@Deprecated
	Class<?>[] value() default {};
}
