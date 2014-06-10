/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.jpa.mapping.JpaPersistentEntity;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for DATAREST-262, checking serialization and deserialization of associations within embeddables.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DataRest262Tests {

	@Configuration
	@Import({ RepositoryRestMvcConfiguration.class, JpaInfrastructureConfig.class })
	@EnableJpaRepositories(considerNestedRepositories = true)
	static class Config {

	}

	@Autowired ApplicationContext beanFactory;
	@Autowired JpaMetamodelMappingContext mappingContext;
	@Autowired AirportRepository repository;
	@Autowired @Qualifier("halObjectMapper") ObjectMapper mapper;

	@Before
	public void setUp() {
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	}

	/**
	 * @see DATAREST-262
	 */
	@Test
	public void deserializesNestedAssociation() throws Exception {

		Airport airport = repository.save(new Airport());
		String payload = "{\"orgOrDstFlightPart\":{\"airport\":\"/api/airports/" + airport.id + "\"}}";

		AircraftMovement result = mapper.readValue(payload, AircraftMovement.class);
		assertThat(result.orgOrDstFlightPart.airport.id, is(airport.id));
	}

	/**
	 * @see DATAREST-262
	 */
	@Test
	public void serializesLinksToNestedAssociations() throws Exception {

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

		JpaPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(AircraftMovement.class);

		Resource<Object> resource = PersistentEntityResource.build(movement, persistentEntity).//
				withLink(new Link("/api/airports/" + movement.id)).//
				build();

		String result = mapper.writeValueAsString(resource);

		assertThat(JsonPath.read(result, "$_links.self"), is(notNullValue()));
		assertThat(JsonPath.read(result, "$_links.airport"), is(notNullValue()));
		assertThat(JsonPath.read(result, "$_links.originOrDestinationAirport"), is(notNullValue()));
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
