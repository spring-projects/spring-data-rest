package org.springframework.data.rest.core.invoke;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.util.Assert;

/**
 * Represents a query method on a repository interface.
 * 
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class RepositoryQueryMethod {

	private Method method;
	private Class<?>[] paramTypes;
	private String[] paramNames;

	public RepositoryQueryMethod(Method method) {
		this.method = method;
		paramTypes = method.getParameterTypes();
		paramNames = new String[paramTypes.length];
		if (null == paramNames) {
			paramNames = new String[paramTypes.length];
		}
		Annotation[][] paramAnnos = method.getParameterAnnotations();
		for (int i = 0; i < paramAnnos.length; i++) {
			if (paramAnnos[i].length == 0) {
				continue;
			}

			for (Annotation anno : paramAnnos[i]) {
				if (Param.class.isAssignableFrom(anno.getClass())) {
					Param p = (Param) anno;
					paramNames[i] = p.value();
					break;
				}
			}

			if (Pageable.class.isAssignableFrom(paramTypes[i]) || Sort.class.isAssignableFrom(paramTypes[i])) {
				continue;
			}

			Assert.notNull(paramNames[i], "No @Param('name') was provided for parameter " + (i + 1) + " of type "
					+ paramTypes[i] + " on " + (method.getDeclaringClass().getName() + "." + method.getName()));
		}
	}

	/**
	 * The method's parameter types.
	 * 
	 * @return
	 */
	public Class<?>[] paramTypes() {
		return paramTypes;
	}

	/**
	 * The parameter names as pulled from the {@link Param} annotations.
	 * 
	 * @return
	 */
	public String[] paramNames() {
		return paramNames;
	}

	/**
	 * The {@link Method} to invoke.
	 * 
	 * @return
	 */
	public Method method() {
		return method;
	}

}
