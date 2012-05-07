package org.springframework.data.rest.test.webmvc;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Entity
public class UuidTest {

  @Id UUID id = UUID.randomUUID();
  String name;

  public UuidTest() {
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
