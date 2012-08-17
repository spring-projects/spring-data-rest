package org.springframework.data.rest.test.webmvc;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Jon Brisbin
 */
public interface CustomerTrackerRepository extends CrudRepository<CustomerTracker, Long> {
}
