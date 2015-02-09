/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.client;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoModule;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.format.DistanceFormatter;
import org.springframework.data.geo.format.PointFormatter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Oliver Gierke
 */
public class RestClientRepositoryFactoryIntegrationTests {

	@Test
	public void testname() {

		FormattingConversionService conversionService = new FormattingConversionService();
		conversionService.addFormatter(PointFormatter.INSTANCE);
		conversionService.addFormatter(DistanceFormatter.INSTANCE);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new Jackson2HalModule());
		mapper.registerModule(new GeoModule());

		MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
		halConverter.setObjectMapper(mapper);
		halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new StringHttpMessageConverter(Charset.forName("UTF-8")));
		converters.add(halConverter);

		RestTemplate template = new RestTemplate();
		template.setMessageConverters(converters);

		RestClientRepositoryFactory factory = new RestClientRepositoryFactory(template, conversionService);

		StoreRepository repository = factory.getRepository(StoreRepository.class);

		for (Store store : repository.foo(new Point(47.11, 47.11), new Distance(10000, Metrics.KILOMETERS))) {

			System.out.println(String.format("%s, %s", store.name, store.address.location));
		}
	}
}
