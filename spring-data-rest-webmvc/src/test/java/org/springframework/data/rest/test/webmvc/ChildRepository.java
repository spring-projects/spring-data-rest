package org.springframework.data.rest.test.webmvc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * @author Jon Brisbin
 */
@RestResource(exported = false)
public interface ChildRepository extends JpaRepository<Child, Long> {
}
