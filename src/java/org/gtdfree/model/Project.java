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

import org.gtdfree.model.GTDData.FolderDataProxy;

/**
 * @author ikesan
 *
 */
public class Project extends Folder {
	private String goal;
	public Project(GTDModel parent, int id, String name, FolderDataProxy data) {
		super(parent, id, name, Folder.FolderType.PROJECT,data);
	}
	/**
	 * @return the goal
	 */
	public String getGoal() {
		return goal;
	}
	/**
	 * @param goal the goal to set
	 */
	public void setGoal(String goal) {
		if ((this.goal!=null && this.goal.equals(goal)) || (this.goal==null && goal==null)) {
			return;
		}
		String o= this.goal;
		this.goal = goal;
		// TODO support in data
		getParent().fireFolderModified(this,"goal",o,goal,false); //$NON-NLS-1$
	}
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append("Project{ id= "); //$NON-NLS-1$
		sb.append(getId());
		sb.append(", name= "); //$NON-NLS-1$
		sb.append(getName());
		sb.append(", closed= "); //$NON-NLS-1$
		sb.append(isClosed());
		sb.append("}"); //$NON-NLS-1$
		return sb.toString();
	}
}
