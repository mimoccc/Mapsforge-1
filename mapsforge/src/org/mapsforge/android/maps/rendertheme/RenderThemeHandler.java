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
package org.mapsforge.android.maps.rendertheme;

import java.io.IOException;
import java.util.Stack;

import org.mapsforge.android.maps.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX2 handler to parse XML render theme files.
 */
public class RenderThemeHandler extends DefaultHandler {
	private static enum Element {
		RENDERING_INSTRUCTION, RULE, RULES;
	}

	static void logUnknownAttribute(String element, String name, String value, int attributeIndex) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("unknown attribute in element ");
		stringBuilder.append(element);
		stringBuilder.append(" (");
		stringBuilder.append(attributeIndex);
		stringBuilder.append("): ");
		stringBuilder.append(name);
		stringBuilder.append('=');
		stringBuilder.append(value);
		Logger.debug(stringBuilder.toString());
	}

	private Rule currentRule;
	private final Stack<Element> elementStack = new Stack<Element>();
	private int level;
	private RenderTheme renderTheme;
	private final Stack<Rule> ruleStack = new Stack<Rule>();

	@Override
	public void endDocument() {
		if (this.renderTheme == null) {
			throw new IllegalArgumentException("missing element: rules");
		}

		this.renderTheme.setLevels(this.level);
		this.renderTheme.complete();
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		this.elementStack.pop();

		if ("rule".equals(localName)) {
			this.ruleStack.pop();
			if (this.ruleStack.empty()) {
				this.renderTheme.addRule(this.currentRule);
				this.currentRule = null;
			} else {
				this.currentRule = this.ruleStack.peek();
			}
		}
	}

	@Override
	public void error(SAXParseException e) {
		Logger.exception(e);
	}

	/**
	 * Returns the RenderTheme which has been parsed from the XML file.
	 * 
	 * @return the RenderTheme.
	 */
	public RenderTheme getRenderTheme() {
		return this.renderTheme;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
		try {
			if ("rules".equals(localName)) {
				checkState(localName, Element.RULES);
				this.renderTheme = RenderTheme.create(localName, attributes);
			}

			else if ("rule".equals(localName)) {
				checkState(localName, Element.RULE);
				Rule rule = Rule.create(localName, attributes, this.ruleStack);
				if (this.currentRule != null) {
					this.currentRule.addSubRule(rule);
				}
				this.currentRule = rule;
				this.ruleStack.push(this.currentRule);
			}

			else if ("area".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Area area = Area.create(localName, attributes, this.level++);
				this.ruleStack.peek().addRenderingInstruction(area);
			}

			else if ("caption".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Caption caption = Caption.create(localName, attributes);
				this.currentRule.addRenderingInstruction(caption);
			}

			else if ("circle".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Circle circle = Circle.create(localName, attributes, this.level++);
				this.currentRule.addRenderingInstruction(circle);
			}

			else if ("line".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Line line = Line.create(localName, attributes, this.level++);
				this.currentRule.addRenderingInstruction(line);
			}

			else if ("lineSymbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				LineSymbol lineSymbol = LineSymbol.create(localName, attributes);
				this.currentRule.addRenderingInstruction(lineSymbol);
			}

			else if ("pathText".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				PathText pathText = PathText.create(localName, attributes);
				this.currentRule.addRenderingInstruction(pathText);
			}

			else if ("symbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Symbol symbol = Symbol.create(localName, attributes);
				this.currentRule.addRenderingInstruction(symbol);
			}

			else {
				throw new SAXException("unknown element: " + localName);
			}
		} catch (IllegalArgumentException e) {
			throw new SAXException(null, e);
		} catch (IOException e) {
			throw new SAXException(null, e);
		}
	}

	@Override
	public void warning(SAXParseException e) {
		Logger.exception(e);
	}

	private void checkState(String elementName, Element element) throws SAXException {
		switch (element) {
			case RULES:
				if (!this.elementStack.empty()) {
					throw new SAXException("unexpected element: " + elementName);
				}
				break;
			case RULE:
				Element parentElement = this.elementStack.peek();
				if (parentElement != Element.RULES && parentElement != Element.RULE) {
					throw new SAXException("unexpected element: " + elementName);
				}
				break;
			case RENDERING_INSTRUCTION:
				if (this.elementStack.peek() != Element.RULE) {
					throw new SAXException("unexpected element: " + elementName);
				}
				break;
			default:
				throw new SAXException("unknown enum value: " + element);
		}

		this.elementStack.push(element);
	}
}