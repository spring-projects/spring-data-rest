package org.springframework.data.rest.test.webmvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class PersonValidator
    implements Validator {

  private static final Logger LOG = LoggerFactory.getLogger(PersonValidator.class);

  @Override public boolean supports(Class<?> clazz) {
    return ClassUtils.isAssignable(clazz, Person.class);
  }

  @Override public void validate(Object target, Errors errors) {
    Person p = (Person)target;
    LOG.debug(" ***** Validating Person " + p);
    ValidationUtils.rejectIfEmpty(errors, "name", "field.name.required", "Field 'name' cannot be blank.");
  }

}
