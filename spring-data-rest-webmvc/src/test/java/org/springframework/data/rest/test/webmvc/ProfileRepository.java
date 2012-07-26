package org.springframework.data.rest.test.webmvc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface ProfileRepository extends CrudRepository<Profile, Long> {

  public Address findByPerson(@Param("person") Person person);

}
