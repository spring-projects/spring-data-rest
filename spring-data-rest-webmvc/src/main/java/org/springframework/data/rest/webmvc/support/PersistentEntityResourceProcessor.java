package org.springframework.data.rest.webmvc.support;

import static org.springframework.util.ClassUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

/**
 * @author Jon Brisbin
 */
public class PersistentEntityResourceProcessor implements RepresentationModelProcessor<PersistentEntityResource> {

	private final List<DomainTypeResourceProcessor> resourceProcessors = new ArrayList<DomainTypeResourceProcessor>();

	@Autowired
	public PersistentEntityResourceProcessor(Repositories repositories,
			List<RepresentationModelProcessor<EntityModel<?>>> resourceProcessors) {
		if (null != resourceProcessors) {
			for (RepresentationModelProcessor<EntityModel<?>> rp : resourceProcessors) {
				TypeInformation<?> typeInfo = TypeInformation.of(rp.getClass());
				TypeInformation<?> domainType = typeInfo.getTypeArguments().get(0);
				if (null != repositories.getPersistentEntity(domainType.getType())) {
					this.resourceProcessors.add(new DomainTypeResourceProcessor(domainType.getType(), rp));
				}
			}
		}
	}

	@Override
	public PersistentEntityResource process(PersistentEntityResource resource) {

		Object content = resource.getContent();

		if (null == content) {
			return resource;
		}

		Class<?> domainType = content.getClass();
		for (DomainTypeResourceProcessor rp : resourceProcessors) {
			if (isAssignable(domainType, rp.domainType)) {
				rp.resourceProcessor.process(resource);
			}
		}

		return resource;
	}

	private static class DomainTypeResourceProcessor {
		final Class<?> domainType;
		final RepresentationModelProcessor<EntityModel<?>> resourceProcessor;

		private DomainTypeResourceProcessor(Class<?> domainType, RepresentationModelProcessor<EntityModel<?>> resourceProcessor) {
			this.domainType = domainType;
			this.resourceProcessor = resourceProcessor;
		}
	}

}
