package org.springframework.data.rest.test.webmvc;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@RestResource(path = "people", rel = "peeps")
public interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

  @RestResource(path = "name", rel = "names")
  public List<Person> findByName(@Param("name") String name);

  @RestResource(path = "nameStartsWith", rel = "nameStartsWith")
  public Page findByNameStartsWith(@Param("name") String name, Pageable p);

}
