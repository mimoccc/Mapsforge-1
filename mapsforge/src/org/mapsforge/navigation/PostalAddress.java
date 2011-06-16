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
/**
 * 
 */
package org.mapsforge.navigation;

/**
 * Represents a postal address.
 * 
 * @author thilo ratnaweera
 * 
 */
public class PostalAddress {

	private String city;
	private String country;
	private String postalCode;
	private String stateOrProvince;
	private String street;
	private String streetNumber;

	/**
	 * @return The city of this postal address.
	 */
	public String getCity() {
		return city;
	}

	/**
	 * @param city
	 *            The city of this postal address.
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * @return The country of this postal address.
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country
	 *            The country of this postal address.
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return The postal code (zip code) of this postal address.
	 */
	public String getPostalCode() {
		return postalCode;
	}

	/**
	 * @param postalCode
	 *            The postal code (zip code) of this postal address.
	 */
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	/**
	 * @return The state or province of this postal address.
	 */
	public String getStateOrProvince() {
		return stateOrProvince;
	}

	/**
	 * @param stateOrProvince
	 *            The state or province of this postal address.
	 */
	public void setStateOrProvince(String stateOrProvince) {
		this.stateOrProvince = stateOrProvince;
	}

	/**
	 * @return The street of this postal address.
	 */
	public String getStreet() {
		return street;
	}

	/**
	 * @param street
	 *            The street of this postal address.
	 */
	public void setStreet(String street) {
		this.street = street;
	}

	/**
	 * @return The street number of this postal address.
	 */
	public String getStreetNumber() {
		return streetNumber;
	}

	/**
	 * @param streetNumber
	 *            The street number of this postal address.
	 */
	public void setStreetNumber(String streetNumber) {
		this.streetNumber = streetNumber;
	}

}
