package org.springframework.data.rest.test.webmvc;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface AddressRepository extends CrudRepository<Address, Long> {
}
