/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.SequenceGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * Custom ID generator that will record its sequence numbers within 
 * the same transaction that uses the generator. This allows the sequence
 * numbers to also be rolled back along with the data.
 * This generator is therefore useful in unit testing as it helps guarantee that
 * unit tests are isolated from each other.  
 * 
 * @author Nick Weedon
 */
@Component
@Transactional
public class TransactionalIDGenerator extends SequenceGenerator {
	
	static ApplicationContext appCtx;
	
	static Map<Class<?>, PropertyWrapper> propertyWrapperMap = new HashMap<Class<?>, PropertyWrapper>();
	
	public TransactionalIDGenerator() {
	}
	
	@Autowired
	public TransactionalIDGenerator(ApplicationContext appCtx) {
		TransactionalIDGenerator.appCtx = appCtx;
	}
	
	private static class PropertyWrapper { 
		
		final Field field;
		final Method readMethod;
		
		public PropertyWrapper(Field field,
				Method readMethod) {
			this.field = field;
			this.readMethod = readMethod;
		}
		
		public Object getPropertyValue(Object beanInstance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			if(field != null) {
				return field.get(beanInstance);
			}
			return readMethod.invoke(beanInstance);
		}
	}
	
	private PropertyWrapper getPropertyWrapperForIdOf(Class<?> clazz) {
		
		PropertyWrapper propWrapper = propertyWrapperMap.get(clazz);
		if(propWrapper != null) {
			return propWrapper;
		}
		
		final Field[] idField = {null};
		
		BeanInfo beanInfo;

		ReflectionUtils.doWithFields(clazz, new FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException,
					IllegalAccessException {
				if(field.isAnnotationPresent(javax.persistence.Id.class)) {
					field.setAccessible(true);
					idField[0] = field;
				}
			}
		});

		Method idReadMethod = null; 
		
		try {
			beanInfo = Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		for(PropertyDescriptor propDesc : beanInfo.getPropertyDescriptors()) {
			Method readMethod = propDesc.getReadMethod(); 
			if(readMethod != null && readMethod.isAnnotationPresent(javax.persistence.Id.class) ) {
				readMethod.setAccessible(true);
				idReadMethod = readMethod;
			}
		}
		if(idReadMethod == null && idField[0] == null) {
			throw new RuntimeException("Could not find ID field for '" + clazz.toString() 
					+ "' while attempting to generate an ID");
		}
		
		propWrapper = new PropertyWrapper(idField[0], idReadMethod);
		
		propertyWrapperMap.put(clazz, propWrapper);
		
		return propWrapper;
	}
	
	@Override
	public synchronized Serializable generate(SessionImplementor session, Object obj) {
		SequenceRepository sequenceRepository = appCtx.getBean(SequenceRepository.class); 
		
		Class<?> domainClass = obj.getClass();
		
		Sequence sequence = sequenceRepository.findOne(domainClass.getName());
		
		if(sequence == null) {
			sequence = new Sequence(domainClass.getName());
		}
		
		Object existingId;
		try {
			existingId = getPropertyWrapperForIdOf(domainClass).getPropertyValue(obj);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		if( existingId != null && ((Number)existingId).intValue() != 0)
			return (Number)existingId;
		
		long id = sequence.getNextValue();
		
		sequenceRepository.save(sequence);
		
		return id;
	}

}
