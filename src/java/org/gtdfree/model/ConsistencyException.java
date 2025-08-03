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
public class ConsistencyException extends Exception {

	private static final long serialVersionUID = 1L;

	private Action[] actions;
	private Folder[] folders;
	private Project[] projects;
	
	/**
	 * @param message
	 */
	public ConsistencyException(String message) {
		super(message);
	}
	/**
	 * @param message
	 * @param cause
	 */
	public ConsistencyException(String message, Throwable cause) {
		super(message, cause);
	}
	/**
	 * @param message
	 */
	public ConsistencyException(String message,Action[] a, Folder[] f, Project[] p) {
		super(message);
		actions=a;
		folders=f;
		projects=p;
	}
	/**
	 * @return the actions
	 */
	public Action[] getActions() {
		return actions;
	}
	/**
	 * @return the folders
	 */
	public Folder[] getFolders() {
		return folders;
	}
	/**
	 * @return the projects
	 */
	public Project[] getProjects() {
		return projects;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb= new StringBuilder();
		
		sb.append(getMessage());
		sb.append('\n');
		sb.append('\n');
		if (actions!=null) {
			if (actions.length==1) {
				sb.append("Action:\n"); //$NON-NLS-1$
				append(sb, actions[0]);
			} else {
				sb.append("Actions:\n"); //$NON-NLS-1$
				for (int i = 0; i < actions.length; i++) {
					append(sb, actions[i]);
				}
			}
			sb.append('\n');
		}
		if (folders!=null) {
			if (folders.length==1) {
				sb.append("List:\n"); //$NON-NLS-1$
				append(sb, folders[0]);
			} else {
				sb.append("Lists:\n"); //$NON-NLS-1$
				for (int i = 0; i < folders.length; i++) {
					append(sb, folders[i]);
				}
			}
			sb.append('\n');
		}
		if (projects!=null) {
			if (projects.length==1) {
				sb.append("Project:\n"); //$NON-NLS-1$
				append(sb, projects[0]);
			} else {
				sb.append("Projects:\n"); //$NON-NLS-1$
				for (int i = 0; i < projects.length; i++) {
					append(sb, projects[i]);
				}
			}
			sb.append('\n');
		}
		
		return sb.toString();
	}
	
	private void append(StringBuilder sb, Action a) {
		sb.append("ID="); //$NON-NLS-1$
		sb.append(a.getId());
		sb.append(" status="); //$NON-NLS-1$
		sb.append(a.getResolution());
		sb.append(" desc='"); //$NON-NLS-1$
		if (a.getDescription()!=null) {
			if (a.getDescription().length()>15) {
				sb.append(a.getDescription().substring(0,11));
				sb.append("...'\n"); //$NON-NLS-1$
			} else {
				sb.append(a.getDescription());
				sb.append("'\n"); //$NON-NLS-1$
			}
		} else {
			sb.append("'\n"); //$NON-NLS-1$
		}
	}
	
	private void append(StringBuilder sb, Folder a) {
		sb.append("ID="); //$NON-NLS-1$
		sb.append(a.getId());
		sb.append(" name="); //$NON-NLS-1$
		sb.append(a.getName());
		sb.append(" type="); //$NON-NLS-1$
		sb.append(a.getType());
		sb.append('\n');
	}

	private void append(StringBuilder sb, Project a) {
		sb.append("ID="); //$NON-NLS-1$
		sb.append(a.getId());
		sb.append(" name="); //$NON-NLS-1$
		sb.append(a.getName());
		sb.append('\n');
	}

}
