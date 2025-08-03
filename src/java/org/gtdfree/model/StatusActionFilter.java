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
public class StatusActionFilter implements ActionFilter {

	private boolean includeResolved;
	private boolean includeDeleted;
	
	
	/**
	 * @param folders
	 * @param projects
	 * @param nullProjectAccepted
	 */
	public StatusActionFilter(boolean resolved, boolean deleted) {
		super();
		this.includeResolved= resolved;
		this.includeDeleted= deleted;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ActionFilter#isAcceptable(org.gtdfree.model.Folder, org.gtdfree.model.Action)
	 */
	@Override
	public boolean isAcceptable(Folder f, Action a) {
		if (a==null) {
			return true;
		}
		if (a.isDeleted() && !includeDeleted) {
			return false;
		}
		if (a.isResolved() && !includeResolved) {
			return false;
		}
		return true;
	}

}
