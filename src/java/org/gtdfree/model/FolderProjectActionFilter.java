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

import java.util.Set;

/**
 * @author ikesan
 *
 */
public class FolderProjectActionFilter implements ActionFilter {

	private Set<Integer> folders;
	private Set<Integer> projects;
	private ActionFilter filter;
	private boolean includeWithoutProject;
	
	
	/**
	 * @param folders
	 * @param projects
	 * @param nullProjectAccepted
	 */
	public FolderProjectActionFilter(Set<Integer> folders,
			Set<Integer> projects, ActionFilter f, boolean includeWithoutProject) {
		super();
		this.folders = folders;
		this.projects = projects;
		this.filter=f;
		this.includeWithoutProject=includeWithoutProject;
		
		if (filter==null) {
			filter= new DummyFilter(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ActionFilter#isAcceptable(org.gtdfree.model.Folder, org.gtdfree.model.Action)
	 */
	@Override
	public boolean isAcceptable(Folder f, Action a) {
		if (f.isProject()) {
			if (!projects.contains(f.getId())) {
				return false;
			}
		} else if (!folders.contains(f.getId())) {
			return false;
		}
		if (a==null) {
			return filter.isAcceptable(f, null);
		}
		if (a.getProject()==null) {
			if (!includeWithoutProject) {
				return false;
			}
		} else if (!projects.contains(a.getProject())) {
			return false;
		}
		return filter.isAcceptable(f, a);
	}

}
