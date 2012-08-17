package org.springframework.data.rest.test.webmvc;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Jon Brisbin
 */
@Entity
public class Customer {

  @Id @GeneratedValue private Long   id;
  private                     String userid;

  public Long getId() {
    return id;
  }

  public String getUserid() {
    return userid;
  }

  public Customer setUserid(String userid) {
    this.userid = userid;
    return this;
  }

}
