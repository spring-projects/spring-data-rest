package org.springframework.data.rest.example.mongodb;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Jon Brisbin
 */
public interface ProfileRepository extends CrudRepository<Profile, String> {
}
