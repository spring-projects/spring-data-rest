/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.support;

import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;

/**
 * A {@link HandlerMapping} that considers a {@link List} of delegates. It will keep on traversing the delegates in case
 * an {@link HttpMediaTypeNotAcceptableException} is thrown while trying to lookup the handler on a particular delegate.
 *
 * @author Oliver Gierke
 * @soundtrack Benny Greb - Stabila (Moving Parts)
 */
public class DelegatingHandlerMapping implements HandlerMapping, Ordered, MatchableHandlerMapping {

	private final @Getter List<HandlerMapping> delegates;

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
		return HandlerSelectionResult.from(request, delegates).resultOrException();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.MatchableHandlerMapping#match(javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {

		try {
			return HandlerSelectionResult.from(request, delegates).match(pattern);
		} catch (Exception o_O) {
			return null;
		}
	}

	@Value
	private static class HandlerSelectionResult {

		@NonNull HttpServletRequest request;
		HandlerMapping mapping;
		HandlerExecutionChain result;
		Exception ignoredException;

		public static HandlerSelectionResult from(HttpServletRequest request, Iterable<HandlerMapping> delegates)
				throws Exception {

			Exception ignoredException = null;

			for (HandlerMapping delegate : delegates) {

				try {

					HandlerExecutionChain result = delegate.getHandler(request);

					if (result != null) {
						return HandlerSelectionResult.forResult(request, delegate, result);
					}

				} catch (HttpMediaTypeNotSupportedException o_O) {
					ignoredException = o_O;
				} catch (HttpMediaTypeNotAcceptableException o_O) {
					ignoredException = o_O;
				} catch (HttpRequestMethodNotSupportedException o_O) {
					ignoredException = o_O;
				} catch (UnsatisfiedServletRequestParameterException o_O) {
					ignoredException = o_O;
				}
			}

			return HandlerSelectionResult.withoutResult(request, ignoredException);
		}

		private static HandlerSelectionResult forResult(HttpServletRequest request, HandlerMapping delegate,
				HandlerExecutionChain result) {
			return new HandlerSelectionResult(request, delegate, result, null);
		}

		private static HandlerSelectionResult withoutResult(HttpServletRequest request, Exception exception) {
			return new HandlerSelectionResult(request, null, null, exception);
		}

		public HandlerExecutionChain resultOrException() throws Exception {

			if (ignoredException != null) {
				throw ignoredException;
			}

			return result;
		}

		public RequestMatchResult match(String pattern) {

			return MatchableHandlerMapping.class.isInstance(mapping) //
					? ((MatchableHandlerMapping) mapping).match(request, pattern) //
					: null;
		}
	}
}
