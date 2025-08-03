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

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

import org.gtdfree.GlobalProperties;
import org.gtdfree.model.Folder.FolderPreset;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public interface GTDData {
	public static interface ActionProxy {
		
		/**
		 * Notifies data that action has been updated.
		 */
		public Action get();

		/**
		 * Notifies data that action has been updated.
		 */
		public void store();
		
		/**
		 * Returns Id for action.
		 * @return
		 */
		public int getId();
		
		public void delete();
		
		public Folder getParent();
		
		public void setParent(Folder f);

	}
	
	public static interface FolderDataProxy {
		
		/**
		 * Notifies data that action has been updated.
		 */
		public void store();
		
		public void delete();

		public String getDescription();

		public void setDescription(String desc);

		public boolean contains(ActionProxy ap);

		public int size();

		public ActionProxy get(int i);

		public Iterator<ActionProxy> iterator(FolderPreset fp);

		public void sort(Comparator<Action> comparator);

		public void add(int i, ActionProxy ap);

		public void add(ActionProxy ap);

		public boolean remove(int i);

		public boolean remove(ActionProxy i);

		public void set(int i, ActionProxy actionProxy);

		public ActionProxy[] toArray();

		public void clear();
		
		public void suspend(boolean b);
		
		public Date getCreated();
		public Date getResolved();
		public Date getModified();
		public void setCreated(Date d);
		public void setResolved(Date d);
		public void setModified(Date d);

		public void reorder(Action[] order);

	}

	/**
	 * Registers new Folder with data repository.
	 * @param f the new folder to be registered
	 * @return new folder
	 */
	public Folder newFolder(int id, String name, FolderType type);
	
	/**
	 * Notifies data repository that folder has been updated
	 * @param f the updated folder
	 */
	public void store();
	/**
	 * Initializes this data repository to be ready for usage.
	 */
	public void initialize(File dataLoc, GlobalProperties prop);

	/**
	 * Restores GTDModel or creates empty one.
	 * @return restored model
	 */
	public GTDModel restore() throws IOException;
	
	/**
	 * Returns reference to Action. Reference to returned action will not be kept in GTDModel.
	 * @return action
	 */
	public ActionProxy newAction(int id, Date created, Date resolved, String description);

	public ActionProxy newAction(int id, Action copy, Integer project);

	/**
	 * Returns proxy for given action. If Action already contains proxy, then it is returned.
	 * If Action does not contain proxy, then proxy it is created and set to the action.
	 * This also means that Action is registered with the data store. 
	 * @param a an action fot which proxy is if necessary created and returned
	 * @return proxy proxy for the action
	 */
	public ActionProxy getProxy(Action a);
	
	/**
	 * Write all cached data to file system.
	 * @throws IOException if fails
	 */
	void flush() throws IOException;
	
	/**
	 * Close data repository for further changes. This is called before application is closed.
	 * @param terminal if true then close of application will be completed regardless the reply.
	 * @return if true, close was successful, if false then data repository denies close attempt for some reason (like user changes his mind when some data is not saved)
	 * @throws IOException if fails
	 */
	public boolean close(boolean terminal) throws IOException;

	public boolean isClosed();

	/**
	 * Suspends save to database files. Used before larger amount of data is to be 
	 * imported in order to speed up the process.
	 * @param b flag to suspend
	 */
	public void suspend(boolean b);

	/**
	 * Check consistency of data.
	 * @param log
	 * @param fail
	 * @param correct
	 * @throws ConsistencyException
	 */
	public void checkConsistency(Logger log, boolean fail, boolean correct) throws ConsistencyException;
	
	/**
	 * Returns data format.
	 * @return data format.
	 */
	public String getDatabaseType();

}
