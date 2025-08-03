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


/**
 * @author ikesan
 *
 */
public class Interval implements Comparable<Interval> {

	private JournalEntry entry;
	private int start;
	private int end;
	
	/**
	 * Construct a time interval.
	 * 
	 * @param st Number of seconds since start of day at the start of the interval.
	 * @param en Number of seconds since start of day at the end of the interval. Might be larger than 24*60*60 if the interval ends in a subsequent day.
	 */
	public Interval(JournalEntry e, int st, int en) {
		this.entry = e;
		start=st;
		end=en;
	}
	
	@Override
	public int compareTo(Interval o) {
		if (o == null ) {
			return 0;
		}
		int diff= start-o.start;
		
		if (diff==0) {
			return end-o.end;
		}
		return diff;
	}

	/**
	 * @return the start
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @return the end
	 */
	public int getEnd() {
		return end;
	}

	public void setStart(int start) {
		this.start = start;
		entry.intervalChanged(this);
	}

	public void setEnd(int end) {
		this.end = end;
		entry.intervalChanged(this);
	}
}
