/*
 * Copyright 2014-2024 the original author or authors.
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.tests.shop.Product.ProductNameOnlyProjection;
import org.springframework.util.ObjectUtils;

/**
 * @author Oliver Gierke
 * @author Craig Andrews
 */
public class LineItem {

	private final @Id UUID id = UUID.randomUUID();
	private final String description;
	private final BigDecimal price;

	private final @Reference Product product;
	private final @Reference List<Product> products;

	private final @Reference LineItemType type;
	private final @Reference List<LineItemType> types;

	public LineItem(Product product, LineItemType type) {

		this.price = product.getPrice();
		this.description = product.getName();

		this.type = type;
		this.types = Arrays.asList(type, type);

		this.product = product;
		this.products = Arrays.asList(product, product);
	}

	public UUID getId() {
		return this.id;
	}

	public String getDescription() {
		return this.description;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public Product getProduct() {
		return this.product;
	}

	public List<Product> getProducts() {
		return this.products;
	}

	public LineItemType getType() {
		return this.type;
	}

	public List<LineItemType> getTypes() {
		return this.types;
	}

	@Projection(name = "productsOnly", types = LineItem.class)
	public interface LineItemProductsOnlyProjection {
		List<ProductNameOnlyProjection> getProducts();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		LineItem lineItem = (LineItem) o;

		return ObjectUtils.nullSafeEquals(id, lineItem.id);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(id);
	}
}
