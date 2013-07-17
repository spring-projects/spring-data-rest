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
package org.springframework.data.rest.repository.invoke;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.hateoas.core.MethodParameters;
import org.springframework.util.Assert;

/**
 * Base {@link RepositoryInvoker} using reflection to invoke methods on Spring Data Repositories.
 * 
 * @author Oliver Gierke
 */
class ReflectionRepositoryInvoker implements RepositoryInvoker {

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
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvocationInformation#exposesFindAll()
	 */
	@Override
	public boolean exposesFindAll() {
		return methods.hasFindAllMethod() && exposes(methods.getFindAllMethod());
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvoker#invokeFindAll(org.springframework.data.domain.Sort)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterable<Object> invokeFindAll(Sort sort) {
		return (Iterable<Object>) invoke(methods.getFindAllMethod(), sort);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvoker#invokeFindAll(org.springframework.data.domain.Pageable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterable<Object> invokeFindAll(Pageable pageable) {
		return (Iterable<Object>) invoke(methods.getFindAllMethod(), pageable);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvocationInformation#exposesSave()
	 */
	@Override
	public boolean exposesSave() {
		return methods.hasSaveMethod() && exposes(methods.getSaveMethod());
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvoker#invokeSave(java.lang.Object)
	 */
	@Override
	public Object invokeSave(Object object) {
		return invoke(methods.getSaveMethod(), object);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvocationInformation#exposesFindOne()
	 */
	@Override
	public boolean exposesFindOne() {
		return methods.hasFindOneMethod() && exposes(methods.getFindOneMethod());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvoker#invokeFindOne(java.io.Serializable)
	 */
	@Override
	public Object invokeFindOne(Serializable id) {
		return invoke(methods.getFindOneMethod(), convertId(id));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvocationInformation#exposesDelete()
	 */
	@Override
	public boolean exposesDelete() {
		return methods.hasDelete() && exposes(methods.getDeleteMethod());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvoker#invokeDelete(java.io.Serializable)
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
	 * @see org.springframework.data.rest.repository.invoke.RepositoryInvoker#invokeQueryMethod(java.lang.reflect.Method, java.util.Map, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort)
	 */
	@Override
	public Object invokeQueryMethod(Method method, Map<String, String[]> parameters, Pageable pageable, Sort sort) {
		return invoke(method, prepareParameters(method, parameters, pageable, sort));
	}

	private Object[] prepareParameters(Method method, Map<String, String[]> rawParameters, Pageable pageable, Sort sort) {

		List<MethodParameter> parameters = new MethodParameters(method).getParameters();

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
				String[] parameterValue = rawParameters.get(parameterName);
				Object value = parameterValue.length == 1 ? parameterValue[0] : parameterValue;

				if (value == null) {
					if (parameterName.startsWith("arg")) {
						throw new IllegalArgumentException("No @Param annotation found on query method " + method.getName()
								+ " for parameter " + parameterName);
					} else {
						throw new IllegalArgumentException("No query parameter specified for " + method.getName() + " param '"
								+ parameterName + "'");
					}
				}

				result[i] = conversionService.convert(parameterValue, targetType);
			}
		}

		return result;
	}

	private Object invoke(Method method, Object... arguments) {

		BeanWrapperImpl wrapper = new BeanWrapperImpl();
		wrapper.setConversionService(conversionService);

		ArgumentConvertingMethodInvoker invoker = new ArgumentConvertingMethodInvoker();
		invoker.setTargetObject(repository);
		invoker.setTargetMethod(method.getName());
		invoker.setArguments(arguments);
		invoker.setTypeConverter(wrapper);

		try {
			invoker.prepare();
			return invoker.invoke();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
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
