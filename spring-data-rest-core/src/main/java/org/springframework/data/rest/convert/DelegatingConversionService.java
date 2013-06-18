/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.rest.convert;

import java.util.Stack;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

/**
 * This {@link ConversionService} implementation delegates the actual conversion to the {@literal ConversionService} it
 * finds in its internal {@link Stack} that claims to be able to convert a given class. It will roll through the
 * {@literal ConversionService}s until it finds one that can convert the given type.
 * 
 * @author Jon Brisbin
 * @authot Oliver Gierke
 */
public class DelegatingConversionService implements ConversionService {

	private final Stack<ConversionService> conversionServices;

	public DelegatingConversionService(ConversionService... svcs) {

		this.conversionServices = new Stack<ConversionService>();

		for (ConversionService svc : svcs) {
			Assert.notNull(svc);
			conversionServices.add(svc);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.ConversionService#canConvert(java.lang.Class, java.lang.Class)
	 */
	@Override
	public boolean canConvert(Class<?> from, Class<?> to) {

		for (ConversionService svc : conversionServices) {
			if (svc.canConvert(from, to)) {
				return true;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.ConversionService#canConvert(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean canConvert(TypeDescriptor from, TypeDescriptor to) {

		for (ConversionService svc : conversionServices) {
			if (svc.canConvert(from, to)) {
				return true;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.ConversionService#convert(java.lang.Object, java.lang.Class)
	 */
	@Override
	public <T> T convert(Object o, Class<T> type) {

		for (ConversionService svc : conversionServices) {
			if (svc.canConvert(o.getClass(), type)) {
				return svc.convert(o, type);
			}
		}

		throw new ConverterNotFoundException(TypeDescriptor.forObject(o), TypeDescriptor.valueOf(type));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.ConversionService#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public Object convert(Object o, TypeDescriptor from, TypeDescriptor to) {

		for (ConversionService svc : conversionServices) {
			if (svc.canConvert(from, to)) {
				return svc.convert(o, from, to);
			}
		}

		throw new ConverterNotFoundException(from, to);
	}
}
