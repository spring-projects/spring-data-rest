package org.springframework.data.rest.core.domain.mongodb;

import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for managing {@link Profile}s in MongoDB.
 * 
 * @author Jon Brisbin
 */
public interface ProfileRepository extends CrudRepository<Profile, ObjectId> {}
