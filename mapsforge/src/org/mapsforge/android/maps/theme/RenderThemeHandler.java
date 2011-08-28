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

import java.io.IOException;
import java.util.Stack;

import org.mapsforge.android.maps.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX2 handler class to parse render theme XML files.
 */
public class RenderThemeHandler extends DefaultHandler {
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
	private int level;
	private RenderTheme renderTheme;

	private final Stack<Rule> rulesStack = new Stack<Rule>();

	@Override
	public void endDocument() {
		this.renderTheme.setLevels(this.level);
		this.renderTheme.complete();
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if ("rule".equals(localName)) {
			this.rulesStack.pop();
			if (this.rulesStack.isEmpty()) {
				this.renderTheme.addRule(this.currentRule);
				this.currentRule = null;
			} else {
				this.currentRule = this.rulesStack.peek();
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
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		try {
			if ("rules".equals(localName)) {
				this.renderTheme = RenderTheme.create(localName, attributes);
			}

			else if ("rule".equals(localName)) {
				Rule rule = Rule.create(localName, attributes);
				if (this.currentRule != null) {
					this.currentRule.addSubRule(rule);
				}
				this.currentRule = rule;
				this.rulesStack.push(this.currentRule);
			}

			else if ("area".equals(localName)) {
				Area area = Area.create(localName, attributes, this.level++);
				this.currentRule.addRenderingInstruction(area);
			}

			else if ("caption".equals(localName)) {
				Caption caption = Caption.create(localName, attributes);
				this.currentRule.addRenderingInstruction(caption);
			}

			else if ("circle".equals(localName)) {
				Circle circle = Circle.create(localName, attributes, this.level++);
				this.currentRule.addRenderingInstruction(circle);
			}

			else if ("line".equals(localName)) {
				Line line = Line.create(localName, attributes, this.level++);
				this.currentRule.addRenderingInstruction(line);
			}

			else if ("lineSymbol".equals(localName)) {
				LineSymbol lineSymbol = LineSymbol.create(localName, attributes);
				this.currentRule.addRenderingInstruction(lineSymbol);
			}

			else if ("pathText".equals(localName)) {
				PathText pathText = PathText.create(localName, attributes);
				this.currentRule.addRenderingInstruction(pathText);
			}

			else if ("symbol".equals(localName)) {
				Symbol symbol = Symbol.create(localName, attributes);
				this.currentRule.addRenderingInstruction(symbol);
			}

			else {
				Logger.debug("unknown element:" + localName);
			}
		} catch (IOException e) {
			Logger.exception(e);
		} catch (IllegalArgumentException e) {
			Logger.exception(e);
		}
	}

	@Override
	public void warning(SAXParseException e) {
		Logger.exception(e);
	}
}