package org.springframework.data.rest.test.webmvc;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@RestResource(exported = false)
public interface UuidTestRepository
    extends CrudRepository<UuidTest, UUID> {
}
