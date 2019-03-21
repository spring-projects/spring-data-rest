/*
 * Copyright 2014-2018 the original author or authors.
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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.tests.shop.Customer.Gender;
import org.springframework.data.rest.tests.shop.Product.ProductNameOnlyProjection;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

/**
 * @author Oliver Gierke
 */
@Configuration
@EnableMapRepositories
public class ShopConfiguration {

	@Autowired CustomerRepository customers;
	@Autowired OrderRepository orders;
	@Autowired ProductRepository products;
	@Autowired LineItemTypeRepository lineItemTypes;

	@Bean
	public ResourceProcessor<Resource<LineItem>> lineItemResourceProcessor() {
		return new ResourceProcessor<Resource<LineItem>>() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.hateoas.ResourceProcessor#process(org.springframework.hateoas.ResourceSupport)
			 */
			@Override
			public Resource<LineItem> process(Resource<LineItem> resource) {
				resource.add(new Link("foo", "bar"));
				return resource;
			}
		};
	}

	@Bean
	public ResourceProcessor<Resource<ProductNameOnlyProjection>> productNameOnlyProjectionResourceProcessor() {

		return new ResourceProcessor<Resource<ProductNameOnlyProjection>>() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.hateoas.ResourceProcessor#process(org.springframework.hateoas.ResourceSupport)
			 */
			@Override
			public Resource<ProductNameOnlyProjection> process(Resource<Product.ProductNameOnlyProjection> resource) {
				resource.add(new Link("alpha", "beta"));
				return resource;
			}
		};
	}

	@PostConstruct
	public void init() {

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
	static class SpringDataRestConfiguration extends RepositoryRestConfigurerAdapter {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter#configureRepositoryRestConfiguration(org.springframework.data.rest.core.config.RepositoryRestConfiguration)
		 */
		@Override
		public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {

			config.withEntityLookup().forRepository(ProductRepository.class, Product::getName, ProductRepository::findByName);
			config.withEntityLookup().forValueRepository(LineItemTypeRepository.class, LineItemType::getName,
					LineItemTypeRepository::findByName);
		}
	}
}
