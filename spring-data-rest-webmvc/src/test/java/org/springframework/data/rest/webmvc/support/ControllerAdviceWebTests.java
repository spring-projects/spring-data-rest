package org.springframework.data.rest.webmvc.support;

import org.junit.Test;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.hateoas.Link;
import org.springframework.test.context.ContextConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thibaud Lepretre
 */
@ContextConfiguration(classes = ControllerAdviceConfig.class)
public class ControllerAdviceWebTests extends AbstractWebIntegrationTests {
	@Test
	public void httpRequestMethodNotSupportedExceptionShouldNowReturnHttpStatus500Over405() throws Exception {

		Link link = client.discoverUnique("addresses");

		mvc.perform(get(link.getHref())).//
				andExpect(status().isInternalServerError());
	}

	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return null;
	}
}
