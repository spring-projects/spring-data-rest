package org.springframework.data.rest.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@link org.springframework.data.repository.Repository} with this to influence how it is exported and what
 * the value of the {@literal rel} attribute will be in links.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Target({
            ElementType.METHOD,
            ElementType.TYPE
        })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RestResource {

  String path();

  String rel() default "";

}
