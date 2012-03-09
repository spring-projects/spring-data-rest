package org.springframework.data.rest.repository.test;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface PersonRepository extends CrudRepository<Person, Long> {
}
