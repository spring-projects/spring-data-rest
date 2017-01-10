/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.DomainClassResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Translator for {@link Sort} arguments that is aware of Jackson-Mapping on domain classes. Jackson field names are
 * translated to {@link PersistentProperty} names. Domain class is looked up by resolving request URLs to mapped
 * repositories. {@link Sort} translation is skipped if a domain class cannot be resolved.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.6
 */
@RequiredArgsConstructor
public class JacksonMappingAwareSortTranslator {

	private final Repositories repositories;
	private final DomainClassResolver domainClassResolver;
	private final SortTranslator sortTranslator;

	/**
	 * Creates a new {@link JacksonMappingAwareSortTranslator} for the given {@link ObjectMapper}, {@link Repositories},
	 * {@link DomainClassResolver} and {@link PersistentEntities}.
	 * 
	 * @param objectMapper must not be {@literal null}.
	 * @param repositories must not be {@literal null}.
	 * @param domainClassResolver must not be {@literal null}.
	 * @param persistentEntities must not be {@literal null}.
	 * @param associations must not be {@literal null}.
	 */
	public JacksonMappingAwareSortTranslator(ObjectMapper objectMapper, Repositories repositories,
			DomainClassResolver domainClassResolver, PersistentEntities persistentEntities, Associations associations) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(domainClassResolver, "DomainClassResolver must not be null!");
		Assert.notNull(associations, "Associations must not be null!");

