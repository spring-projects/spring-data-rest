/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.support.DomainClassResolver;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Translator for {@link Sort} arguments that is aware of Jackson-Mapping on domain classes. Jackson field names are
 * translated to {@link PersistentProperty} names. Domain class are looked up by resolving request URLs to mapped
 * repositories.
 *
 * @author Mark Paluch
 * @since 2.6, 2.5.3
 */
@RequiredArgsConstructor
public class JacksonMappingAwareSortTranslator {

	private final @NonNull ObjectMapper objectMapper;
	private final @NonNull Repositories repositories;
	private final @NonNull DomainClassResolver domainClassResolver;

	/**
	 * Translates Jackson field names within a {@link Sort} to {@link PersistentProperty} property names.
	 * 
	 * @param input must not be {@literal null}.
	 * @param parameter must not be {@literal null}.
	 * @param webRequest must not be {@literal null}.
	 * @return a {@link Sort} containing translated property names or {@literal null} the resulting {@link Sort} contains
	 *         no properties.
	 */
	protected Sort translateSort(Sort input, MethodParameter parameter, NativeWebRequest webRequest) {

		Assert.notNull(input, "Sort must not be null!");
		Assert.notNull(parameter, "MethodParameter must not be null!");
		Assert.notNull(webRequest, "NativeWebRequest must not be null!");

		Class<?> domainClass = domainClassResolver.resolve(parameter.getMethod(), webRequest);
		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(domainClass);
		MappedProperties mappedProperties = MappedProperties.fromJacksonProperties(persistentEntity, objectMapper);

		return new SortTranslator(mappedProperties).translateSort(input);
	}

	/**
	 * Translates {@link Sort} orders from Jackson-mapped field names to {@link PersistentProperty} names.
	 *
	 * @author Mark Paluch
	 * @author Oliver Gierke
	 * @since 2.6, 2.5.3
	 */
	@RequiredArgsConstructor
	static class SortTranslator {

		private final @NonNull MappedProperties mappedProperties;

		/**
		 * Translates {@link Sort} orders from Jackson-mapped field names to {@link PersistentProperty} names. Properties
		 * that cannot be resolved are dropped.
		 *
		 * @param input must not be {@literal null}.
		 * @return {@link Sort} with translated field names or {@literal null} if translation dropped all sort fields.
		 */
		Sort translateSort(Sort input) {

			List<Order> filteredOrders = new ArrayList<Order>();

			for (Order order : input) {

				if (mappedProperties.hasPersistentPropertyForField(order.getProperty())) {

					PersistentProperty<?> persistentProperty = mappedProperties.getPersistentProperty(order.getProperty());
					Order mappedOrder = new Order(order.getDirection(), persistentProperty.getName(), order.getNullHandling());
					filteredOrders.add(order.isIgnoreCase() ? mappedOrder.ignoreCase() : mappedOrder);
				}
			}

			return filteredOrders.isEmpty() ? null : new Sort(filteredOrders);
		}
	}
}
