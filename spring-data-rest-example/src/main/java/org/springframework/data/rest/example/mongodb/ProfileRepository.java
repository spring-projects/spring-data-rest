package org.springframework.data.rest.example.mongodb;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * @author Jon Brisbin
 */
@RestResource(exported = true)
public interface ProfileRepository extends CrudRepository<Profile, String> {
}
