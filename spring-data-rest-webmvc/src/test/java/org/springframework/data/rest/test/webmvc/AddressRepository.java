package org.springframework.data.rest.test.webmvc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface AddressRepository extends CrudRepository<Address, Long> {
}
