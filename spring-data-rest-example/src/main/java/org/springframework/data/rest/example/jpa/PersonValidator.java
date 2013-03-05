package org.springframework.data.rest.example.jpa;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * @author Jon Brisbin
 */
public class PersonValidator implements Validator {

	@Override public boolean supports(Class<?> clazz) {
		return Person.class.isAssignableFrom(clazz);
	}

	@Override public void validate(Object target, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "firstName", "not.blank");
		ValidationUtils.rejectIfEmpty(errors, "lastName", "not.blank");
	}

}
