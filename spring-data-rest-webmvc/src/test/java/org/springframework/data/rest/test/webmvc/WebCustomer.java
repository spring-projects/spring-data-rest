package org.springframework.data.rest.test.webmvc;

import javax.persistence.Entity;

/**
 * @author Jon Brisbin
 */
@Entity
public class WebCustomer extends Customer {

  private String username;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

}
