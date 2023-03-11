/*
 * Copyright 2023 the original author or authors.
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

import org.springframework.data.rest.core.AggregateReference;
import org.springframework.data.rest.core.AssociationAggregateReference;
import org.springframework.data.rest.tests.shop.Order.OrderIdentifier;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Custom controller to mimic a user-defined one using {@link AggregateReference}s to receive references to other Spring
 * Data REST managed aggregates.
 *
 * @author Oliver Drotbohm
 * @since 4.1
 */
@ResponseBody
@BasePathAwareController
class CustomController {

	@GetMapping("/order-custom-id")
	OrderIdentifier customOrderId(@RequestParam("order") AggregateReference<Order, OrderIdentifier> reference) {

		return reference //
				.withIdSource(it -> it.getPathSegments().get(3)) //
				.resolveId();
	}

	@GetMapping("/order-custom-association")
	OrderIdentifier customOrderAssociation(
			@RequestParam("order") AssociationAggregateReference<Order, OrderIdentifier> reference) {

		return reference //
				.withIdSource(it -> it.getPathSegments().get(3)) //
				.resolveAssociation() //
				.getId();
	}

	@GetMapping("/order-custom")
	OrderIdentifier customOrder(@RequestParam("order") AggregateReference<Order, OrderIdentifier> reference) {

		return reference //
				.withIdSource(it -> it.getPathSegments().get(3)) //
				.resolveAggregate() //
				.getId();
	}
}
