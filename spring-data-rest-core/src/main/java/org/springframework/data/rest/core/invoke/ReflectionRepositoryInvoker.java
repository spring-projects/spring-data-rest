/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.invoke;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.hateoas.core.AnnotationAttribute;
import org.springframework.hateoas.core.MethodParameters;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Base {@link RepositoryInvoker} using reflection to invoke methods on Spring Data Repositories.
 * 
 * @author Oliver Gierke
 */
class ReflectionRepositoryInvoker implements RepositoryInvoker {

	private static final AnnotationAttribute PARAM_ANNOTATION = new AnnotationAttribute(Param.class);

	private final Object repository;
	private final CrudMethods methods;
	private final RepositoryInformation information;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link ReflectionRepositoryInvoker} for the given repository, {@link RepositoryInformation} and
	 * {@link ConversionService}.
	 * 
	 * @param repository must not be {@literal null}.
	 * @param information must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public ReflectionRepositoryInvoker(Object repository, RepositoryInformation information,
			ConversionService conversionService) {

		Assert.notNull(repository, "Repository must not be null!");
		Assert.notNull(information, "RepositoryInformation must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.repository = repository;
		this.methods = information.getCrudMethods();
		this.information = information;
		this.conversionService = conversionService;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvocationInformation#exposesFindAll()
	 */
	@Override
	public boolean exposesFindAll() {
		return methods.hasFindAllMethod() && exposes(methods.getFindAllMethod());
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeFindAll(org.springframework.data.domain.Sort)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterable<Object> invokeFindAll(Sort sort) {
		return (Iterable<Object>) invoke(methods.getFindAllMethod(), sort);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeFindAll(org.springframework.data.domain.Pageable)
	 */
	@Override
	public Iterable<Object> invokeFindAll(Pageable pageable) {

		if (!exposesFindAll()) {
			return Collections.emptyList();
		}

		Method method = methods.getFindAllMethod();
		Class<?>[] types = method.getParameterTypes();

		if (types.length == 0) {
			return invoke(method);
		}

		if (Sort.class.isAssignableFrom(types[0])) {
			return invoke(method, pageable == null ? null : pageable.getSort());
		}

		return invoke(method, pageable);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvocationInformation#exposesSave()
	 */
	@Override
	public boolean exposesSave() {
		return methods.hasSaveMethod() && exposes(methods.getSaveMethod());
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeSave(java.lang.Object)
	 */
	@Override
	public <T> T invokeSave(T object) {
		return invoke(methods.getSaveMethod(), object);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvocationInformation#exposesFindOne()
	 */
	@Override
	public boolean exposesFindOne() {
		return methods.hasFindOneMethod() && exposes(methods.getFindOneMethod());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeFindOne(java.io.Serializable)
	 */
	@Override
	public <T> T invokeFindOne(Serializable id) {
		return invoke(methods.getFindOneMethod(), convertId(id));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvocationInformation#exposesDelete()
	 */
	@Override
	public boolean exposesDelete() {
		return methods.hasDelete() && exposes(methods.getDeleteMethod());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeDelete(java.io.Serializable)
	 */
	@Override
	public void invokeDelete(Serializable id) {

		Method method = methods.getDeleteMethod();

		if (method.getParameterTypes()[0].equals(Serializable.class)) {
			invoke(method, convertId(id));
		} else {
			invoke(method, invokeFindOne(id));
		}
	}

	private boolean exposes(Method method) {

		RestResource annotation = AnnotationUtils.findAnnotation(method, RestResource.class);
		return annotation == null ? true : annotation.exported();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeQueryMethod(java.lang.reflect.Method, java.util.Map, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort)
	 */
	@Override
	public Object invokeQueryMethod(Method method, Map<String, String[]> parameters, Pageable pageable, Sort sort) {
		return invoke(method, prepareParameters(method, parameters, pageable, sort));
	}

	private Object[] prepareParameters(Method method, Map<String, String[]> rawParameters, Pageable pageable, Sort sort) {

		List<MethodParameter> parameters = new MethodParameters(method, PARAM_ANNOTATION).getParameters();

		if (parameters.isEmpty()) {
			return new Object[0];
		}

		Object[] result = new Object[parameters.size()];
		Sort sortToUse = pageable == null ? sort : pageable.getSort();

		for (int i = 0; i < result.length; i++) {

			MethodParameter param = parameters.get(i);
			Class<?> targetType = param.getParameterType();

			if (Pageable.class.isAssignableFrom(targetType)) {
				result[i] = pageable;
			} else if (Sort.class.isAssignableFrom(targetType)) {
				result[i] = sortToUse;
			} else {

				String parameterName = param.getParameterName();

				if (!StringUtils.hasText(parameterName)) {
					throw new IllegalArgumentException("No @Param annotation found on query method " + method.getName()
							+ " for parameter " + parameterName);
				}

				String[] parameterValue = rawParameters.get(parameterName);
				Object value = parameterValue == null ? null : parameterValue.length == 1 ? parameterValue[0] : parameterValue;

				result[i] = conversionService.convert(value, TypeDescriptor.forObject(value), new TypeDescriptor(param));
			}
		}

		return result;
	}

	/**
	 * Invokes the given method with the given arguments on the backing repository.
	 * 
	 * @param method
	 * @param arguments
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T invoke(Method method, Object... arguments) {
		return (T) ReflectionUtils.invokeMethod(method, repository, arguments);
	}

	/**
	 * Converts the given id into the id type of the backing repository.
	 * 
	 * @param id must not be {@literal null}.
	 * @return
	 */
	protected Serializable convertId(Serializable id) {
		Assert.notNull(id, "Id must not be null!");
		return conversionService.convert(id, information.getIdType());
	}
}
