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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.gtdfree.GlobalProperties;
import org.gtdfree.model.Folder.FolderPreset;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public class GTDDataDefault implements GTDData {

	class Proxy implements ActionProxy {
		
		Action action;
		Folder parent;
		
		public Proxy(Action a) {
			action=a;
			action.setProxy(this);
		}
		
		@Override
		public Action get() {
			return action;
		}
		
		@Override
		public int getId() {
			return action.getId();
		}
		@Override
		public void store() {
		}
		@Override
		public void delete() {
		}
		@Override
		public Folder getParent() {
			return parent;
		}
		
		public void setParent(Folder parent) {
			this.parent=parent;
		}
		
		@Override
		public String toString() {
			return action != null ? action.toString() : "NULL"; //$NON-NLS-1$
		}
	}
	
	class FolderData implements FolderDataProxy {
		
		private class ProxyComparator implements Comparator<ActionProxy> {
			Comparator<Action> c;
			public ProxyComparator(Comparator<Action> c) {
				this.c=c;
			}
			@Override
			public int compare(ActionProxy o1, ActionProxy o2) {
				return c.compare(o1.get(), o2.get());
			}
		}

		private List<ActionProxy> actions= new ArrayList<ActionProxy>();
		private String description;

		private Date created;
		private Date resolved;
		private Date modified;
		
		public FolderData() {
		}
		
		
		/**
		 * @return the created
		 */
		public Date getCreated() {
			return created;
		}


		/**
		 * @param created the created to set
		 */
		public void setCreated(Date created) {
			this.created = created;
		}


		/**
		 * @return the resolved
		 */
		public Date getResolved() {
			return resolved;
		}


		/**
		 * @param resolved the resolved to set
		 */
		public void setResolved(Date resolved) {
			this.resolved = resolved;
		}


		/**
		 * @return the modified
		 */
		public Date getModified() {
			return modified;
		}
		
		private void modify() {
			modified= new Date();
		}


		/**
		 * @param modified the modified to set
		 */
		public void setModified(Date modified) {
			this.modified = modified;
		}

		@Override
		public void delete() {
		}
		@Override
		public String getDescription() {
			return description;
		}
		@Override
		public void setDescription(String desc) {
			description=desc;
			modify();
		}
		@Override
		public void store() {
		}
		@Override
		public boolean contains(ActionProxy ap) {
			return actions.contains(ap);
		}
		@Override
		public ActionProxy get(int i) {
			return actions.get(i);
		}
		@Override
		public int size() {
			return actions.size();
		}
		@Override
		public Iterator<ActionProxy> iterator(FolderPreset fp) {
			if (fp==FolderPreset.OPEN) {
				return new Iterator<ActionProxy>() {
					Iterator<ActionProxy> i= actions.iterator();
					ActionProxy next;
					@Override
					public boolean hasNext() {
						if (next==null) {
							next=_next();
							if (next==null) {
								return false;
							}
						}
						return true;
					}
					public ActionProxy _next() {
						if (i.hasNext()) {
							ActionProxy ap= i.next();
							if (!ap.get().isOpen()) {
								return _next();
							}
							return ap;
						}
						return null;
					}
					@Override
					public ActionProxy next() {
						if (!hasNext()) {
							return null;
						}
						ActionProxy ap= next;
						next=null;
						return ap;
					}
					@Override
					public void remove() {
					}
				};
			}
			return actions.iterator();
		}
		
		public void setFolder(@SuppressWarnings("unused") Folder folder) {
		}
		@Override
		public void add(ActionProxy ap) {
			actions.add(ap);
			modify();
		}
		@Override
		public void add(int i, ActionProxy ap) {
			actions.add(i, ap);
			modify();
		}
		@Override
		public void clear() {
			actions.clear();
			modify();
		}
		@Override
		public boolean remove(ActionProxy i) {
			modify();
			return actions.remove(i);
		}
		@Override
		public boolean remove(int i) {
			modify();
			return actions.remove(i)!=null;
		}
		@Override
		public void set(int i, ActionProxy actionProxy) {
			actions.set(i, actionProxy);
			modify();
		}
		@Override
		public void sort(Comparator<Action> comparator) {
			Collections.sort(actions,new ProxyComparator(comparator));
			modify();
		}
		@Override
		public ActionProxy[] toArray() {
			return actions.toArray(new ActionProxy[actions.size()]);
		}

		@Override
		public void suspend(boolean b) {
		}
		
		@Override
		public String toString() {
			return actions.toString();
		}


		@Override
		public void reorder(Action[] order) {
			for (int i = 0; i < order.length; i++) {
				actions.remove(order[i].getProxy());
			}
			for (int i = 0; i < order.length; i++) {
				if (actions.size()>i) {
					actions.set(i,order[i].getProxy());
				} else {
					actions.add(order[i].getProxy());
				}
			}
		}
	}

	private GTDModel model;
	private boolean closed=false;
	
	
	public GTDDataDefault() {
	}

	public GTDDataDefault(GTDModel m) {
		model=m;
	}
	
	@Override
	public boolean isClosed() {
		return closed;
	}
	
	/* (non-Javadoc)
	 * @see org.gtdfree.model.GTDDataRepository#close()
	 */
	@Override
	public boolean close(boolean t) throws IOException {
		closed=true;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.GTDDataRepository#getProxy(org.gtdfree.model.Action)
	 */
	@Override
	public ActionProxy getProxy(Action a) {
		if (a.getProxy()!=null) {
			return a.getProxy();
		}
		return new Proxy(a);
	}
	
	/* (non-Javadoc)
	 * @see org.gtdfree.model.GTDDataRepository#initialize(org.gtdfree.GTDFreeEngine)
	 */
	@Override
	public void initialize(File f, GlobalProperties gp) {
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.GTDDataRepository#restore()
	 */
	@Override
	public GTDModel restore() throws IOException {
		if (model==null) {
			model= new GTDModel(null);
		}
		return model;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.GTDDataRepository#save()
	 */
	@Override
	public void flush() throws IOException {
	}

	@Override
	public void store() {
	}
	
	@Override
	public ActionProxy newAction(int id, Date created, Date resolved,
			String description) {
		return new Proxy(new Action(id,created,resolved,description));
	}
	
	@Override
	public Folder newFolder(int id, String name, FolderType type) {
		Folder f;
		FolderData fd= new FolderData();
		if (type==FolderType.PROJECT) {
			f= new Project(model,id, name, fd);
		} else {
			f= new Folder(model,id, name, type,fd);
		}
		fd.setFolder(f);
		return f;
	}
	
	@Override
	public void suspend(boolean b) {
	}

	@Override
	public ActionProxy newAction(int id, Action aa, Integer project) {
		Action a= new Action(id,aa.getCreated(),aa.getResolved(),aa.getDescription());
		a.copy(aa);
		a.setProject(project);
		return getProxy(a);
	}
	
	@Override
	public void checkConsistency(Logger log, boolean fail, boolean correct)
			throws ConsistencyException {
	}
	
	@Override
	public String getDatabaseType() {
		return "Dummy"; //$NON-NLS-1$
	}

}
