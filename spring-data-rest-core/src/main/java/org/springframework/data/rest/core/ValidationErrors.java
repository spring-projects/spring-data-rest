package org.springframework.data.rest.core;

import static org.springframework.util.ReflectionUtils.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.validation.AbstractErrors;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * An {@link Errors} implementation for use in the events mechanism of Spring Data REST.
 * 
 * @author Jon Brisbin
 */
public class ValidationErrors extends AbstractErrors {

	private static final long serialVersionUID = 8141826537389141361L;

	private String name;
	private Object entity;
	private PersistentEntity<?, ?> persistentEntity;
	private List<ObjectError> globalErrors = new ArrayList<ObjectError>();
	private List<FieldError> fieldErrors = new ArrayList<FieldError>();

	public ValidationErrors(String name, Object entity, PersistentEntity<?, ?> persistentEntity) {
		this.name = name;
		this.entity = entity;
		this.persistentEntity = persistentEntity;
	}

	@Override
	public String getObjectName() {
		return name;
	}

	@Override
	public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
		globalErrors.add(new ObjectError(name, new String[] { errorCode }, errorArgs, defaultMessage));
	}

	@Override
	public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
		fieldErrors.add(new FieldError(name, field, getFieldValue(field), true, new String[] { errorCode }, errorArgs,
				defaultMessage));
	}

	@Override
	public void addAllErrors(Errors errors) {
		globalErrors.addAll(errors.getAllErrors());
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		return globalErrors;
	}

	@Override
	public List<FieldError> getFieldErrors() {
		return fieldErrors;
	}

	@Override
	public Object getFieldValue(String field) {
		PersistentProperty<?> prop = persistentEntity != null ? persistentEntity.getPersistentProperty(field) : null;
		if (null == prop) {
			return null;
		}

		Method getter = prop.getGetter();
		if (null != getter) {
			return invokeMethod(getter, entity);
		}
		Field fld = prop.getField();
		if (null != fld) {
			return getField(fld, entity);
		}

		return null;
	}

}
