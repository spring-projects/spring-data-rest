package org.springframework.data.rest.test.webmvc;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface UuidTestRepository extends CrudRepository<UuidTest, UUID> {
}
