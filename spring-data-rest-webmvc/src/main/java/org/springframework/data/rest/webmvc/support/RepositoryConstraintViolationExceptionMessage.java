package org.springframework.data.rest.webmvc.support;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.context.MessageSource;
import org.springframework.data.rest.repository.RepositoryConstraintViolationException;
import org.springframework.validation.FieldError;

/**
 * @author Jon Brisbin
 */
public class RepositoryConstraintViolationExceptionMessage {

  private final RepositoryConstraintViolationException violationException;
  private final List<String> errors = new ArrayList<String>();

  public RepositoryConstraintViolationExceptionMessage(RepositoryConstraintViolationException violationException,
                                                       MessageSource msgSrc) {
    this.violationException = violationException;

    for(FieldError fe : violationException.getErrors().getFieldErrors()) {
      List<Object> args = new ArrayList<Object>();
      args.add(fe.getObjectName());
      args.add(fe.getField());
      args.add(fe.getRejectedValue());
      if(null != fe.getArguments()) {
        for(Object o : fe.getArguments()) {
          args.add(o);
        }
      }

      String msg = msgSrc.getMessage(fe.getCode(),
                                     args.toArray(),
                                     fe.getDefaultMessage(),
                                     null);
      this.errors.add(msg);
    }
  }

  @JsonProperty("errors")
  public List<String> getErrors() {
    return errors;
  }

}
