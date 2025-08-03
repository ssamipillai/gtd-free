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

package org.gtdfree.model;


/**
 * @author ikesan
 *
 */
public class RemindFilter implements ActionFilter {

	private boolean past;
	private long start;
	private long end;
	
	public RemindFilter(long start, long end) {
		
		this.start=start;
		this.end= end;
		past=false;
	}
	
	public RemindFilter(long end, boolean past) {
		this.end=end;
		this.past=past;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ActionFilter#isAccepted(org.gtdfree.model.Folder, org.gtdfree.model.Action)
	 */
	@Override
	public boolean isAcceptable(Folder f, Action a) {
		if (a==null || a.getRemind()==null) {
			return false;
		}
		
		long test= a.getRemind().getTime();

		if (past) {
			return end>test;
		} 
		
		
		return start<=test && test<end;
		
	}

}
