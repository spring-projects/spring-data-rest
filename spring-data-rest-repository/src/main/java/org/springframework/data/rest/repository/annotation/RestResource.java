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
 * @author Jon Brisbin
 */
@Target({
            ElementType.FIELD,
            ElementType.METHOD,
            ElementType.TYPE
        })
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

}
