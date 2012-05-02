package org.springframework.data.rest.repository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.Errors;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
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
