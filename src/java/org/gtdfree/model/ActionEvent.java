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

import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.model.GTDData.ActionProxy;

/**
 * @author ikesan
 *
 */
public class ActionEvent extends EventObject {
	
	private static final long serialVersionUID = 1L;

	static class SortedElements {
		
		enum ActionIndex {RESOLVED,DELETED,REMINDER,PRIORITY,QUEUE,PROJECT}
		
		private Action[] ac;
		private ActionProxy[] ap;
		private List<Integer>[] indices;

		@SuppressWarnings("unchecked")
		public SortedElements(Action[] ac, ActionProxy[] ap) {
			this.ac=ac;
			this.ap=ap;
			indices = new List[ActionIndex.values().length];
			
			for (Integer i = 0; i < ap.length; i++) {
				Action aa= ac[i];
				
				if (aa.isResolved()) {
					index(ActionIndex.RESOLVED).add(i);
				}
				if (aa.isDeleted()) {
					index(ActionIndex.DELETED).add(i);
				}
				if (aa.getRemind()!=null) {
					index(ActionIndex.REMINDER).add(i);
				}
				if (aa.getPriority()!=null && aa.getPriority()!=Priority.None) {
					index(ActionIndex.PRIORITY).add(i);
				}
				if (aa.isQueued()) {
					index(ActionIndex.QUEUE).add(i);
				}
				if (aa.getProject()!=null) {
					index(ActionIndex.PROJECT).add(i);
				}
			}
		}
		
		private List<Integer> index(ActionIndex i) {
			List<Integer> l= indices[i.ordinal()];
			if (l==null) {
				indices[i.ordinal()] = l = new LinkedList<Integer>();
			}
			return l;
		}
		
		private Action[] getActions(List<Integer> l) {
			Action[] a= new Action[l.size()];
			int k=0;
			for (Integer i : l) {
				a[k++]=ac[i];
			}
			return a;
		}
		
		private ActionProxy[] getActionProxies(List<Integer> l) {
			ActionProxy[] a= new ActionProxy[l.size()];
			int k=0;
			for (Integer i : l) {
				a[k++]=ap[i];
			}
			return a;
		}
		
		private Action[] getActionsInv(List<Integer> l) {
			if (l.size()==0) {
				return ac;
			}
			Action[] a= new Action[ac.length-l.size()];
			int k=0;
			Iterator<Integer> it= l.iterator();
			int in= it.next();
			for (int i=0; i< ac.length; i++) {
				if (i==in) {
					in=it.hasNext() ? it.next() : -1; 
				} else {
					a[k++]=ac[i];
				}
			}
			return a;
		}
		
		private ActionProxy[] getActionProxiesInv(List<Integer> l) {
			if (l.size()==0) {
				return ap;
			}
			ActionProxy[] a= new ActionProxy[ap.length-l.size()];
			int k=0;
			Iterator<Integer> it= l.iterator();
			int in= it.next();
			for (int i=0; i< ap.length; i++) {
				if (i==in) {
					in=it.hasNext() ? it.next() : -1; 
				} else {
					a[k++]=ap[i];
				}
			}
			return a;
		}

		public Action[] getActions() {
			return ac;
		}
		
		public ActionProxy[] getActionProxies() {
			return ap;
		}

		public Action[] getActions(ActionIndex i) {
			return getActions(index(i));
		}
		
		public ActionProxy[] getActionProxies(ActionIndex i) {
			return getActionProxies(index(i));
		}
		
		public Action[] getActionsInv(ActionIndex i) {
			return getActionsInv(index(i));
		}
		
		public ActionProxy[] getActionProxiesInv(ActionIndex i) {
			return getActionProxiesInv(index(i));
		}

		public int size(ActionIndex i) {
			return indices[i.ordinal()] == null ? 0 : index(i).size();
		}
		public int sizeInv(ActionIndex i) {
			return ac.length - (indices[i.ordinal()] == null ? 0 : index(i).size());
		}
		
	}
	
	private Action[] action;
	private ActionProxy[] actionP;
	private String property;
	private Object oldValue;
	private Object newValue;

	private boolean recycled=false;
	private SortedElements sortedElements;

	
	/**
	 * @param source
	 * @param action
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	public ActionEvent(Folder f, Action action, ActionProxy actionP, String property, Object oldValue, Object newValue, boolean recycled) {
		this(f,new Action[]{action}, new ActionProxy[]{actionP},property,oldValue,newValue,recycled);
	}
	/**
	 * @param source
	 * @param action
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	public ActionEvent(Folder f, Action[] action, ActionProxy[] actionP, String property, Object oldValue, Object newValue, boolean recycled) {
		super(f);
		this.action = action;
		this.actionP = actionP;
		this.property = property;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.recycled = recycled;
		if (property!=null) {
			if (getNewValue()==getOldValue()) {
				throw new RuntimeException("Internal error, property not changed: "+toString()); //$NON-NLS-1$
			}
			if ((getNewValue()==null || ApplicationHelper.EMPTY_STRING.equals(getNewValue())) 
					&& (!Action.REMIND_PROPERTY_NAME.equals(property))) {
				/*try {
					throw new RuntimeException("Internal warning, new value is null: "+toString()); //$NON-NLS-1$
				} catch (Exception e) {
					Logger.getLogger(this.getClass()).debug("Internal error.", e);
				}*/
			}
		}
	}

	/**
	 * @param source
	 * @param i
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	public ActionEvent(Folder f, Action[] a, ActionProxy[] ap, boolean recycled) {
		super(f);
		this.action = a;
		this.actionP = ap;
		this.recycled=recycled;
	}
	/**
	 * @param source
	 * @param i
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	public ActionEvent(Folder f, Action a, ActionProxy ap, boolean recycled) {
		this(f,new Action[]{a}, new ActionProxy[]{ap},recycled);
	}
	
	public boolean isRecycled() {
		return recycled;
	}
	
	/**
	 * @return the action
	 */
	public Action getAction() {
		return action[0];
	}

	/**
	 * @return the action
	 */
	public ActionProxy getActionProxy() {
		return actionP[0];
	}

	/**
	 * @return the newValue
	 */
	public Object getNewValue() {
		return newValue;
	}

	/**
	 * @return the oldValue
	 */
	public Object getOldValue() {
		return oldValue;
	}

	/**
	 * @return the property
	 */
	public String getProperty() {
		return property;
	}
	
	public Action[] getActions() {
		return action;
	}
	
	public ActionProxy[] getActionProxies() {
		return actionP;
	}

	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append("ActionEvent={source="); //$NON-NLS-1$
		sb.append(((Folder)getSource()).getName());
		sb.append(", actions="); //$NON-NLS-1$
		sb.append(action.length);
		sb.append(", prop="); //$NON-NLS-1$
		sb.append(property);
		sb.append(", old="); //$NON-NLS-1$
		sb.append(oldValue);
		sb.append(", new="); //$NON-NLS-1$
		sb.append(newValue);
		sb.append("}"); //$NON-NLS-1$
		return sb.toString();
	}
	
	public synchronized SortedElements getSortedElements() {
		if (sortedElements==null) {
			sortedElements= new SortedElements(action,actionP);
		}
		return sortedElements;
	}
}
