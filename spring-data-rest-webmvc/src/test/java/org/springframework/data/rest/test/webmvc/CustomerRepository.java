package org.springframework.data.rest.test.webmvc;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Jon Brisbin
 */
public interface CustomerRepository extends CrudRepository<Customer, Long> {
}
