/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

/**
 * Variant of {@link jakarta.persistence.GeneratedValue} using Hibernate-specific generators.
 *
 * @see <a href=
 *      "https://discourse.hibernate.org/t/manually-setting-the-identifier-results-in-object-optimistic-locking-failure-exception/10743/6">Manually
 *      setting the identifier results in object optimistic locking failure exception</a>
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-17472">HHH-17472</a>
 */
@IdGeneratorType(AssignableSequenceStyleGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface SequenceGenerator {
}
