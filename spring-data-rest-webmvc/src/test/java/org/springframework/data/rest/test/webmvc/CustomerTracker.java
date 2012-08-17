package org.springframework.data.rest.test.webmvc;

import java.util.Collections;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Jon Brisbin
 */
@Entity
public class CustomerTracker {

  @Id @GeneratedValue private Long id;
  @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<Customer> customers = Collections.emptyList();

  public Long getId() {
    return id;
  }

  public List<Customer> getCustomers() {
    return customers;
  }

  public CustomerTracker setCustomers(List<Customer> customers) {
    this.customers = customers;
    return this;
  }

}
