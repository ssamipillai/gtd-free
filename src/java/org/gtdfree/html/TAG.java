/*
 *    Copyright (C) 2008-2010 Igor Kriznar
 *    
 *    This file is part of GTD-Free.
 *    
 *    GTD-Free is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *    
 *    GTD-Free is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 *    You should have received a copy of the GNU General Public License
 *    along with GTD-Free.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gtdfree.html;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author ikesan
 *
 */
public final class TAG implements Cloneable, Iterable<Attribute> {
	
	public static final TAG DIV= new TAG("DIV"); //$NON-NLS-1$
	public static final TAG TR= new TAG("TR"); //$NON-NLS-1$
	public static final TAG TD= new TAG("TD"); //$NON-NLS-1$
	public static final TAG TH= new TAG("TH"); //$NON-NLS-1$
	public static final TAG A = new TAG("A"); //$NON-NLS-1$
	public static final TAG BR = new TAG("BR"); //$NON-NLS-1$
	public static final TAG TABLE = new TAG("TABLE"); //$NON-NLS-1$
	public static final TAG STRONG = new TAG("STRONG"); //$NON-NLS-1$
	public static final TAG P = new TAG("P"); //$NON-NLS-1$
	public static final TAG H1 = new TAG("H1"); //$NON-NLS-1$
	public static final TAG H2 = new TAG("H2"); //$NON-NLS-1$
	public static final TAG H3 = new TAG("H3"); //$NON-NLS-1$
	public static final TAG H4 = new TAG("H4"); //$NON-NLS-1$
	public static final TAG HTML = new TAG("HTML"); //$NON-NLS-1$
	public static final TAG BODY = new TAG("BODY"); //$NON-NLS-1$
	public static final TAG SPAN = new TAG("SPAN"); //$NON-NLS-1$
	public static final TAG STYLE = new TAG("STYLE").withAttribute(Attribute.TYPE, "text/css"); //$NON-NLS-1$ //$NON-NLS-2$

	public static TAG withName(String name) {
		return new TAG(name);
	}
	
	private String name;
	private HashMap<Attribute, String> attributes;
	
	private TAG(String name) {
		this.name=name;
	}
	
	private Map<Attribute, String> getAttr() {
		if (attributes == null) {
			attributes = new HashMap<Attribute, String>(10);
		}
		return attributes;
	}
	
	public String getName() {
		return name;
	}
	
	public String getCLASS() {
		return getAttr().get(Attribute.CLASS);
	}
	
	public String getID() {
		return getAttr().get(Attribute.ID);
	}
	
	private TAG copy() {
		try {
			return (TAG)clone();
		} catch (CloneNotSupportedException e) {
			// not happening
		};
		return null;
	}
	
	
	public TAG withAttribute(Attribute a, String val) {
		TAG t= copy();
		t.getAttr().put(a, val);
		return t;
	}
	
	public TAG withClass(String val) {
		return withAttribute(Attribute.CLASS, val);
	}
	
	public TAG withROWSPAN(int val) {
		return withAttribute(Attribute.ROWSPAN, String.valueOf(val));
	}

	public TAG withCOLSPAN(int val) {
		return withAttribute(Attribute.COLSPAN, String.valueOf(val));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<Attribute> iterator() {
		if (attributes!=null) {
			return attributes.keySet().iterator();
		}
		return Collections.EMPTY_LIST.iterator();
	};
	
	public String getAttribute(Attribute a) {
		if (attributes!=null) {
			return attributes.get(a);
		}
		return null;
	}

	public TAG withHREF(URL url) {
		return withAttribute(Attribute.HREF, url.toString());
	}

	public int getAttributeCount() {
		if (attributes==null) {
			return 0;
		}
		return attributes.size();
	}
	
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(8);
		printTag(sb);
		return sb.toString();
	}
	
	public void printTag(StringBuilder sb) {
		sb.append('<');
		sb.append(name);
		for (Attribute att : this) {
			sb.append(' ');
			sb.append(att.name());
			sb.append('=');
			sb.append('"');
			sb.append(getAttribute(att));
			sb.append('"');
		}
		sb.append('>');
	}
	
	public void printTagEnd(StringBuilder sb) {
		sb.append('<');
		sb.append('/');
		sb.append(name);
		sb.append('>');
	}

}
