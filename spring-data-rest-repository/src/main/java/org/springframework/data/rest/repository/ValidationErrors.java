package org.springframework.data.rest.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.AbstractErrors;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class ValidationErrors
    extends AbstractErrors {

  private String         name;
  private Object         entity;
  private EntityMetadata entityMetadata;
  private List<ObjectError> globalErrors = new ArrayList<ObjectError>();
  private List<FieldError>  fieldErrors  = new ArrayList<FieldError>();

  public ValidationErrors(String name, Object entity, EntityMetadata entityMetadata) {
    this.name = name;
    this.entity = entity;
    this.entityMetadata = entityMetadata;
  }

  @Override public String getObjectName() {
    return name;
  }

  @Override public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
    globalErrors.add(new ObjectError(name, new String[]{errorCode}, errorArgs, defaultMessage));
  }

  @Override public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
    fieldErrors.add(new FieldError(name,
                                   field,
                                   getFieldValue(field),
                                   true,
                                   new String[]{errorCode},
                                   errorArgs,
                                   defaultMessage));
  }

  @Override public void addAllErrors(Errors errors) {
    globalErrors.addAll(errors.getAllErrors());
  }

  @Override public List<ObjectError> getGlobalErrors() {
    return globalErrors;
  }

  @Override public List<FieldError> getFieldErrors() {
    return fieldErrors;
  }

  @Override public Object getFieldValue(String field) {
    return entityMetadata.attribute(field).get(entity);
  }
}
