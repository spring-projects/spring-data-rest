package org.springframework.data.rest.webmvc.jpa;

import javax.persistence.Embeddable;

import org.springframework.hateoas.core.Relation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Mathias Düsterhöft
 * @see DATAREST-904
 */
@Embeddable
@Getter
@AllArgsConstructor
@Relation(collectionRelation = "country-list")
public class Country {
    private String countryCode;
    private String country;
}
