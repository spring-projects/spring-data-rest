package org.springframework.data.rest.webmvc.mongodb;

import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Jon Brisbin
 */
public interface ProfileRepository extends PagingAndSortingRepository<Profile, String> {
}
