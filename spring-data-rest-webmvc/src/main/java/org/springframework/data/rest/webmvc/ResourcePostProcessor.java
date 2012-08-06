package org.springframework.data.rest.webmvc;

import org.springframework.data.rest.core.PostProcessor;
import org.springframework.data.rest.core.Resource;

/**
 * Implementations of this interface are allowed to mutate a {@link Resource} being sent back to the client.
 *
 * @author Jon Brisbin
 */
public interface ResourcePostProcessor extends PostProcessor<Resource> {
}
