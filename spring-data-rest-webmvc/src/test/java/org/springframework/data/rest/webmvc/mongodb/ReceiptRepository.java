package org.springframework.data.rest.webmvc.mongodb;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * A repository to manage {@link Receipt}s.
 *
 * @author Pablo Lozano
 */

public interface ReceiptRepository extends CrudRepository<Receipt, String> {

}