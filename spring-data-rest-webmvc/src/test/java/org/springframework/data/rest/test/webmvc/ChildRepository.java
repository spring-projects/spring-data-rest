package org.springframework.data.rest.test.webmvc;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Jon Brisbin
 */
public interface ChildRepository extends JpaRepository<Child, Long> {
}
