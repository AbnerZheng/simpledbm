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
 *    Linking this library statically or dynamically with other modules 
 *    is making a combined work based on this library. Thus, the terms and
 *    conditions of the GNU General Public License cover the whole
 *    combination.
 *    
 *    As a special exception, the copyright holders of this library give 
 *    you permission to link this library with independent modules to 
 *    produce an executable, regardless of the license terms of these 
 *    independent modules, and to copy and distribute the resulting 
 *    executable under terms of your choice, provided that you also meet, 
 *    for each linked independent module, the terms and conditions of the 
 *    license of that module.  An independent module is a module which 
 *    is not derived from or based on this library.  If you modify this 
 *    library, you may extend this exception to your version of the 
 *    library, but you are not obligated to do so.  If you do not wish 
 *    to do so, delete this exception statement from your version.
 *
 *    Project: www.simpledbm.org
 *    Author : Dibyendu Majumdar
 *    Email  : d dot majumdar at gmail dot com ignore
 */
package org.simpledbm.common.util;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.simpledbm.common.api.registry.Storable;

/**
 * A format for String objects that is easier to persist.
 * 
 * @author Dibyendu Majumdar
 * @since 6 Mar 2010
 */
public final class StorableString implements Storable,
        Comparable<StorableString> {

    private final char[] data;

    public StorableString() {
        data = new char[0];
    }

    public StorableString(String s) {
        data = s.toCharArray();
    }

    public StorableString(char[] charArray) {
        this.data = charArray.clone();
    }

    public StorableString(StorableString s) {
        this.data = s.data.clone();
    }

    public StorableString(ByteBuffer bb) {
        short n = bb.getShort();
        if (n > 0) {
            data = new char[n];
            bb.asCharBuffer().get(data);
            bb.position(bb.position() + n * TypeSize.CHARACTER);
        } else {
            data = new char[0];
        }
    }

    @Override
    public String toString() {
        return new String(data);
    }

    public int getStoredLength() {
        return data.length * TypeSize.CHARACTER + TypeSize.SHORT;
    }

    public void store(ByteBuffer bb) {
        short n = 0;
        if (data != null) {
            n = (short) data.length;
        }
        bb.putShort(n);
        if (n > 0) {
            bb.asCharBuffer().put(data);
            bb.position(bb.position() + n * TypeSize.CHARACTER);
        }
    }

    public int compareTo(StorableString o) {
        int len = (data.length <= o.data.length) ? data.length : o.data.length;
        for (int i = 0; i < len; i++) {
            int result = data[i] - o.data[i];
            if (result != 0) {
                return result;
            }
        }
        return data.length - o.data.length;
    }

    public int length() {
        return data.length;
    }

    public char get(int offset) {
        return data[offset];
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final StorableString other = (StorableString) obj;
        if (!Arrays.equals(data, other.data))
            return false;
        return true;
    }
}
