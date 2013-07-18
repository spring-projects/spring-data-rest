package org.springframework.data.rest.core.domain.jpa;

import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * A repository to manage {@link Person}s.
 * 
 * @author Jon Brisbin
 */
@RestResource(rel = "people", path = "people")
public interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

	@RestResource(rel = "firstname", path = "firstname")
	Page<Person> findByFirstName(@Param("firstName") String firstName, Pageable pageable);

	Page<Person> findByCreatedGreaterThan(@Param("date") Date date, Pageable pageable);

	@Query("select p from Person p where p.created > :date")
	Page<Person> findByCreatedUsingISO8601Date(@Param("date") @DateTimeFormat(iso = ISO.DATE_TIME) Date date,
			Pageable pageable);
}
