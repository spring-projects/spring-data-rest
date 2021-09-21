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
package org.springframework.data.rest.webmvc.jpa;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.jpa.mapping.JpaPersistentEntity;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for DATAREST-262, checking serialization and deserialization of associations within embeddables.
 *
 * @author Oliver Gierke
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class DataRest262Tests {

	@Configuration
	@Import({ RepositoryRestMvcConfiguration.class, JpaInfrastructureConfig.class })
	@EnableJpaRepositories(considerNestedRepositories = true)
	static class Config {

	}

	@Autowired ApplicationContext beanFactory;
	@Autowired JpaMetamodelMappingContext mappingContext;
	@Autowired AirportRepository repository;

	ObjectMapper mapper;

	@BeforeEach
	void setUp() {

		this.mapper = beanFactory //
				.getBean("halJacksonHttpMessageConverter", AbstractJackson2HttpMessageConverter.class) //
				.getObjectMapper() //
				.copy();

		this.mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	}

	@Test // DATAREST-262
	void deserializesNestedAssociation() throws Exception {

		Airport airport = repository.save(new Airport());
		String payload = "{\"orgOrDstFlightPart\":{\"airport\":\"/api/airports/" + airport.id + "\"}}";

		AircraftMovement result = mapper.readValue(payload, AircraftMovement.class);
		assertThat(result.orgOrDstFlightPart.airport.id).isEqualTo(airport.id);
	}

	@Test // DATAREST-262
	void serializesLinksToNestedAssociations() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE);
		RequestAttributes attributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(attributes);

		Airport first = new Airport();
		first.id = 1L;

		Airport second = new Airport();
		second.id = 2L;

		FlightPart part = new FlightPart();
		part.airport = second;

		AircraftMovement movement = new AircraftMovement();
		movement.id = 3L;
		movement.originOrDestinationAirport = first;
		movement.orgOrDstFlightPart = part;

		JpaPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(AircraftMovement.class);

		EntityModel<Object> resource = PersistentEntityResource.build(movement, persistentEntity).//
				withLink(Link.of("/api/airports/" + movement.id)).//
				build();

		String result = mapper.writeValueAsString(resource);

		assertThat(JsonPath.<Object> read(result, "$._links.self")).isNotNull();
		assertThat(JsonPath.<Object> read(result, "$._links.originOrDestinationAirport")).isNotNull();
		assertThat(JsonPath.<Object> read(result, "$.orgOrDstFlightPart._links.airport")).isNotNull();
	}

	public interface AircraftMovementRepository extends CrudRepository<AircraftMovement, Long> {

	}

	public interface AirportRepository extends CrudRepository<Airport, Long> {

	}

	@Entity(name = "aircraftmovement")
	public static class AircraftMovement {

		@Id @GeneratedValue Long id;
		@ManyToOne Airport originOrDestinationAirport;
		@Embedded @NotNull FlightPart orgOrDstFlightPart;
	}

	@Embeddable
	public static class FlightPart {
		@ManyToOne Airport airport;
	}

	@Entity(name = "airport")
	public static class Airport {
		@Id @GeneratedValue Long id;
	}
}
