package org.springframework.data.rest.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Jon Brisbin
 */
@Target({
            ElementType.TYPE,
            ElementType.FIELD,
            ElementType.METHOD
        })
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {
  String value();
}
