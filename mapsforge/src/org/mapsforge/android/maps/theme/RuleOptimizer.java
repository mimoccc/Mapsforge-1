/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.theme;

import java.util.Stack;

import org.mapsforge.android.maps.Logger;

final class RuleOptimizer {
	private static AttributeMatcher optimizeKeyMatcher(KeyMatcher keyMatcher, Stack<Rule> ruleStack) {
		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i) instanceof PositiveRule) {
				PositiveRule positiveRule = (PositiveRule) ruleStack.get(i);
				if (positiveRule.keyMatcher.isCoveredBy(keyMatcher)) {
					return AnyMatcher.getInstance();
				}
			}
		}

		return keyMatcher;
	}

	private static AttributeMatcher optimizeValueMatcher(ValueMatcher valueMatcher,
			Stack<Rule> ruleStack) {
		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i) instanceof PositiveRule) {
				PositiveRule positiveRule = (PositiveRule) ruleStack.get(i);
				if (positiveRule.valueMatcher.isCoveredBy(valueMatcher)) {
					return AnyMatcher.getInstance();
				}
			}
		}

		return valueMatcher;
	}

	static AttributeMatcher optimize(AttributeMatcher attributeMatcher, Stack<Rule> ruleStack) {
		if (attributeMatcher instanceof AnyMatcher || attributeMatcher instanceof NegativeMatcher) {
			return attributeMatcher;
		} else if (attributeMatcher instanceof KeyMatcher) {
			return optimizeKeyMatcher((KeyMatcher) attributeMatcher, ruleStack);
		} else if (attributeMatcher instanceof ValueMatcher) {
			return optimizeValueMatcher((ValueMatcher) attributeMatcher, ruleStack);
		}
		throw new IllegalArgumentException("unknown AttributeMatcher: " + attributeMatcher);
	}

	static ClosedMatcher optimize(ClosedMatcher closedMatcher, Stack<Rule> ruleStack) {
		if (closedMatcher instanceof AnyMatcher) {
			return closedMatcher;
		}

		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i).closedMatcher.isCoveredBy(closedMatcher)) {
				return AnyMatcher.getInstance();
			} else if (!closedMatcher.isCoveredBy(ruleStack.get(i).closedMatcher)) {
				Logger.debug("Warning: unreachable rule (closed)");
			}
		}

		return closedMatcher;
	}

	static ElementMatcher optimize(ElementMatcher elementMatcher, Stack<Rule> ruleStack) {
		if (elementMatcher instanceof AnyMatcher) {
			return elementMatcher;
		}

		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i).elementMatcher.isCoveredBy(elementMatcher)) {
				return AnyMatcher.getInstance();
			} else if (!elementMatcher.isCoveredBy(ruleStack.get(i).elementMatcher)) {
				Logger.debug("Warning: unreachable rule (e)");
			}
		}

		return elementMatcher;
	}

	/**
	 * Private constructor to prevent instantiation from other classes.
	 */
	private RuleOptimizer() {
		// do nothing
	}
}
