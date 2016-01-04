package org.springframework.data.rest.core.projection;

import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.core.config.Projection;

/**
 * ProjectionFactory that supports {@link Projection#subProjections()}
 * by selecting the first sub-projection that supports the projected source object
 * and using it instead of the requested projection.
 * A sub-projection must satisfy two criteria in order to be chosen: it must extend the requested projection
 * and it must have at least one of {@link Projection#types()} matching the source object.
 * This is done recursively, selecting a sub-projection of a sub-projection, if possible.
 */
public class SubTypeAwareProxyProjectionFactory extends SpelAwareProxyProjectionFactory {

	@Override
	public <T> T createProjection(Class<T> projection, Object source) {

		Class<? extends T> subProjection = projection;
		Projection annotation = projection.getAnnotation(Projection.class);
		if (annotation != null) {
			subProjection = getSubProjection(projection, source, annotation.subProjections());
		}
		return super.createProjection(subProjection, source);
	}

	private <T> Class<? extends T> getSubProjection(Class<T> projection, Object source, Class<?>[] candidates) {

		for (Class<?> candidate : candidates) {
			Projection annotation = candidate.getAnnotation(Projection.class);
			if (annotation != null
					&& projection.isAssignableFrom(candidate)
					&& isOneOf(source, annotation.types())) {
				// recursively find possible sub-projections of the sub-projection
				return getSubProjection(candidate.asSubclass(projection), source, annotation.subProjections());
			}
		}
		return projection;
	}

	private <T> boolean isOneOf(Object source, Class<?>[] types) {

		for (Class<?> type : types) {
			if (type.isInstance(source)) {
				return true;
			}
		}
		return false;
	}
}
