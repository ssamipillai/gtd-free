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

package org.gtdfree.journal;

import java.util.EventObject;

/**
 * @author ikesan
 *
 */
public class JournalEntryEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	private JournalEntry journalEntry;
	private int index;
	private String property;
	private Object oldValue;
	private Object newValue; 
	
	/**
	 * @param source
	 * @param index
	 * @param newValue
	 * @param oldValue
	 * @param property
	 * @param journalEntry
	 */
	public JournalEntryEvent(JournalEntry journalEntry, String property, Object newValue,
			Object oldValue, int index) {
		super(journalEntry);
		this.index = index;
		this.newValue = newValue;
		this.oldValue = oldValue;
		this.property = property;
		this.journalEntry = journalEntry;
	}

	/**
	 * @return the journalEntry
	 */
	public JournalEntry getJournalEntry() {
		return journalEntry;
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the property
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * @return the oldValue
	 */
	public Object getOldValue() {
		return oldValue;
	}

	/**
	 * @return the newValue
	 */
	public Object getNewValue() {
		return newValue;
	}

}
