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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;
import org.gtdfree.model.Action.Resolution;
import org.gtdfree.model.GTDData.ActionProxy;
import org.gtdfree.model.GTDData.FolderDataProxy;


public class Folder implements Iterable<Action> {

	public enum FolderType {
		INBUCKET, ACTION, REFERENCE, BUILDIN, PROJECT, QUEUE, BUILDIN_REMIND, BUILDIN_RESOLVED, BUILDIN_PRIORITY, SOMEDAY, BUILDIN_DELETED
	};
	
	public enum FolderPreset {
		ALL, OPEN;
	}
	
	private String name;
	private int id;
	private Comparator<Action> comparator;
	private int openCount=0;
	private boolean closed=false;
	private FolderDataProxy data;
	private Integer typeId;

	private transient EventListenerList listeners;
	private transient FolderType type;
	private transient GTDModel parent;
	private transient boolean suspendedForMultipleChanges;
	

	
	FolderDataProxy getData() {
		return data;
	}
	/**
	 * @return the comparator
	 */
	public Comparator<Action> getComparator() {
		return comparator;
	}

	/**
	 * @param comparator the comparator to set
	 */
	public void setComparator(Comparator<Action> comparator) {
		this.comparator = comparator;
		data.store();
	}

	/**
	 * @return the type
	 */
	public FolderType getType() {
		if (type==null && typeId!=null) {
			type= FolderType.values()[typeId];
		}
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	void setType(FolderType type) {
		this.type = type;
		typeId= type.ordinal();
		data.store();
	}

	public void addFolderListener(FolderListener l) {
		if (listeners==null) {
			listeners= new EventListenerList();
		}
		listeners.add(FolderListener.class, l);
	}

	public void removeFolderListener(FolderListener l) {
		if (listeners==null) {
			listeners= new EventListenerList();
		}
		listeners.remove(FolderListener.class, l);
	}

	private void fireElementAdded(Action a, ActionProxy ap) {
		fireElementAdded(new Action[]{a}, new ActionProxy[]{ap}, !isUserFolder()&&!isInBucket());	
	}
	
	private void fireElementAdded(Action[] a, ActionProxy[] ap) {
		fireElementAdded(a, ap, !isUserFolder()&&!isInBucket());	
	}
	
	private void fireElementAdded(Action[] a, ActionProxy[] ap, boolean recycled) {
		if (a==null || a.length==0 || ap==null || ap.length==0) {
			return;
		}
		
		FolderEvent f= new FolderEvent(this,a,ap,recycled);
		if (listeners==null) {
			listeners= new EventListenerList();
		}
		FolderListener[] l = listeners.getListeners(FolderListener.class);
		for (FolderListener listener : l) {
			try {
				listener.elementAdded(f);
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			}
		}
	}

	private void fireElementRemoved(Action a, ActionProxy i) {
		fireElementRemoved(new Action[]{a}, new ActionProxy[]{i}, !isUserFolder()&&!isInBucket());
	}

	private void fireElementRemoved(Action[] a, ActionProxy[] i) {
		fireElementRemoved(a, i, !isUserFolder()&&!isInBucket());
	}
	
	private void fireElementRemoved(Action[] a, ActionProxy[] ap, boolean recycled) {
		if (a==null || a.length==0 || ap==null || ap.length==0) {
			return;
		}
		FolderEvent f= new FolderEvent(this,a,ap,recycled);
		if (listeners==null) {
			listeners= new EventListenerList();
		}
		FolderListener[] l = listeners.getListeners(FolderListener.class);
		for (FolderListener listener : l) {
			try {
				listener.elementRemoved(f);
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			}
		}
	}

	public void fireElementModified(Action a, ActionProxy ap, String property, Object oldVal, Object newVal) {
		fireElementModified(a,ap,property,oldVal,newVal,false);
		ap.store();
	}

	public void fireElementModified(Action a, ActionProxy ap, String property, Object oldVal, Object newVal, boolean recycled) {
		if (a==null || ap==null) {
			return;
		}
		if ((oldVal==null && newVal==null) || (oldVal!=null && oldVal.equals(newVal))) {
			return;
		}
		fireElementModified(new ActionEvent(this,a,ap,property,oldVal,newVal,recycled));
	}

	public void fireElementModified(Action[] a, ActionProxy[] ap, String property, Object oldVal, Object newVal, boolean recycled) {
		if (a==null || a.length==0 || ap==null || ap.length==0) {
			return;
		}
		if ((oldVal==null && newVal==null) || (oldVal!=null && oldVal.equals(newVal))) {
			return;
		}
		fireElementModified(new ActionEvent(this,a,ap,property,oldVal,newVal,recycled));
	}

	private void fireElementModified(ActionEvent i) {
		if (!data.contains(i.getActionProxy())) {
			return;
		}
		if (i.getProperty().equals(Action.RESOLUTION_PROPERTY_NAME)) {
			if (Resolution.OPEN==i.getOldValue()) {
				decOpenCount();
			} else if (Resolution.OPEN==i.getNewValue()) {
				incOpenCount();
			}
		}
		if (listeners==null) {
			listeners= new EventListenerList();
		}
		FolderListener[] l = listeners.getListeners(FolderListener.class);
		for (FolderListener listener : l) {
			try {
				listener.elementModified(i);
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			}
		}
	}

	private void fireOrderChanged() {
		if (listeners==null) {
			listeners= new EventListenerList();
		}
		FolderListener[] l = listeners.getListeners(FolderListener.class);
		for (FolderListener listener : l) {
			try {
				listener.orderChanged(this);
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			}
		}
	}

	private void sort() {
		if (getComparator()!=null && !suspendedForMultipleChanges) {
			data.sort(getComparator());
		}
	}

	synchronized void add(int i, Action a) {
		ActionProxy ap = parent.getDataRepository().getProxy(a);
		add(i,a,ap);
	}
	
	synchronized void add(int i, Action a, ActionProxy ap) {
		if (data.contains(ap)) {
			return;
		}
		data.suspend(true);
		data.add(i, ap);
		if (!isMeta()) ap.setParent(this);
		sort();
		if (a.isOpen() && isClosed()) {
			setClosed(false);
		}
		if (a.isOpen()) {
			incOpenCount();
		}
		data.suspend(false);
		fireElementAdded(a,ap);
	}

	synchronized void add(int idx, Action[] ac) {
		if (ac==null || ac.length==0) {
			return;
		}
		ActionProxy[] ap= new ActionProxy[ac.length];
		for (int i = 0; i < ap.length; i++) {
			ap[i]=ac[i].getProxy();
		}
		add(idx, ac, ap);
	}
	synchronized void add(int idx, Action[] ac, ActionProxy[] ap) {
		if (ac==null || ac.length==0 || ap==null || ap.length==0) {
			return;
		}
		
		List<Action> aac= new ArrayList<Action>(ac.length);
		List<ActionProxy> aap= new ArrayList<ActionProxy>(ac.length);
		
		data.suspend(true);
		int ins=0;
		for (int i = 0; i < ac.length; i++) {
			Action a= ac[i];
			ActionProxy p= ap[i];
			if (data.contains(p)) {
				continue;
			}
			data.add(idx+ins,p);
			ins++;
			if (!isMeta()) a.setParent(this);
			if (a.isOpen() && isClosed()) {
				setClosed(false);
			}
			aac.add(a);
			aap.add(p);
			if (a.isOpen()) {
				incOpenCount();
			}
		}
		if (aac.size()==0) {
			return;
		}
		sort();
		data.suspend(false);

		fireElementAdded(aac.toArray(new Action[aac.size()]), aap.toArray(new ActionProxy[aap.size()]));
	}

	synchronized void add(Action a) {
		ActionProxy ap = parent.getDataRepository().getProxy(a);
		add(a,ap);
	}
	
	synchronized void add(Action a, ActionProxy ap) {
		if (data.contains(ap)) {
			return;
		}
		data.suspend(true);
		data.add(ap);
		if (!isMeta()) a.setParent(this);
		sort();
		if (a.isOpen() && isClosed()) {
			setClosed(false);
		}
		if (a.isOpen()) {
			incOpenCount();
		}
		data.suspend(false);
		fireElementAdded(a,ap);
	}

	synchronized boolean remove(Action a, ActionProxy i) {
		data.suspend(true);
		boolean b= data.remove(i);
		if (b) {
			if (a.isOpen()) {
				decOpenCount();
			}
			fireElementRemoved(a,i);
		}
		data.suspend(false);
		return b;
	}

	synchronized boolean remove(int i) {
		ActionProxy ap= data.get(i);
		Action a= ap.get();
		data.suspend(true);
		data.remove(i);
		if (a!=null) {
			if (a.isOpen()) {
				decOpenCount();
			}
		}
		data.suspend(false);
		fireElementRemoved(a,ap);
		return true;
	}

	/**
	 * @param parent
	 * @param name
	 */
	public Folder(GTDModel parent, int id, String name, FolderType type, FolderDataProxy data) {
		super();
		this.id=id;
		this.parent = parent;
		this.name = name;
		this.type = type;
		this.data=data;
		typeId= type.ordinal();
	}

	public void rename(String newName) {
		data.suspend(true);
		parent.renameFolder(this, newName);
		data.store();
		data.suspend(false);
	}

	public String getName() {
		return name;
	}

	public Iterator<Action> iterator() {
		return new Iterator<Action>() {
			int i= 0;
			@Override
			public boolean hasNext() {
				return i<size();
			}
			@Override
			public Action next() {
				synchronized (Folder.this) {
					if (i<size()) {
						return data.get(i++).get();
					}
					return null;
				}
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public Iterator<Action> iterator(final FolderPreset fp) {
		return new Iterator<Action>() {
			Iterator<ActionProxy> i= data.iterator(fp);
			@Override
			public boolean hasNext() {
				return i.hasNext();
			}
			@Override
			public Action next() {
				synchronized (Folder.this) {
					if (i.hasNext()) {
						return i.next().get();
					}
					return null;
				}
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public Iterator<ActionProxy> proxyIterator(FolderPreset fp) {
		return data.iterator(fp);
	}

	public Action getActionByID(int id) {
		Iterator<ActionProxy> i= data.iterator(FolderPreset.ALL);
		while (i.hasNext()) {
			ActionProxy a= i.next();
			if (a.getId() == id) {
				return a.get();
			}
		}
		return null;
	}

	public GTDModel getParent() {
		return parent;
	}

	void setParent(GTDModel  p) {
		parent=p;
	}

	public int size() {
		return data.size();
	}
	
	/**
	 * Meta folder is folder which does to actually contain actions but provide folder-like
	 * interface to group of actions with particular common feature.
	 * @return
	 */
	public boolean isMeta() {
		return isBuildIn() || type==FolderType.PROJECT || type==FolderType.QUEUE;
	}
	
	public synchronized void moveUp(Action a) {
		int open=0;
		for (int i = 0; i < data.size(); i++) {
			if (a.getId()==data.get(i).getId()) {
				if (open==i) {
					return;
				}
				data.suspend(true);
				data.set(i, data.get(open));
				data.set(open, parent.getDataRepository().getProxy(a));
				data.suspend(false);
				fireOrderChanged();
				return;
			}
			if (data.get(i).get().isOpen()) {
				open=i;
			}
		}
	}
	public boolean canMoveUp(Action a) {
		if (a==null) {
			return false;
		}
		for (int i = 0; i < data.size(); i++) {
			if (a.getId()==data.get(i).getId()) {
				return false;
			}
			if (data.get(i).get().isOpen()) {
				return true;
			}
		}
		return false;
	}
	public synchronized void moveDown(Action a) {
		int open= data.size()-1;
		for (int i = data.size()-1 ; i > -1 ; i--) {
			if (a.getId()==data.get(i).getId()) {
				if (open==i) {
					return;
				}
				data.suspend(true);
				data.set(i, data.get(open));
				data.set(open, parent.getDataRepository().getProxy(a));
				data.suspend(false);
				fireOrderChanged();
				return;
			}
			if (data.get(i).get().isOpen()) {
				open=i;
			}
		}
	}
	public boolean canMoveDown(Action a) {
		if (a==null) {
			return false;
		}
		for (int i = data.size()-1 ; i > -1 ; i--) {
			if (a.getId()==data.get(i).getId()) {
				return false;
			}
			if (data.get(i).get().isOpen()) {
				return true;
			}
		}
		return false;
	}
	
	public Action get(int i) {
		return data.get(i).get();
	}

	public ActionProxy getProxy(int i) {
		return data.get(i);
	}
	
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append("Folder{ id= "); //$NON-NLS-1$
		sb.append(id);
		sb.append(", name= "); //$NON-NLS-1$
		sb.append(name);
		sb.append(", type= "); //$NON-NLS-1$
		sb.append(type);
		sb.append(", actions= "); //$NON-NLS-1$
		sb.append(data.size());
		sb.append("}"); //$NON-NLS-1$
		return sb.toString();
	}
	
	/*public synchronized Action[] actions() {
		return actions.toArray(new Action[actions.size()]);
	}*/

	void setName(String newName) {
		name=newName;
		data.store();
	}

	/**
	 * @return the openCount
	 */
	public int getOpenCount() {
		return openCount;
	}

	public synchronized void visit(Visitor v) {
		v.meet(this);
		Iterator<ActionProxy> i= data.iterator(FolderPreset.ALL);
		while (i.hasNext()) {
			v.meet(i.next().get());
		}
		v.depart(this);
	}

	public synchronized int indexOf(Action selectedAction) {
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).getId()==selectedAction.getId()) {
				return i;
			}
		}
		return -1;
	}
	public int getId() {
		return id;
	}

	public boolean isBuildIn() {
		return getType()==FolderType.BUILDIN || getType()==FolderType.BUILDIN_REMIND || getType()==FolderType.BUILDIN_RESOLVED || getType()==FolderType.BUILDIN_DELETED || getType()==FolderType.BUILDIN_PRIORITY;
	}
	
	public boolean contains(ActionProxy a) {
		return data.contains(a);
	}
	
	/**
	 * Returns true if folder is ACTION or REFERENCE or SOMEDAY.
	 * @return true if folder is ACTION or REFERENCE or SOMEDAY
	 */
	public boolean isUserFolder() {
		return getType()==FolderType.ACTION || getType()==FolderType.REFERENCE || getType()==FolderType.SOMEDAY;
	}
	
	/**
	 * Returns true if folder is PROJECT.
	 * @return true if folder is PROJECT
	 */
	public boolean isProject() {
		return getType()==FolderType.PROJECT;
	}

	/**
	 * Returns true if folder is INBUCKET.
	 * @return true if folder is INBUCKET
	 */
	public boolean isInBucket() {
		return getType()==FolderType.INBUCKET;
	}

	/**
	 * @return the closed
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * @param closed the closed to set
	 */
	public void setClosed(boolean closed) {
		if (this.closed == closed) {
			return;
		}
		this.closed = closed;
		data.setResolved(new Date());
		parent.fireFolderModified(this,"closed",!closed,closed,false); //$NON-NLS-1$
	}
	
	public boolean isSuspendedForMultipleChanges() {
		return suspendedForMultipleChanges;
	}

	public void setSuspendedForMultipleChanges(boolean suspendedForMultipleChanges) {
		this.suspendedForMultipleChanges = suspendedForMultipleChanges;
		if (!suspendedForMultipleChanges) {
			sort();
		}
	}

	public boolean isQueue() {
		return type==FolderType.QUEUE;
	}

	/**
	 * Returns <code>true</code> if this folder type is REFERENCE.
	 * @return <code>true</code> if this folder type is REFERENCE
	 */
	public boolean isReference() {
		return getType()==FolderType.REFERENCE;
	}

	/**
	 * Returns <code>true</code> if this folder type is ACTION.
	 * @return <code>true</code> if this folder type is ACTION
	 */
	public boolean isAction() {
		return getType()==FolderType.ACTION;
	}

	/**
	 * Returns <code>true</code> if this folder type is SOMEDAY.
	 * @return <code>true</code> if this folder type is SOMEDAY
	 */
	public boolean isSomeday() {
		return getType()==FolderType.SOMEDAY;
	}

	/**
	 * Returns <code>true</code> if this is one of default folders (exists in each GTDModel).
	 * Default folders are QUEUE, INBUCKET or one of BUILDIN.
	 * @return <code>true</code> if this is one of default folders
	 */
	public boolean isDefault() {
		return getType()==FolderType.QUEUE || getType()==FolderType.INBUCKET || isBuildIn();
	}

	public String getDescription() {
		return data.getDescription();
	}
	
	public void setDescription(String d) {
		if ((data.getDescription()!=null && data.getDescription().equals(d)) 
				|| ((data.getDescription()==null || data.getDescription().length()==0) 
						&& (d==null || d.length()==0))) {
			return;
		}
		String old= data.getDescription();
		data.setDescription(d);
		if (parent!=null) {
			parent.fireFolderModified(this, "description", old, d, false); //$NON-NLS-1$
		}
	}
	
	/*public int count(ActionFilter f) {
		int c=0;
		for (ActionProxy a : data.getActions()) {
			if (f.isAcceptable(this, a.get())) {
				c++;
			}
		}
		return c;
	}*/

	public boolean isTickler() {
		return type == FolderType.BUILDIN_REMIND;
	}
	
	void purgeAll() {
		ActionProxy[] ap= data.toArray();
		Action[] ac= new Action[ap.length];
		
		data.suspend(true);
		data.clear();
		openCount=0;
		data.suspend(false);
		
		for (int i = 0; i < ap.length; i++) {
			ac[i]= ap[i].get();
		}
		
		fireElementRemoved(ac, ap);
		
		for (int i = 0; i < ap.length; i++) {
			ap[i].delete();
		}
	}

	void clear() {
		ActionProxy[] ap= data.toArray();
		Action[] ac= new Action[ap.length];
		
		data.suspend(true);
		data.clear();
		openCount=0;
		data.suspend(false);
		
		for (int i = 0; i < ap.length; i++) {
			ac[i]= ap[i].get();
		}
		
		fireElementRemoved(ac, ap);
	}
	
	void setOpenCount(int open) {
		openCount= open;
		data.store();
	}

	void add(Action[] ac, ActionProxy[] ap) {
		if (ac==null || ac.length==0 || ap==null || ap.length==0) {
			return;
		}
		
		List<Action> aac= new ArrayList<Action>(ac.length);
		List<ActionProxy> aap= new ArrayList<ActionProxy>(ac.length);
		
		data.suspend(true);

		for (int i = 0; i < ac.length; i++) {
			Action a= ac[i];
			ActionProxy p= ap[i];
			if (data.contains(p)) {
				continue;
			}
			data.add(p);
			if (!isMeta()) a.setParent(this);
			if (a.isOpen() && isClosed()) {
				setClosed(false);
			}
			aac.add(a);
			aap.add(p);
			if (a.isOpen()) {
				incOpenCount();
			}
		}
		if (aac.size()==0) {
			return;
		}
		sort();
		
		data.suspend(false);
		fireElementAdded(aac.toArray(new Action[aac.size()]), aap.toArray(new ActionProxy[aap.size()]));
	}
	
	synchronized boolean remove(Action[] ac, ActionProxy[] ap) {
		if (ac==null || ac.length==0 || ap==null || ap.length==0) {
			return false;
		}
		
		List<Action> aac= new ArrayList<Action>(ac.length);
		List<ActionProxy> aap= new ArrayList<ActionProxy>(ac.length);
		
		data.suspend(true);
		for (int i = 0; i < ac.length; i++) {
			Action a= ac[i];
			ActionProxy p= ap[i];
			if (data.remove(p)) {
				aac.add(a);
				aap.add(p);
				if (a.isOpen()) {
					decOpenCount();
				}
			}
		}
		if (aac.size()==0) {
			return false;
		}
		sort();
		data.suspend(false);
		fireElementRemoved(aac.toArray(new Action[aac.size()]), aap.toArray(new ActionProxy[aap.size()]));
		return true;
	}
	
	private void incOpenCount() {
		if (openCount<size()) {
			openCount++;
			data.store();
		}
		
/*		int open= countOpen();
		if (open!=openCount) {
			System.out.println("Open reference "+openCount+" counted "+open);
			Thread.dumpStack();
			openCount=open;
		}*/
		
	}

	private void decOpenCount() {
		if (openCount>0) {
			openCount--;
			data.store();
		}
		
		/*int open= countOpen();
		if (open!=openCount) {
			System.out.println("Open reference "+openCount+" counted "+open);
			Thread.dumpStack();
			openCount=open;
		}*/
	}
	
	@SuppressWarnings("unused")
	private int countOpen() {
		int open=0;
		
		for (Action a : this) {
			if (a.isOpen()) {
				open++;
			}
		}
		return open;
	}
	
	public Date getResolved() {
		return data.getResolved();
	}
	
	public Date getModified() {
		return data.getModified();
	}
	
	public Date getCreated() {
		return data.getCreated();
	}
	
	void setDates(Date created, Date modified, Date resolved) {
		data.suspend(true);
		data.setCreated(created);
		data.setModified(modified);
		data.setResolved(resolved);
		data.suspend(false);
	}
	
	/**
	 * Returns open action with same or higher index, then parameter.
	 * @param i low action index filter
	 * @return open action with same or higher index, then parameter, or null if not found
	 */
	public Action getOpenFrom(int i) {
		for (;i<size();i++) {
			Action a= get(i);
			if (a.isOpen()) {
				return a;
			}
		}
		return null;
	}
	
	public void reorder(Action[] order) {
		data.reorder(order);
		fireOrderChanged();
	}
}
