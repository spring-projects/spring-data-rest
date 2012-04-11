package org.springframework.data.rest.test.webmvc;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface AddressRepository extends CrudRepository<Address, Long> {
}
