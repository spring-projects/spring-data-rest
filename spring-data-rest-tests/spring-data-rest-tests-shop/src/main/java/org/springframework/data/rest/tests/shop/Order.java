/*
 * Copyright 2014-2025 the original author or authors.
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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.tests.shop.LineItem.LineItemProductsOnlyProjection;
import org.springframework.data.rest.tests.shop.Order.OrderIdentifier;
import org.springframework.util.ObjectUtils;

/**
 * @author Oliver Gierke
 * @author Craig Andrews
 */
public final class Order implements AggregateRoot<Order, OrderIdentifier> {

	private final @Id OrderIdentifier id = new OrderIdentifier(UUID.randomUUID());
	private final List<LineItem> items = new ArrayList<>();
	private final @Reference Customer customer;

	public Order(Customer customer) {
		this.customer = customer;
	}

	public OrderIdentifier getId() {
		return this.id;
	}

	public List<LineItem> getItems() {
		return this.items;
	}

	public Customer getCustomer() {
		return this.customer;
	}

	public Order add(LineItem item) {

		this.items.add(item);
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Order order = (Order) o;

		if (!ObjectUtils.nullSafeEquals(id, order.id)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(items, order.items)) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(customer, order.customer);
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(id);
		result = 31 * result + ObjectUtils.nullSafeHashCode(items);
		result = 31 * result + ObjectUtils.nullSafeHashCode(customer);
		return result;
	}

	@Projection(name = "itemsOnly", types = Order.class)
	public interface OrderItemsOnlyProjection {
		List<LineItemProductsOnlyProjection> getItems();
	}

	static final class OrderIdentifier implements Identifier, Serializable {
		private static final @Serial long serialVersionUID = -3362660123468974881L;
		private final UUID id;

		public OrderIdentifier(UUID id) {
			this.id = id;
		}

		public UUID getId() {
			return this.id;
		}

		public boolean equals(final Object o) {
			if (o == this)
				return true;
			if (!(o instanceof OrderIdentifier))
				return false;
			final OrderIdentifier other = (OrderIdentifier) o;
			final Object this$id = this.getId();
			final Object other$id = other.getId();
			if (this$id == null ? other$id != null : !this$id.equals(other$id))
				return false;
			return true;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $id = this.getId();
			result = result * PRIME + ($id == null ? 43 : $id.hashCode());
			return result;
		}

		public String toString() {
			return "Order.OrderIdentifier(id=" + this.getId() + ")";
		}
	}
}
