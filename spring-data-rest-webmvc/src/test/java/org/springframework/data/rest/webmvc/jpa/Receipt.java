package org.springframework.data.rest.webmvc.jpa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * An entity that represents a receipt.
 *
 * @author Pablo Lozano
 */
@Entity
@Cacheable(value = false)
@JsonIgnoreProperties({"version"})
public class Receipt {

    @Id
    @GeneratedValue
    private Long id;

    private String saleItem;

    private BigDecimal amount;

    @Version
    @Temporal(TemporalType.TIMESTAMP)
    private Date version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSaleItem() {
        return saleItem;
    }

    public void setSaleItem(String saleItem) {
        this.saleItem = saleItem;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    public Date getVersion() {
        return version;
    }

    public void setVersion(Date version) {
        this.version = version;
    }
}