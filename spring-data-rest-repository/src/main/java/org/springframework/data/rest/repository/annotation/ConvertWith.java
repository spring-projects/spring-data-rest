package org.springframework.data.rest.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Jon Brisbin
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ConvertWith {

	Class<? extends Converter<?, ?>> value();

}
