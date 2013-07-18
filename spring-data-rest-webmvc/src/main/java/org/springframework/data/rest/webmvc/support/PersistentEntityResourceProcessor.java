package org.springframework.data.rest.webmvc.support;

import static org.springframework.util.ClassUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

/**
 * @author Jon Brisbin
 */
public class PersistentEntityResourceProcessor implements ResourceProcessor<PersistentEntityResource<?>> {

	private final List<DomainTypeResourceProcessor> resourceProcessors = new ArrayList<DomainTypeResourceProcessor>();

	@Autowired
	public PersistentEntityResourceProcessor(Repositories repositories,
			List<ResourceProcessor<Resource<?>>> resourceProcessors) {
		if (null != resourceProcessors) {
			for (ResourceProcessor<Resource<?>> rp : resourceProcessors) {
				TypeInformation<?> typeInfo = ClassTypeInformation.from(rp.getClass());
				TypeInformation<?> domainType = typeInfo.getTypeArguments().get(0);
				if (null != repositories.getPersistentEntity(domainType.getType())) {
					this.resourceProcessors.add(new DomainTypeResourceProcessor(domainType.getType(), rp));
				}
			}
		}
	}

	@Override
	public PersistentEntityResource<?> process(PersistentEntityResource<?> resource) {
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
		final ResourceProcessor<Resource<?>> resourceProcessor;

		private DomainTypeResourceProcessor(Class<?> domainType, ResourceProcessor<Resource<?>> resourceProcessor) {
			this.domainType = domainType;
			this.resourceProcessor = resourceProcessor;
		}
	}

}
