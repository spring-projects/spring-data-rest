package org.springframework.data.rest.webmvc;

import org.springframework.data.rest.core.PostProcessor;
import org.springframework.data.rest.core.ResourceSet;

/**
 * Implementations of this interface are allowed to mutate the {@link org.springframework.data.rest.core.ResourceSet}
 * object that is being sent back to the client as a response.
 *
 * @author Jon Brisbin
 */
public interface ResponsePostProcessor extends PostProcessor<ResourceSet> {
}
