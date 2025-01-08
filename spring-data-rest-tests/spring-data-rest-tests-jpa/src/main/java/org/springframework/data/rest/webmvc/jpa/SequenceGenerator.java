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