		this.repositories = repositories;
		this.domainClassResolver = domainClassResolver;
		this.sortTranslator = new SortTranslator(persistentEntities, objectMapper, associations);
	}

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

		if (domainClass == null) {
			return input;
		}

		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(domainClass);

		return sortTranslator.translateSort(input, persistentEntity);
	}

	/**
	 * Translates {@link Sort} orders from Jackson-mapped field names to {@link PersistentProperty} names.
	 *
	 * @author Mark Paluch
	 * @author Oliver Gierke
	 * @since 2.6
	 */
	@RequiredArgsConstructor
	public static class SortTranslator {

		private static final String DELIMITERS = "_\\.";
		private static final String ALL_UPPERCASE = "[A-Z0-9._$]+";

		private static final Pattern SPLITTER = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", DELIMITERS));

		private final @NonNull PersistentEntities persistentEntities;
		private final @NonNull ObjectMapper objectMapper;
		private final @NonNull Associations associations;

		/**
		 * Translates {@link Sort} orders from Jackson-mapped field names to {@link PersistentProperty} names. Properties
		 * that cannot be resolved are dropped.
		 *
		 * @param input must not be {@literal null}.
		 * @param rootEntity must not be {@literal null}.
		 * @return {@link Sort} with translated field names or {@literal null} if translation dropped all sort fields.
		 */
		public Sort translateSort(Sort input, PersistentEntity<?, ?> rootEntity) {

			Assert.notNull(input, "Sort must not be null!");
			Assert.notNull(rootEntity, "PersistentEntity must not be null!");

			List<Order> filteredOrders = new ArrayList<Order>();

			for (Order order : input) {

				List<String> iteratorSource = new ArrayList<String>();
				Matcher matcher = SPLITTER.matcher("_" + order.getProperty());

				while (matcher.find()) {
					iteratorSource.add(matcher.group(1));
				}

				String mappedPropertyPath = getMappedPropertyPath(rootEntity, iteratorSource);

				if (mappedPropertyPath != null) {
					filteredOrders.add(order.withProperty(mappedPropertyPath));
				}
			}

			return filteredOrders.isEmpty() ? null : new Sort(filteredOrders);
		}

		private String getMappedPropertyPath(PersistentEntity<?, ?> rootEntity, List<String> iteratorSource) {

			List<String> persistentPropertyPath = mapPropertyPath(rootEntity, iteratorSource);

			if (persistentPropertyPath.isEmpty()) {
				return null;
			}

			return StringUtils.collectionToDelimitedString(persistentPropertyPath, ".");
		}

		private List<String> mapPropertyPath(PersistentEntity<?, ?> rootEntity, List<String> iteratorSource) {

			List<String> persistentPropertyPath = new ArrayList<String>(iteratorSource.size());

			TypedSegment typedSegment = TypedSegment.create(persistentEntities, objectMapper, rootEntity);

			for (String field : iteratorSource) {

				String fieldName = field.matches(ALL_UPPERCASE) ? field : StringUtils.uncapitalize(field);

				if (!typedSegment.hasPersistentPropertyForField(fieldName)) {
					return Collections.emptyList();
				}

				List<? extends PersistentProperty<?>> persistentProperties = typedSegment.getPersistentProperties(fieldName);

				for (PersistentProperty<?> persistentProperty : persistentProperties) {

					if (associations.isLinkableAssociation(persistentProperty)) {
						return Collections.emptyList();
					}

					persistentPropertyPath.add(persistentProperty.getName());
				}

				typedSegment = typedSegment.next(persistentProperties.get(persistentProperties.size() - 1));
			}

			return persistentPropertyPath;
		}
	}

	/**
	 * A typed segment inside a Jackson property path. {@link TypedSegment} represents a segment in JSON field path to
	 * {@link PersistentProperty} mapping.
	 *
	 * @author Mark Paluch
	 */
	static class TypedSegment {

		private final PersistentEntities persistentEntities;
		private final ObjectMapper objectMapper;
		private final PersistentEntity<?, ?> currentType;
		private final MappedProperties currentProperties;
		private final WrappedProperties currentWrappedProperties;

		private TypedSegment(TypedSegment previous, PersistentEntity<?, ?> persistentEntity) {
			this(previous.persistentEntities, previous.objectMapper, persistentEntity);
		}

		private TypedSegment(PersistentEntities persistentEntities, ObjectMapper objectMapper,
				PersistentEntity<?, ?> persistentEntity) {

			this.persistentEntities = persistentEntities;
			this.objectMapper = objectMapper;
			this.currentType = persistentEntity;

			if (persistentEntity != null) {

				this.currentProperties = MappedProperties.fromJacksonProperties(currentType, objectMapper);
				this.currentWrappedProperties = WrappedProperties.fromJacksonProperties(persistentEntities, currentType,
						objectMapper);

			} else {

				this.currentProperties = null;
				this.currentWrappedProperties = null;
			}
		}

		/**
		 * Creates the initial {@link TypedSegment} given {@link PersistentEntities}, {@link ObjectMapper} and
		 * {@link PersistentEntity}.
		 * 
		 * @param persistentEntities must not be {@literal null}.
		 * @param objectMapper must not be {@literal null}.
		 * @param rootEntity the initial entity to start mapping from, must not be {@literal null}.
		 * @return
		 */
		public static TypedSegment create(PersistentEntities persistentEntities, ObjectMapper objectMapper,
				PersistentEntity<?, ?> rootEntity) {

			Assert.notNull(persistentEntities, "PersistentEntities must not be null!");
			Assert.notNull(objectMapper, "ObjectMapper must not be null!");
			Assert.notNull(rootEntity, "PersistentEntity must not be null!");

			return new TypedSegment(persistentEntities, objectMapper, rootEntity);
		}

		/**
		 * Continue mapping by providing the next {@link PersistentProperty}.
		 *
		 * @param persistentProperty must not be {@literal null}.
		 * @return
		 */
		public TypedSegment next(PersistentProperty<?> persistentProperty) {

			Assert.notNull(persistentProperty, "PersistentProperty must not be null!");

			PersistentEntity<?, ?> persistentEntity = persistentEntities.getPersistentEntity(persistentProperty.getType());
			return new TypedSegment(this, persistentEntity);
		}

		private boolean hasPersistentPropertyForField(String fieldName) {

			return currentType != null && (currentProperties.hasPersistentPropertyForField(fieldName)
					|| currentWrappedProperties.hasPersistentPropertiesForField(fieldName));
		}

		private List<? extends PersistentProperty<?>> getPersistentProperties(String fieldName) {

			if (currentWrappedProperties.hasPersistentPropertiesForField(fieldName)) {
				return currentWrappedProperties.getPersistentProperties(fieldName);
			}

			return Collections.singletonList(currentProperties.getPersistentProperty(fieldName));
		}
	}
}
