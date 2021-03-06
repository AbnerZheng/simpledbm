/***
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *    Project: www.simpledbm.org
 *    Author : Dibyendu Majumdar
 *    Email  : d dot majumdar at gmail dot com ignore
 */
package org.simpledbm.integrationtests.btree;

import java.nio.ByteBuffer;

import org.simpledbm.common.api.registry.ObjectFactory;
import org.simpledbm.rss.api.loc.Location;
import org.simpledbm.rss.api.loc.LocationFactory;

public class RowLocationFactory implements LocationFactory, ObjectFactory {
	public Location newLocation() {
		return new RowLocation();
	}
	public Location newLocation(ByteBuffer buf) {
		return new RowLocation(buf);
	}
	public Class<?> getType() {
		return RowLocation.class;
	}
	public Object newInstance(ByteBuffer buf) {
		return newLocation(buf);
	}
	public Location newLocation(String s) {
		return new RowLocation(s);
	}
}