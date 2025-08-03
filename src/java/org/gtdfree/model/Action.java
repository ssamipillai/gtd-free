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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.apache.log4j.Logger;
import org.gtdfree.model.GTDData.ActionProxy;

public final class Action {
	
	public static final String RESOLUTION_PROPERTY_NAME= "resolution"; //$NON-NLS-1$
	public static final String PROJECT_PROPERTY_NAME= "project"; //$NON-NLS-1$
	public static final String QUEUED_PROPERTY_NAME= "queued"; //$NON-NLS-1$
	public static final String REMIND_PROPERTY_NAME= "remind"; //$NON-NLS-1$
	public static final String PRIORITY_PROPERTY_NAME= "priority"; //$NON-NLS-1$

	public enum ActionType {Mail,Phone,Meet,Read,Watch};
	public static enum Resolution {
		OPEN,DELETED,RESOLVED,STALLED;
		
		public static Resolution toResolution(String s) {
			if ("TRASHED".equalsIgnoreCase(s)) { //$NON-NLS-1$
				return DELETED;
			}
			if ("RESOVED".equalsIgnoreCase(s)) { //$NON-NLS-1$
				return RESOLVED;
			}
			return valueOf(s);
		}
	};
	
	public static final boolean hasOpen(Action[] actions) {
		if (actions!=null) { 
			for (Action action : actions) {
				if (action.isOpen()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static final boolean hasNonOpen(Action[] actions) {
		if (actions!=null) { 
			for (Action action : actions) {
				if (!action.isOpen()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean hasDeleted(Action[] actions) {
		if (actions!=null) { 
			for (Action action : actions) {
				if (action.isDeleted()) {
					return true;
				}
			}
		}
		return false;
	}
	public static boolean hasNonDeleted(Action[] actions) {
		if (actions!=null) { 
			for (Action action : actions) {
				if (!action.isDeleted()) {
					return true;
				}
			}
		}
		return false;
	}

	private int id;
	private Date created;
	private Date resolved;
	private Date modified;
	private String description;
	private Date start;
	private Date remind;
	private Date due;
	private Integer project;
	private boolean queued= false;
	
	private transient Resolution resolution = Resolution.OPEN;
	private Integer resolutionId = Resolution.OPEN.ordinal();
	private transient ActionType type;
	private Integer typeId;
	private transient Priority priority= Priority.None;
	private Integer priorityId= Priority.None.ordinal();
	private transient URL url;
	private String urlId;
	private transient ActionProxy proxy;

	public Action(int id, Date created, Date resolved, String description, Date modified) {
		this.id=id;
		this.created=created;
		this.modified=modified;
		this.resolved=resolved;
		this.description = description;
	}
	public Action(int id, Date created, Date resolved, String description) {
		this(id,created,resolved,description,new Date());
	}
	
	public boolean isOpen() {
		return getResolution()==Resolution.OPEN;
	}


	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}


	/**
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}


	/**
	 * @return the owner
	 */
	public Folder getParent() {
		if (proxy!=null) {
			return proxy.getParent();
		}
		return null;
	}
	
	public Date getModified() {
		return modified;
	}


	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		if (description!=null && description.equals(this.description)) {
			return;
		}
		if (this.description!=null && this.description.equals(description)) {
			return;
		}
		String old= this.description;
		this.description = description;
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,"description",old,description); //$NON-NLS-1$
	}


	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * @param resolution the resolution to set
	 */
	public void setResolution(Resolution resolution) {
		if (this.getResolution()==resolution) {
			return;
		}
		Resolution old= this.resolution;
		this.resolution = resolution;
		this.resolutionId = resolution.ordinal();
		if (resolved==null && !isOpen()) {
			resolved= new Date();
		} else if (isOpen()) {
			resolved=null;
		}
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,RESOLUTION_PROPERTY_NAME,old,resolution);
		setQueued(false);
	}


	/**
	 * @return the resolution
	 */
	public Resolution getResolution() {
		if (resolution==null && resolutionId!=null) {
			resolution= Resolution.values()[resolutionId];
		}
		return resolution;
	}


	/**
	 * @return the resolved
	 */
	public Date getResolved() {
		return resolved;
	}

	/**
	 * @return the start
	 */
	public Date getStart() {
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(Date start) {
		Date old= this.start;
		this.start = start;
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,"start",old,start); //$NON-NLS-1$
	}

	/**
	 * @return the type
	 */
	public ActionType getType() {
		if (type==null && typeId!=null) {
			type= ActionType.values()[typeId];
		}
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(ActionType type) {
		if (getType() == type) {
			return;
		}
		ActionType old= this.type;
		this.type = type;
		this.typeId = type.ordinal();
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,"type",old,type); //$NON-NLS-1$
	}

	/**
	 * @return the url
	 */
	public URL getUrl() {
		if (url==null && urlId!=null) {
			try {
				url= new URL(urlId);
			} catch (MalformedURLException e) {
				urlId=null;
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			}
		}
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(URL url) {
		if ((this.url==null && url==null) || (this.url!=null && url!=null && this.url.toString().equals(url.toString()))) {
			return;
		}
		URL old= this.url;
		this.url = url;
		if (url!=null) {
			this.urlId = url.toString();
		} else {
			urlId=null;
		}
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,"url",old,url); //$NON-NLS-1$
	}
	
	private void modified() {
		modified= new Date();
	}

	/**
	 * @return the priority
	 */
	public Priority getPriority() {
		if (priority==null && priorityId!=null) {
			priority= Priority.values()[priorityId];
		}
		return priority;
	}

	/**
	 * @param priority the priority to set
	 */
	public void setPriority(Priority priority) {
		if (priority==null) priority=Priority.None;
		if (this.priority == priority) {
			return;
		}
		Priority old= this.priority;
		this.priority = priority;
		this.priorityId = priority.ordinal();
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,PRIORITY_PROPERTY_NAME,old,priority);

	}

	/**
	 * @return the remind
	 */
	public Date getRemind() {
		return remind;
	}

	/**
	 * @param remind the remind to set
	 */
	public void setRemind(Date remind) {
		Date old= this.remind;
		this.remind = remind;
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,REMIND_PROPERTY_NAME,old,remind);
	}

	/**
	 * @return the due
	 */
	public Date getDue() {
		return due;
	}

	/**
	 * @param due the due to set
	 */
	public void setDue(Date due) {
		Date old= this.due;
		this.due = due;
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,"due",old,due); //$NON-NLS-1$
	}

	/**
	 * @param folder the folder to set
	 */
	void setParent(Folder folder) {
		if (proxy!=null) {
			proxy.setParent(folder);
		} else {
			if (folder!=null) {
				proxy= folder.getParent().getDataRepository().getProxy(this);
				proxy.setParent(folder);
			}
		}
	}
	
	public void moveUp() {
		getParent().moveUp(this);
	}
	
	public void moveDown() {
		getParent().moveDown(this);
	}

	public boolean canMoveUp() {
		return getParent().canMoveUp(this);
	}

	public boolean canMoveDown() {
		return getParent().canMoveDown(this);
	}
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append("Action={id="); //$NON-NLS-1$
		sb.append(id);
		sb.append(",resolution="); //$NON-NLS-1$
		sb.append(getResolution());
		sb.append("}"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * @return the project
	 */
	public Integer getProject() {
		return project;
	}

	/**
	 * @param project the project to set
	 */
	public void setProject(Integer project) {
		if (project!=null && project.equals(this.project)) {
			return;
		}
		if (this.project!=null && this.project.equals(project)) {
			return;
		}
		if (this.project==null && project==null) {
			return;
		}
		Integer old= this.project;
		this.project = project;
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,PROJECT_PROPERTY_NAME,old,project);
	}
	
	public void copy(Action a) {
		setDescription(a.getDescription());
		setDue(a.getDue());
		setPriority(a.getPriority());
		setRemind(a.getRemind());
		setResolution(a.getResolution());
		setStart(a.getStart());
		setType(a.getType());
		setUrl(a.getUrl());
		setQueued(a.isQueued());
	}

	/**
	 * @return the queued
	 */
	public boolean isQueued() {
		return queued;
	}

	/**
	 * @param queued the queued to set
	 */
	public void setQueued(boolean queued) {
		if (queued==this.queued) {
			return;
		}
		this.queued = queued;
		modified();
		if (getParent()!=null) getParent().fireElementModified(this,proxy,QUEUED_PROPERTY_NAME,!queued,queued);
	}

	public boolean isResolved() {
		return getResolution()==Resolution.RESOLVED;
	}

	public boolean isDeleted() {
		return getResolution()==Resolution.DELETED;
	}

	/**
	 * @return the proxy
	 */
	ActionProxy getProxy() {
		return proxy;
	}

	/**
	 * @param proxy the proxy to set
	 */
	void setProxy(ActionProxy proxy) {
		this.proxy = proxy;
	}
}
