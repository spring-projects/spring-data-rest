package org.springframework.data.rest.core.domain.jpa;

import static org.springframework.util.ClassUtils.*;
import static org.springframework.util.StringUtils.*;

import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * A test {@link Validator} that checks for non-blank names.
 * 
 * @author Jon Brisbin
 */
@Component
@HandleBeforeSave
public class PersonNameValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return isAssignable(clazz, Person.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Person p = (Person) target;
		if (!hasText(p.getLastName())) {
			errors.rejectValue("lastName", "blank", "Last name cannot be blank");
		}
	}

}
