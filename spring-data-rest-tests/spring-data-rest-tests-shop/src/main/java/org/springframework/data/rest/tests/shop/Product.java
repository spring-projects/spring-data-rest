/*
 * Copyright 2016-2025 the original author or authors.
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

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.util.ObjectUtils;

/**
 * @author Oliver Gierke
 * @author Craig Andrews
 */
public class Product {

	private final @Id UUID id = UUID.randomUUID();
	private final String name;
	private final BigDecimal price;

	public Product(String name, BigDecimal price) {
		this.name = name;
		this.price = price;
	}

	public UUID getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Product product = (Product) o;

		if (!ObjectUtils.nullSafeEquals(id, product.id)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(name, product.name)) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(price, product.price);
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(id);
		result = 31 * result + ObjectUtils.nullSafeHashCode(name);
		result = 31 * result + ObjectUtils.nullSafeHashCode(price);
		return result;
	}

	public String toString() {
		return "Product(id=" + this.getId() + ", name=" + this.getName() + ", price=" + this.getPrice() + ")";
	}

	@Projection(name = "nameOnly", types = Product.class)
	public interface ProductNameOnlyProjection {
		String getName();
	}

}
