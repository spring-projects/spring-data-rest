/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.rest.tests.shop;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.rest.core.config.Projection;

/**
 * @author Oliver Gierke
 * @author Craig Andrews
 */
@NonFinal
@Value
@RequiredArgsConstructor
public class Product {

	@Projection(name = "nameOnly", types = Product.class)
	public interface ProductNameOnlyProjection {
		String getName();
	}

	private final @Id UUID id = UUID.randomUUID();
	private final String name;
	private final BigDecimal price;
}
