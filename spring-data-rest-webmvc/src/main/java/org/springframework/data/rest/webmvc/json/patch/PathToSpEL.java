/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.rest.webmvc.json.patch;

import java.util.Arrays;
import java.util.List;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Utilities for converting patch paths to/from SpEL expressions. For example, "/foo/bars/1/baz" becomes
 * "foo.bars[1].baz".
 * 
 * @author Craig Walls
 * @author Oliver Gierke
 */
public class PathToSpEL {

	private static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();
	static final List<String> APPEND_CHARACTERS = Arrays.asList("-", "~");

	/**
	 * Converts a patch path to an {@link Expression}.
	 * 
	 * @param path the patch path to convert.
	 * @return an {@link Expression}
	 */
	public static Expression pathToExpression(String path) {
		return SPEL_EXPRESSION_PARSER.parseExpression(pathToSpEL(path));
	}

	/**
	 * Convenience method to convert a SpEL String to an {@link Expression}.
	 * 
	 * @param spel the SpEL expression as a String
	 * @return an {@link Expression}
	 */
	public static Expression spelToExpression(String spel) {
		return SPEL_EXPRESSION_PARSER.parseExpression(spel);
	}

	/**
	 * Produces an expression targeting the parent of the object that the given path targets.
	 * 
	 * @param path the path to find a parent expression for.
	 * @return an {@link Expression} targeting the parent of the object specified by path.
	 */
	public static Expression pathToParentExpression(String path) {
		return spelToExpression(pathNodesToSpEL(copyOf(path.split("\\/"), path.split("\\/").length - 1)));
	}

	private static String pathToSpEL(String path) {
		return pathNodesToSpEL(path.split("\\/"));
	}

	private static String pathNodesToSpEL(String[] pathNodes) {
		StringBuilder spelBuilder = new StringBuilder();

		for (int i = 0; i < pathNodes.length; i++) {

			String pathNode = pathNodes[i];

			if (pathNode.length() == 0) {
				continue;
			}

			if (APPEND_CHARACTERS.contains(pathNode)) {

				if (spelBuilder.length() > 0) {
					spelBuilder.append(".");
				}

				spelBuilder.append("$[true]");
				continue;
			}

			try {

				int index = Integer.parseInt(pathNode);
				spelBuilder.append('[').append(index).append(']');

			} catch (NumberFormatException e) {

				if (spelBuilder.length() > 0) {
					spelBuilder.append('.');
				}

				spelBuilder.append(pathNode);
			}
		}

		String spel = spelBuilder.toString();

		if (spel.length() == 0) {
			spel = "#this";
		}

		return spel;
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] copyOf(T[] original, int newLength) {
		return (T[]) Arrays.copyOf(original, newLength, original.getClass());
	}
}
