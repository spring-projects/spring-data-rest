package org.springframework.data.rest.repository;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class RepositoryNotFoundException extends DataAccessResourceFailureException {

  public RepositoryNotFoundException(String msg) {
    super(msg);
  }

  public RepositoryNotFoundException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
