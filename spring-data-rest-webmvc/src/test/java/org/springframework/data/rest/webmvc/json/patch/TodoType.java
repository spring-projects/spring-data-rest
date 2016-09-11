package org.springframework.data.rest.webmvc.json.patch;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Mathias Düsterhöft
 */
@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TodoType {
    private String value = "none";
}
