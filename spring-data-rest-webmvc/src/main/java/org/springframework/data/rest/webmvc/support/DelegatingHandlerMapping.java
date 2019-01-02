/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A {@link HandlerMapping} that considers a {@link List} of delegates. It will keep on traversing the delegates in case
 * an {@link HttpMediaTypeNotAcceptableException} is thrown while trying to lookup the handler on a particular delegate.
 * 
 * @author Oliver Gierke
 * @soundtrack Benny Greb - Stabila (Moving Parts)
 */
public class DelegatingHandlerMapping implements HandlerMapping, Ordered {

	private final List<HandlerMapping> delegates;

	/**
	 * Creates a new {@link DelegatingHandlerMapping} for the given delegates.
	 * 
	 * @param delegates must not be {@literal null}.
	 */
	public DelegatingHandlerMapping(List<HandlerMapping> delegates) {

		Assert.notNull(delegates, "Delegates must not be null!");

		this.delegates = delegates;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.HandlerMapping#getHandler(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {

		Exception exception = null;

		for (HandlerMapping delegate : delegates) {

			try {

				HandlerExecutionChain result = delegate.getHandler(request);

				if (result != null) {
					return result;
				}

			} catch (HttpMediaTypeNotAcceptableException o_O) {
				exception = o_O;
			} catch (HttpRequestMethodNotSupportedException o_O) {
				exception = o_O;
			} catch (UnsatisfiedServletRequestParameterException o_O) {
				exception = o_O;
			}
		}

		if (exception != null) {
			throw exception;
		}

		return null;
	}
}
