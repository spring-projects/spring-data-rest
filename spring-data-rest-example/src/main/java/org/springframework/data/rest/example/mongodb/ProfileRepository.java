package org.springframework.data.rest.example.mongodb;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * @author Jon Brisbin
 */
@RestResource(exported = false)
public interface ProfileRepository extends CrudRepository<Profile, String> {
}
