package org.springframework.data.rest.webmvc.support;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;

/**
 * @author Jon Brisbin
 */
public class JpaHelper implements BeanFactoryAware {

	private Object interceptor = null;

	@Override public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				(ListableBeanFactory)beanFactory,
				EntityManagerFactory.class
		);
		if(beanNames.length != 1) {
			throw new BeanInstantiationException(JpaHelper.class,
			                                     String.format("Only one EntityManagerFactory expected but %s found",
			                                                   beanNames.length));
		}
		String unitName = beanNames[0];
		EntityManagerFactory emf = (EntityManagerFactory)beanFactory.getBean(unitName);
		OpenEntityManagerInViewInterceptor omivi = new OpenEntityManagerInViewInterceptor();
		omivi.setEntityManagerFactory(emf);
		this.interceptor = omivi;
	}

	public Object getInterceptor() {
		return interceptor;
	}

}
