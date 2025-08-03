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

import java.util.Iterator;

/**
 * @author ikesan
 *
 */
public final class Structure implements Iterable<Structure> {

	public static enum ElementType {TAG_START,TAG_END,TAG,TEXT};
	
	public static final Structure withStart(TAG tag) {
		return new Structure(tag,ElementType.TAG_START);
	}

	public static final Structure with(TAG tag) {
		return new Structure(tag,ElementType.TAG);
	}

	public static final Structure with(String text) {
		return new Structure(text);
	}

	public static final Structure withEnd() {
		return new Structure(ElementType.TAG_END);
	}

	public static final Structure with(Structure s) {
		if (s.getType()==ElementType.TAG_END) {
			return withEnd().add(s.next());
		} 
		if (s.getType()==ElementType.TAG_START) {
			return withStart(s.getTag()).add(s.next());
		} 
		if (s.getType()==ElementType.TAG) {
			return with(s.getTag()).add(s.next());
		} 
		if (s.getType()==ElementType.TEXT) {
			return with(s.getText()).add(s.next());
		}
		return null;
	}

	private Structure next;
	private Structure last;
	private ElementType type;
	private TAG tag;
	private String text;
	
	private Structure(TAG t, ElementType type) {
		tag=t;
		this.type=type;
	}

	private Structure(String t) {
		text=t;
		type=ElementType.TEXT;
	}

	private Structure(ElementType t) {
		type=t;
	}
	
	private Structure addLast(Structure s) {
		if (last!=null) {
			return last= last.addLast(s);
		} 
		if (next!=null) {
			return last= next.addLast(s);
		}
		return last= next= s;
	}
	
	public Structure add(Structure s) {
		if (s==null) {
			return this;
		}
		for (Structure st : s) {
			if (st.getType()==ElementType.TAG_END) {
				addEnd();
			} else if (st.getType()==ElementType.TAG_START) {
				addStart(st.getTag());
			} else if (st.getType()==ElementType.TAG) {
				add(st.getTag());
			} else if (st.getType()==ElementType.TEXT) {
				add(st.getText());
			}
		}
		return this;
	}

	public boolean hasNext() {
		return next!=null;
	}
	
	public Structure next() {
		return next;
	}

	public ElementType getType() {
		return type;
	}
	
	public TAG getTag() {
		return tag;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String t) {
		if (type==ElementType.TEXT) {
			text=t;
		} else if (next!=null) {
			next.setText(t);
		}
	}
	
	public Structure addStart(TAG tag) {
		addLast(new Structure(tag,ElementType.TAG_START));
		return this;
	}
	
	public Structure add(TAG tag) {
		addLast(new Structure(tag,ElementType.TAG));
		return this;
	}

	public Structure add(String text) {
		addLast(new Structure(text));
		return this;
	}

	public Structure addEnd() {
		addLast(new Structure(ElementType.TAG_END));
		return this;
	}
	
	@Override
	public Iterator<Structure> iterator() {
		return new Iterator<Structure>() {
		
			Structure s= Structure.this;
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		
			@Override
			public Structure next() {
				Structure n= s;
				s=s.next;
				return n;
			}
		
			@Override
			public boolean hasNext() {
				return s!=null;
			}
		};
	}
	
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		for (Structure s : this) {
			if (s.getType()==ElementType.TAG) {
				sb.append(s.getTag().toString());
				sb.insert(sb.length()-1, '/');
			} else if (s.getType()==ElementType.TAG_START) {
				sb.append(s.getTag().toString());
			} else if (s.getType()==ElementType.TAG_END) {
				sb.append('<');
				sb.append('/');
				sb.append('>');
			} else if (s.getType()==ElementType.TEXT) {
				sb.append(s.getText());
			}
		}
		return sb.toString();
	}
	
}
