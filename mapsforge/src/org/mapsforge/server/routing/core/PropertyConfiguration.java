/*
 * Copyright 2010 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.server.routing.core;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class PropertyConfiguration<E extends Enum<E> & IProperty> implements
		Map<E, String> {

	private static enum Issue {
		ASSERTION_ERR, UNMODIFIABLE_EXC;
		public final String msg() {
			return Messages.getString(PropertyConfiguration.class.getCanonicalName() + '.'
					+ this.name());
		}
	}

	public static <E extends Enum<E> & IProperty> String keyString(E key) {
		return key.getClass().getCanonicalName() + '.' + key.name();
	}

	public static <E extends Enum<E> & IProperty> PropertyConfiguration<E> newInstance(
			Properties props, Class<E> enumType) {
		Properties newProps = new Properties();
		newProps.putAll(props);
		return new PropertyConfiguration<E>(newProps, enumType);
	}

	public static <E extends Enum<E> & IProperty> PropertyConfiguration<E> newInstance(
			PropertyConfiguration<?> propConf, Class<E> enumType) {
		return new PropertyConfiguration<E>(propConf, enumType);
	}

	private final Properties props;

	private final Map<E, String> values;

	private PropertyConfiguration(Properties props, Class<E> enumType) {
		this.values = new EnumMap<E, String>(enumType);
		try {
			E[] keysArr = (E[]) enumType.getDeclaredMethod("values").invoke(null); //$NON-NLS-1$
			for (E key : keysArr)
				this.values.put(key, props.getProperty(keyString(key)));
		} catch (Exception exc) {
			throw new AssertionError(Issue.ASSERTION_ERR.msg());
		}
		this.props = props;
	}

	private PropertyConfiguration(PropertyConfiguration<?> propConf, Class<E> enumType) {
		this(propConf.props, enumType);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(Issue.UNMODIFIABLE_EXC.msg());
	}

	@Override
	public boolean containsKey(Object key) {
		return this.values.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.values.containsValue(value);
	}

	@Override
	public Set<Entry<E, String>> entrySet() {
		return this.values.entrySet();
	}

	@Override
	public String get(Object key) {
		return this.values.get(key);
	}

	@Override
	public boolean isEmpty() {
		return this.values.isEmpty();
	}

	@Override
	public Set<E> keySet() {
		return this.values.keySet();
	}

	/**
	 * Itemizes all the properties and it's default values specified by the enum used and
	 * provides them as a {@link Properties} object, ready for writing it out onto disk, etc.
	 * 
	 * @return the Properties containing all entries used.
	 */
	public Properties propertiesUsed() {
		Properties props = new Properties();
		for (Entry<E, String> entry : this.values.entrySet()) {
			props.setProperty(keyString(entry.getKey()), entry.getValue());
		}
		return props;
	}

	@Override
	public String put(E key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends E, ? extends String> t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return this.values.size();
	}

	@Override
	public Collection<String> values() {
		return this.values.values();
	}
}
