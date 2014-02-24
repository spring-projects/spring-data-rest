/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.projection;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link MethodInterceptor} to invoke a SpEL expression to compute the method result. Will forward the resolution to a
 * delegate {@link MethodInterceptor} if no {@link Value} annotation is found.
 * 
 * @author Oliver Gierke
 */
class SpelEvaluatingMethodInterceptor implements MethodInterceptor {

	private final SpelExpressionParser parser;
	private final ParserContext parserContext;
	private final EvaluationContext evaluationContext;
	private final MethodInterceptor delegate;

	/**
	 * Creates a new {@link SpelEvaluatingMethodInterceptor} delegating to the given {@link MethodInterceptor} as fallback
	 * and exposing the given target object via {@code target} to the SpEl expressions. If a {@link BeanFactory} is given,
	 * bean references in SpEl expressions can be resolved as well.
	 * 
	 * @param delegate must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param beanFactory can be {@literal null}.
	 */
	public SpelEvaluatingMethodInterceptor(MethodInterceptor delegate, Object target, BeanFactory beanFactory) {

		Assert.notNull(delegate, "Delegate MethodInterceptor must not be null!");
		Assert.notNull(target, "TargetObject must not be null!");

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new TargetWrapper(target));

		if (beanFactory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}

		this.evaluationContext = evaluationContext;
		this.parser = new SpelExpressionParser();
		this.parserContext = new TemplateParserContext();
		this.delegate = delegate;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();
		Value annotation = method.getAnnotation(Value.class);

		if (annotation == null || !StringUtils.hasText(annotation.value())) {
			return delegate.invoke(invocation);
		}

		Expression expression = parser.parseExpression(annotation.value(), parserContext);
		return expression.getValue(evaluationContext);
	}

	/**
	 * Wrapper class to expose an object to the SpEL expression as {@code target}.
	 * 
	 * @author Oliver Gierke
	 */
	static class TargetWrapper {

		private final Object target;

		public TargetWrapper(Object target) {
			this.target = target;
		}

		/**
		 * @return the target
		 */
		public Object getTarget() {
			return target;
		}
	}
}
