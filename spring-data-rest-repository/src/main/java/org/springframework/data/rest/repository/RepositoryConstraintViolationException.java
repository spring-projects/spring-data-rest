package org.springframework.data.rest.repository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.Errors;

/**
 * Exception that is thrown when a Spring {@link org.springframework.validation.Validator} throws an error.
 *
 * @author Jon Brisbin
 */
public class RepositoryConstraintViolationException extends DataIntegrityViolationException {

  private Errors errors;

  public RepositoryConstraintViolationException(Errors errors) {
    super("Validation failed");
    this.errors = errors;
  }

  public Errors getErrors() {
    return errors;
  }

}
