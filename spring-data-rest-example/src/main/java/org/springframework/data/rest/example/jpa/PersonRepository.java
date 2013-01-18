package org.springframework.data.rest.example.jpa;

import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.convert.ISO8601DateConverter;
import org.springframework.data.rest.repository.annotation.ConvertWith;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * A repository to manage {@link Person}s.
 *
 * @author Jon Brisbin
 */
@RestResource(rel = "people", path = "people")
public interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

  @RestResource(rel = "firstname", path = "firstname")
  public Page<Person> findByFirstName(@Param("firstName") String firstName, Pageable pageable);

  public Person findFirstPersonByFirstName(@Param("firstName") String firstName);

  public Page<Person> findByCreatedGreaterThan(@Param("date") Date date, Pageable pageable);

  @Query("select p from Person p where p.created > :date")
  public Page<Person> findByCreatedUsingISO8601Date(@Param("date")
                                                    @ConvertWith(
                                                        ISO8601DateConverter.class)
                                                    Date date,
                                                    Pageable pageable);

}
