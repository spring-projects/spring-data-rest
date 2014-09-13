package org.springframework.data.rest.webmvc.jpa;

import org.springframework.data.repository.CrudRepository;

/**
 * A repository to manage {@link Receipt}s.
 *
 * @author Pablo Lozano
 */

public interface ReceiptRepository extends CrudRepository<Receipt, Long> {

}
