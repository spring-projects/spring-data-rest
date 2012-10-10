package org.springframework.data.rest.test.webmvc;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

/**
 * @author Jon Brisbin
 */
@Entity
public class Customer {

  @Id @GeneratedValue private Long   id;
  @NotNull(message = "no.userid")
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
