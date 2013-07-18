package org.springframework.data.rest.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advertises classes annotated with this that they are event handlers.
 * 
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RepositoryEventHandler {

	/**
	 * The list of {@link org.springframework.context.ApplicationEvent} classes this event handler cares about.
	 */
	Class<?>[] value() default {};

}
