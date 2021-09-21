/*
 * Copyright 2014-2021 the original author or authors.
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

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.rest.core.config.EntityLookupRegistrar;
import org.springframework.data.rest.tests.shop.Customer.Gender;
import org.springframework.data.rest.tests.shop.Product.ProductNameOnlyProjection;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;

/**
 * @author Oliver Gierke
 */
@Configuration
@EnableMapRepositories
class ShopConfiguration {

	@Autowired CustomerRepository customers;
	@Autowired OrderRepository orders;
	@Autowired ProductRepository products;
	@Autowired LineItemTypeRepository lineItemTypes;

	@Bean
	public RepresentationModelProcessor<EntityModel<LineItem>> lineItemResourceProcessor() {
		return new RepresentationModelProcessor<EntityModel<LineItem>>() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.hateoas.server.RepresentationModelProcessor#process(org.springframework.hateoas.RepresentationModel)
			 */
			@Override
			public EntityModel<LineItem> process(EntityModel<LineItem> resource) {
				resource.add(Link.of("foo", "bar"));
				return resource;
			}
		};
	}

	@Bean
	public RepresentationModelProcessor<EntityModel<ProductNameOnlyProjection>> productNameOnlyProjectionResourceProcessor() {

		return new RepresentationModelProcessor<EntityModel<ProductNameOnlyProjection>>() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.hateoas.server.RepresentationModelProcessor#process(org.springframework.hateoas.RepresentationModel)
			 */
			@Override
			public EntityModel<ProductNameOnlyProjection> process(EntityModel<Product.ProductNameOnlyProjection> resource) {
				resource.add(Link.of("alpha", "beta"));
				return resource;
			}
		};
	}

	@PostConstruct
	void init() {

		LineItemType lineItemType = lineItemTypes.save(new LineItemType("good"));

		Product guitar = products.save(new Product("Lakewood guitar", new BigDecimal(1299.0)));
		Product drums = products.save(new Product("Yamaha Drums", new BigDecimal(2999.0)));

		Customer dave = customers.save(new Customer("Dave", "Matthews", Gender.MALE, //
				new Address("4711 Some Place", "54321", "Charlottesville", "VA")));

		Order order = new Order(dave);

		order.add(new LineItem(guitar, lineItemType));
		order.add(new LineItem(drums, lineItemType));

		orders.save(order);
	}

	@Configuration
	static class SpringDataRestConfiguration implements RepositoryRestConfigurer {

		@Bean
		RepositoryRestConfigurer configurer() {

			return RepositoryRestConfigurer.withConfig(config -> {

				EntityLookupRegistrar lookup = config.withEntityLookup();

				lookup.forRepository(ProductRepository.class, Product::getName, ProductRepository::findByName);
				lookup.forValueRepository(LineItemTypeRepository.class, LineItemType::getName,
						LineItemTypeRepository::findByName);
			});
		}
	}
}
