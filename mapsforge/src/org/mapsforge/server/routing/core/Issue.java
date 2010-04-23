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

/** package private */
public enum Issue {
	ASSERTION_ERR, CALLBACK_TYPE_ASSERTION, CONNECTION_NOT_SET_PROPPERLY, DATAINPUT_FILE_NOT_FOUND_EXC, DATAINPUT_NOT_SET, DB_CON_SETUP_EXC, DB_CON_SHUTDOWN_EXC, DB_CONNECTION_ERROR, DB_DRIVER_NOT_INSTANTIABLE_EXC, DEFAULTS_NOT_ACCESSIBLE, FACTORY_CLASS_NO_XFACTORY_IMPL_EXC, FACTORY_CLASS_NOT_INSTANTIABLE_EXC, FACTORY_METHOD_HAS_WRONG_RETURN_TYPE_EXC, FACTORY_METHOD_NOT_ACCESSIBLE_EXC, FACTORY_METHOD_NOT_EXISTING_EXC, GM__BAD_DATAINPUT, GM__BAD_DATAOUTPUT, GM__CONSTRUCTOR_NOT_APPLICABLE, GM__DATAINPUT_NOT_CONFGD_PRPLY, GM__DATAOUTPUT_NOT_CONFGD_PRPLY, GM__DATE_NOT_CONFGD_PRPLY, GM__GEOMAP_INSTANTIATION, GM__NAME_NOT_CONFGD, R__CONSTRUCTOR_PROBLEM_EXC, R__ROUTER_NOT_INSTANTIABLE, R__ROUTINGGRAPH_NOT_APPROPRIATE, RG__BAD_DATAINPUT, RG__BAD_DATAOUTPUT, RG__DATAINPUT_NOT_CONFGD_PRPLY, RG__DATAOUTPUT_NOT_CONFGD_PRPLY, RG__ROUTINGGRAPH_INSTANTIATION, RG__VALUEOF_NOT_APPLICABLE, UNMODIFIABLE_EXC, V__NOT_EXISTENT, VERTEXID_OUTOFRANGE, ;
	public final String msg() {
		return Messages.getString(Issue.class.getCanonicalName() + "." + this.name()); //$NON-NLS-1$
	}
}
