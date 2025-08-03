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

import org.gtdfree.model.GTDData.ActionProxy;


/**
 * @author ikesan
 *
 */
public class FolderEvent extends ActionEvent {
	private static final long serialVersionUID = 1L;

	/**
	 * @param source
	 * @param folder
	 * @param i
	 */
	public FolderEvent(Folder folder, Action a, ActionProxy ap, boolean recycled) {
		super(folder,a,ap,recycled);
	}

	/**
	 * @param source
	 * @param folder
	 * @param i
	 */
	public FolderEvent(Folder folder, Action[] a, ActionProxy[] ap, boolean recycled) {
		super(folder,a,ap,recycled);
	}

	/**
	 * @param source
	 * @param action
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	public FolderEvent(Folder f, Action action, ActionProxy actionP, String property, Object oldValue, Object newValue,boolean recycled) {
		super(f,action,actionP,property,oldValue,newValue,recycled);
	}

	/**
	 * @param source
	 * @param action
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	public FolderEvent(Folder f, Action[] action, ActionProxy[] actionP, String property, Object oldValue, Object newValue,boolean recycled) {
		super(f,action,actionP,property,oldValue,newValue,recycled);
	}

	/**
	 * @return the folder
	 */
	public Folder getFolder() {
		return (Folder)getSource();
	}
	
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append("FolderEvent={source="); //$NON-NLS-1$
		sb.append(((Folder)getSource()).getName());
		sb.append(", prop="); //$NON-NLS-1$
		sb.append(getProperty());
		sb.append(", old="); //$NON-NLS-1$
		sb.append(getOldValue());
		sb.append(", new="); //$NON-NLS-1$
		sb.append(getNewValue());
		sb.append("}"); //$NON-NLS-1$
		return sb.toString();
	}

	
}
