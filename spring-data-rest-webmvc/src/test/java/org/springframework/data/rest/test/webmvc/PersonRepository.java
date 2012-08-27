package org.springframework.data.rest.test.webmvc;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * Example {@link org.springframework.data.repository.CrudRepository} for dealing with a {@link Person}. Also uses the
 * {@link RestResource} annotation to turn off the delete methods.
 *
 * @author Jon Brisbin
 */
@RestResource(path = "people", rel = "peeps")
public interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

  @Override
  @RestResource(exported = false) void delete(Long id);

  @Override
  @RestResource(exported = false) void delete(Person entity);

  @RestResource(path = "name", rel = "names") List<Person> findByName(@Param("name") String name);

  @RestResource(path = "nameStartsWith", rel = "nameStartsWith")
  Page findByNameStartsWith(@Param("name") String name, Pageable p);

  @Query("select count(p) from Person p")
  @RestResource(path = "count") Long personCount();

  @Query("select p from Person p where p.id in(:ids)")
  @RestResource(path = "id") Page<Person> findById(@Param("ids") List<Long> ids, Pageable pageable);

}
