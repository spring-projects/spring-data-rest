/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A {@link HandlerMapping} that considers a {@link List} of delegates. It will keep on traversing the delegates in case
 * an {@link HttpMediaTypeNotAcceptableException} is thrown while trying to lookup the handler on a particular delegate.
 *
 * @author Oliver Gierke
 * @soundtrack Benny Greb - Stabila (Moving Parts)
 */
class DelegatingHandlerMapping implements MatchableHandlerMapping, Iterable<HandlerMapping>, Ordered {

	private final List<HandlerMapping> delegates;
	private final @Nullable PathPatternParser parser;

	/**
	 * Creates a new {@link DelegatingHandlerMapping} for the given delegates.
	 *
	 * @param delegates must not be {@literal null}.
	 */
	public DelegatingHandlerMapping(List<HandlerMapping> delegates, @Nullable PathPatternParser parser) {

		Assert.notNull(delegates, "Delegates must not be null!");

		this.delegates = delegates;
		this.parser = parser;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.HandlerMapping#usesPathPatterns()
	 */
	@Override
	public boolean usesPathPatterns() {
		return parser != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.MatchableHandlerMapping#getPatternParser()
	 */
	@Nullable
	@Override
	public PathPatternParser getPatternParser() {
		return parser;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.support.DelegatingHandlerMapping#getDelegates()
	 */
	@SuppressWarnings("all")
	public List<HandlerMapping> getDelegates() {
		return this.delegates;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<HandlerMapping> iterator() {
		return delegates.iterator();
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
	 * @see org.springframework.web.servlet.HandlerMapping#getHandler(jakarta.servlet.http.HttpServletRequest)
	 */
	@Override
	public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		return HandlerSelectionResult.from(request, delegates).resultOrException();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.MatchableHandlerMapping#match(jakarta.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {

		try {
			return HandlerSelectionResult.from(request, delegates).match(pattern);
		} catch (Exception o_O) {
			return null;
		}
	}

	private static class HandlerSelectionResult {

		private final HttpServletRequest request;
		private final HandlerMapping mapping;
		private final HandlerExecutionChain result;
		private final Exception ignoredException;

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

		public HandlerSelectionResult(HttpServletRequest request, HandlerMapping mapping, HandlerExecutionChain result,
				Exception ignoredException) {

			Assert.notNull(request, "HttpServletRequest must not be null!");

			this.request = request;
			this.mapping = mapping;
			this.result = result;
			this.ignoredException = ignoredException;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final java.lang.Object o) {

			if (o == this) {
				return true;
			}

			if (!(o instanceof DelegatingHandlerMapping.HandlerSelectionResult)) {
				return false;
			}

			HandlerSelectionResult other = (HandlerSelectionResult) o;

			return Objects.equals(request, other.request) //
					&& Objects.equals(mapping, other.mapping) //
					&& Objects.equals(result, other.result) //
					&& Objects.equals(ignoredException, other.ignoredException);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return Objects.hash(request, mapping, result, ignoredException);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public java.lang.String toString() {
			return "DelegatingHandlerMapping.HandlerSelectionResult(request=" + request + ", mapping=" + mapping + ", result="
					+ result + ", ignoredException=" + ignoredException + ")";
		}
	}
}
