package org.springframework.data.rest.test.webmvc;

import javax.persistence.Entity;

/**
 * @author Jon Brisbin
 */
@Entity
public class Child extends Parent {

  private String occupation;

  public Child() {
  }

  public Child(String name, String occupation) {
    super(name);
    this.occupation = occupation;
  }

  public String getOccupation() {
    return occupation;
  }

  public void setOccupation(String occupation) {
    this.occupation = occupation;
  }

}
