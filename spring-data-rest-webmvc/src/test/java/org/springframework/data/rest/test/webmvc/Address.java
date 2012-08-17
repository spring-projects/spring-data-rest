package org.springframework.data.rest.test.webmvc;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Entity
public class Address {

  @Id @GeneratedValue private Long     id;
  private                     String[] lines;
  private                     String   city;
  private                     String   province;
  private                     String   postalCode;
  @ManyToOne(optional = false, cascade = CascadeType.REMOVE)
  private                     Person   person;

  public Address() {
  }

  public Address(String[] lines, String city, String province, String postalCode, Person person) {
    this.lines = lines;
    this.city = city;
    this.province = province;
    this.postalCode = postalCode;
    this.person = person;
  }

  public Long getId() {
    return id;
  }

  public String[] getLines() {
    return lines;
  }

  public void setLines(String[] lines) {
    this.lines = lines;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getProvince() {
    return province;
  }

  public void setProvince(String province) {
    this.province = province;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }

  @Override public boolean equals(Object o) {
    if(!(o instanceof Address)) {
      return false;
    }

    Address address2 = (Address)o;
    return (address2.id == id || (id != null && id.equals(address2.id)));
  }

}
