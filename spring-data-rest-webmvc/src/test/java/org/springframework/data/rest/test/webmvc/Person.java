package org.springframework.data.rest.test.webmvc;

import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Entity
public class Person {

  @Id @GeneratedValue private Long id;
  private String name;
  @Version
  private Long version;
  @OneToMany
  private List<Address> addresses;
  @OneToMany
  private Map<String, Profile> profiles;

  public Person() {
  }

  public Person(Long id, String name, List<Address> addresses, Map<String, Profile> profiles) {
    this.id = id;
    this.name = name;
    this.addresses = addresses;
    this.profiles = profiles;
  }

  public Person(String name, List<Address> addresses, Map<String, Profile> profiles) {
    this.name = name;
    this.addresses = addresses;
    this.profiles = profiles;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public List<Address> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<Address> addresses) {
    this.addresses = addresses;
  }

  public Map<String, Profile> getProfiles() {
    return profiles;
  }

  public void setProfiles(Map<String, Profile> profiles) {
    this.profiles = profiles;
  }

}
