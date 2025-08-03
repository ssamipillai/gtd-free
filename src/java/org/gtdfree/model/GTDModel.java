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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.gtdfree.Messages;
import org.gtdfree.model.ActionEvent.SortedElements;
import org.gtdfree.model.ActionEvent.SortedElements.ActionIndex;
import org.gtdfree.model.Folder.FolderType;
import org.gtdfree.model.GTDData.ActionProxy;


public class GTDModel implements Iterable<Folder> {
	
	public static class TotalIterator implements Iterator<Object> {
		
		private Iterator<Folder> folders;
		private Iterator<Action> actions;
		private Folder folder;
		private Action action;
		private ActionFilter filter;
		private boolean folderConsumed=false;

		public TotalIterator(Iterator<Folder> i, ActionFilter f) {
			folders=i;
			filter=f;
		}
		
		@Override
		public boolean hasNext() {
			if (folder==null) {
				if (!folders.hasNext()) {
					return false;
				}
				folder= folders.next();
				if (!filter.isAcceptable(folder, null)) {
					folder=null;
					return hasNext();
				}
				folderConsumed=false;
				return true;
			}
			if (actions==null) {
				actions= folder.iterator();
			}
			if (action!=null) {
				return true;
			}
			boolean b= actions.hasNext();
			if (b) {
				action= actions.next();
				if (!filter.isAcceptable(folder, action)) {
					action=null;
					return hasNext();
				}
				return true;
			}
			if (!b) {
				folder=null;
				actions=null;
				return hasNext();
			}
			return b;
		}
		
		@Override
		public Object next() {
			if (!hasNext()) {
				return null;
			}
			if (!folderConsumed) {
				folderConsumed=true;
				return folder;
			}
			Action a= action;
			action=null;
			return a;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	public static final void checkConsistency(GTDModel m, Logger log, boolean fail, boolean correct) throws ConsistencyException {
		
		//Map<Integer, Folder> ids2Folders= new HashMap<Integer, Folder>();
		Map<Integer, Folder> actions2Folders= new HashMap<Integer, Folder>();
		Set<Integer> project= new HashSet<Integer>();
		Set<Integer> resolved= new HashSet<Integer>();
		Set<Integer> reminder= new HashSet<Integer>();
		Set<Integer> deleted= new HashSet<Integer>();
		Set<Integer> priority= new HashSet<Integer>();

		int lastIDA=0;
		int lastIDF=0;

		boolean restart=true;
		
		while (restart) {
			
			restart=false;
			
			actions2Folders.clear();
			project.clear();
			resolved.clear();
			reminder.clear();
			deleted.clear();
			priority.clear();
			
			if (lastIDF > m.lastFolderID) {
				if (log!=null) {
					log.log(Level.WARNING, "Internal inconsistency, highest folder ID not property registered."); //$NON-NLS-1$
				}
				m.lastFolderID=lastIDF;
			}

			if (lastIDA > m.lastActionID) {
				if (log!=null) {
					log.log(Level.WARNING, "Internal inconsistency, highest action ID not properly registered."); //$NON-NLS-1$
				}
				m.lastActionID=lastIDA;
			}
			
			for (Folder f : m) {
				
				// save last ID
				if (f.getId()>lastIDF) {
					lastIDF=f.getId();
				}
				
				// check if parent model is set correctly
				if (f.getParent()!=m) {
					ConsistencyException e= new ConsistencyException("Folder has no reference to model.", null, new Folder[]{f},null); //$NON-NLS-1$
					if (fail) {
						throw e;
					}
					log.log(Level.WARNING, "Folder '"+f.getName()+"' has no reference to model.", e); //$NON-NLS-1$ //$NON-NLS-2$
					if (correct) {
						f.setParent(m);
						log.log(Level.INFO, "Parent set to folder  '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}

				// go trough all folders which are primary containers for actions, not index folders
				if (!f.isMeta()) {
					for (int i=0; i< f.size(); i++) {
						Action a = f.get(i);
						
						
						// Action may be null because database is corrupted
						if (a==null) {
							ConsistencyException e= new ConsistencyException("Action at position '"+i+"' is null.", new Action[]{}, new Folder[]{f},null); //$NON-NLS-1$ //$NON-NLS-2$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action at position '"+i+"' is null.", e); //$NON-NLS-1$ //$NON-NLS-2$
							// null actions are too dangerous to hang around, they can be removed immediately without side effects 
							if (correct || true) {
								ActionProxy ap = f.getProxy(i);
								f.remove(i);
								m.removeDeleted(null, ap);
								ap.delete();
								log.log(Level.INFO, "Null action at position '"+i+"' in '"+f.getName()+"' is removed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								i--;
								continue;
							}
						}
						
						// save highest action ID
						if (a.getId()>lastIDA) {
							lastIDA= a.getId();
						}
						
						if (a.getProxy()==null) {
							ConsistencyException e= new ConsistencyException("Action with ID '"+a.getId()+"' has no internal proxy.", new Action[]{a}, new Folder[]{f},null); //$NON-NLS-1$ //$NON-NLS-2$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action with ID '"+a.getId()+"' has no internal proxy.", e); //$NON-NLS-1$ //$NON-NLS-2$
							if (correct) {
								a.setProxy(m.getDataRepository().getProxy(a));
								log.log(Level.INFO, "Proxy set to action with ID '"+a.getId()+"'."); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
						
						if (a.getParent()!=f) {
							ConsistencyException e= new ConsistencyException("Action with ID '"+a.getId()+"' has inconsistent parent list reference.", new Action[]{a}, new Folder[]{f},null); //$NON-NLS-1$ //$NON-NLS-2$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action with ID '"+a.getId()+"' has inconsistent parent list reference.", e); //$NON-NLS-1$ //$NON-NLS-2$
							if (correct) {
								Folder f2= a.getParent();
								ActionProxy ap= m.getDataRepository().getProxy(a);
								if (f2!=null) {
									f2.remove(a,ap);
								}
								a.setParent(f);
								log.log(Level.INFO, "Action with ID '"+a.getId()+"' got reference to correct list."); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}

						if (actions2Folders.containsKey(a.getId())) {
							Folder f2= actions2Folders.get(a.getId());
							Action a2= f2.getActionByID(a.getId());
							if (a2==a) {
								ConsistencyException e= new ConsistencyException("Action is registered in two lists.", new Action[]{a}, new Folder[]{f,f2},null); //$NON-NLS-1$
								if (fail) {
									throw e;
								}
								log.log(Level.WARNING, "Action with ID '"+a.getId()+"' is registered in two lists.", e); //$NON-NLS-1$ //$NON-NLS-2$
								if (correct) {
									Folder f3= a.getParent();
									if (f3==f) {
										f2.remove(a,m.getDataRepository().getProxy(a));
										log.log(Level.INFO, "Action with ID '"+a.getId()+"' is only in list '"+f.getName()+"' and removed from others."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									} else if (f3==f2) {
										f.remove(a,m.getDataRepository().getProxy(a));
										log.log(Level.INFO, "Action with ID '"+a.getId()+"' is only in list '"+f2.getName()+"' and removed from others."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									} else {
										ActionProxy ap= m.getDataRepository().getProxy(a); 
										f2.remove(a,ap);
										f.remove(a,ap);
										f3.remove(a,ap);
										m.getInBucketFolder().add(a, ap);
										log.log(Level.INFO, "Action with ID '"+a.getId()+"' has been moved to InBucket."); //$NON-NLS-1$ //$NON-NLS-2$
									}
									restart=true;
									break;
								}
							} else {
								ConsistencyException e= new ConsistencyException("Two action has same ID.", new Action[]{a,a2}, new Folder[]{f,f2},null); //$NON-NLS-1$
								if (fail) {
									throw e;
								}
								log.log(Level.WARNING, "Two action has same ID '"+a.getId()+"'.", e); //$NON-NLS-1$ //$NON-NLS-2$
								if (correct) {
									ActionProxy ap= m.getDataRepository().getProxy(a);
									f.remove(a,ap);
									Action a3= m.createActionCopy(f, a, a.getProject());
									m.removeDeleted(a, ap);
									ap.delete();
									log.log(Level.INFO, "Action was reinserted to folder '"+f.getName()+"' with new ID '"+a3.getId()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									restart=true;
									break;
								}
							}
						}

						actions2Folders.put(a.getId(), f);

						if (a.getProject()!=null) {
							project.add(a.getId());
						}
						if (a.isResolved()) {
							resolved.add(a.getId());
						}
						if (a.isDeleted()) {
							deleted.add(a.getId());
						}
						if (a.getRemind()!=null) {
							reminder.add(a.getId());
						}
						if (a.getPriority()!=null && a.getPriority()!=Priority.None) {
							priority.add(a.getId());
						}

					}
				}
			}
		}
		
		if (lastIDF > m.lastFolderID) {
			log.log(Level.WARNING, "Internal inconsistency, highest folder ID not property registered."); //$NON-NLS-1$
			m.lastFolderID=lastIDF;
		}

		if (lastIDA > m.lastActionID) {
			log.log(Level.WARNING, "Internal inconsistency, highest action ID not properly registered."); //$NON-NLS-1$
			m.lastActionID=lastIDA;
		}

		
		for (Folder f : m) {
			restart=true;
			while (restart) {
				restart=false;
				
				int open=0;
				
				for (int i=0; i< f.size(); i++) {
					Action a = f.get(i);
					
					if (a==null) {
						ConsistencyException e= new ConsistencyException("Action in '"+f.getName()+"'  at position '"+i+"' is null.", new Action[]{}, new Folder[]{f},null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						if (fail) {
							throw e;
						}
						log.log(Level.WARNING, "Action at position '"+i+"' is null.", e); //$NON-NLS-1$ //$NON-NLS-2$
						if (correct || true) {
							ActionProxy ap = f.getProxy(i);
							f.remove(i);
							m.removeDeleted(null, ap);
							ap.delete();
							log.log(Level.INFO, "Null action at position '"+i+"' in '"+f.getName()+"' is removed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							i--;
							continue;
						}
					}
					
					if (a.isOpen()) {
						open++;
					}
					
					// check if action has ben already registered with primary folders, 
					// actions residing only in index folders will fail here
					if (!actions2Folders.containsKey(a.getId())) {
						ConsistencyException e= new ConsistencyException("Action has no defined list.", new Action[]{a}, new Folder[]{f},null); //$NON-NLS-1$
						if (fail) {
							throw e;
						}
						log.log(Level.WARNING, "Action with ID '"+a.getId()+"' has no defined list.", e); //$NON-NLS-1$ //$NON-NLS-2$
						if (correct) {
							Folder f1= a.getParent();
							if (f1==null) {
								f1= m.getInBucketFolder();
							}
							ActionProxy ap= m.getDataRepository().getProxy(a);
							f1.add(a, ap);
							actions2Folders.put(a.getId(), f1);
							
							if (a.getId()>lastIDA) {
								lastIDA=a.getId();
							}
							
							log.log(Level.INFO,"Action with ID '"+a.getId()+"' added to list '"+f1.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
					
					if (f==m.getDeletedFolder()) {
						if (!a.isDeleted()) {
							ConsistencyException e= new ConsistencyException("Action in deleted list is not deleted.", new Action[]{a}, new Folder[]{f},null); //$NON-NLS-1$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action with ID '"+a.getId()+"' in deleted list is not deleted.", e); //$NON-NLS-1$ //$NON-NLS-2$
							if (correct) {
								ActionProxy ap= m.getDataRepository().getProxy(a);
								f.remove(a,ap);
								log.log(Level.INFO,"Action with ID '"+a.getId()+"' removed from list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								restart=true;
								break;
							}
						}
						deleted.remove(a.getId());
					} else 	if (f==m.getResolvedFolder()) {
						if (!a.isResolved()) {
							ConsistencyException e= new ConsistencyException("Action in resolved list is not resolved.", new Action[]{a}, new Folder[]{f},null); //$NON-NLS-1$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action with ID '"+a.getId()+"' in resolved list is not resolved.", e); //$NON-NLS-1$ //$NON-NLS-2$
							if (correct) {
								ActionProxy ap= m.getDataRepository().getProxy(a);
								f.remove(a,ap);
								log.log(Level.INFO,"Action with ID '"+a.getId()+"' removed from list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								restart=true;
								break;
							}
						}
						resolved.remove(a.getId());
					} else if (f==m.getPriorityFolder()) {
						if (a.getPriority()==null || a.getPriority()==Priority.None) {
							ConsistencyException e= new ConsistencyException("Action in priority list has no priority set.", new Action[]{a}, new Folder[]{f},null); //$NON-NLS-1$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action with ID '"+a.getId()+"' in priority list has no priority set.", e); //$NON-NLS-1$ //$NON-NLS-2$
							if (correct) {
								ActionProxy ap= m.getDataRepository().getProxy(a);
								f.remove(a,ap);
								log.log(Level.INFO,"Action with ID '"+a.getId()+"' removed from list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								restart=true;
								break;
							}
						}
						priority.remove(a.getId());
					} else if (f==m.getRemindFolder()) {
						if (a.getRemind() ==null ) {
							ConsistencyException e= new ConsistencyException("Action in reminder list has no reminder set.", new Action[]{a}, new Folder[]{f},null); //$NON-NLS-1$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action with ID '"+a.getId()+"' in remind list has no reminder set.", e); //$NON-NLS-1$ //$NON-NLS-2$
							if (correct) {
								ActionProxy ap= m.getDataRepository().getProxy(a);
								f.remove(a,ap);
								log.log(Level.INFO,"Action with ID '"+a.getId()+"' removed from list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								restart=true;
								break;
							}
						}
						reminder.remove(a.getId());
					} else 	if (f.isProject()) {
						
						if (a.getProject()==null) {
							ConsistencyException e= new ConsistencyException("Action in project list has no project set.", new Action[]{a}, new Folder[]{f},null); //$NON-NLS-1$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action with ID '"+a.getId()+"' in project list has no project set.", e); //$NON-NLS-1$ //$NON-NLS-2$
							if (correct) {
								ActionProxy ap= m.getDataRepository().getProxy(a);
								f.remove(a,ap);
								log.log(Level.INFO,"Action with ID '"+a.getId()+"' removed from list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								restart=true;
								break;
							}
						} else if (!a.getProject().equals(f.getId())) {
							ConsistencyException e= new ConsistencyException("Action's project and action's project list are not same.", new Action[]{a}, new Folder[]{f}, new Project[]{m.getProject(a.getProject())}); //$NON-NLS-1$
							if (fail) {
								throw e;
							}
							log.log(Level.WARNING, "Action with ID '"+a.getId()+"' in is in wrong project list.", e); //$NON-NLS-1$ //$NON-NLS-2$
							if (correct) {
								ActionProxy ap= m.getDataRepository().getProxy(a);
								f.remove(a,ap);
								log.log(Level.INFO,"Action with ID '"+a.getId()+"' removed from list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								restart=true;
								break;
							}
						}
						project.remove(a.getId());
					}
					
					
				}
				
				if (open != f.getOpenCount()) {
					log.log(Level.WARNING, "Internal inconsistency, folder '"+f.getName()+"' has wrong open count."); //$NON-NLS-1$ //$NON-NLS-2$
					f.setOpenCount(open);
				}
			}			
		}
		
		if (lastIDF > m.lastFolderID) {
			log.log(Level.WARNING, "Internal inconsistency, highest folder ID not property registered."); //$NON-NLS-1$
			m.lastFolderID=lastIDF;
		}

		if (lastIDA > m.lastActionID) {
			log.log(Level.WARNING, "Internal inconsistency, highest action ID not properly registered."); //$NON-NLS-1$
			m.lastActionID=lastIDA;
		}

		
		if (project.size()>0) {
			
			Integer[] id= project.toArray(new Integer[project.size()]);
			Action[] a= new Action[id.length];
			
			for (int i = 0; i < a.length; i++) {
				a[i]= m.getAction(id[i]);
			}
			
			ConsistencyException e= new ConsistencyException("Actions with project set are not listed in project lists.", a, null, null); //$NON-NLS-1$
			if (fail) {
				throw e;
			}
			log.log(Level.WARNING, "Actions with IDs '"+Arrays.toString(id)+"' are not in project lists.", e); //$NON-NLS-1$ //$NON-NLS-2$
			if (correct) {
				for (int i = 0; i < a.length; i++) {
					ActionProxy ap= m.getDataRepository().getProxy(a[i]);
					Project p= m.getProject(a[i].getProject());
					p.add(a[i],ap);
					log.log(Level.INFO,"Action with ID '"+a[i].getId()+"' added to project list '"+p.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			
		}
		
		if (deleted.size()>0) {
			
			Integer[] id= deleted.toArray(new Integer[deleted.size()]);
			Action[] a= new Action[id.length];
			
			for (int i = 0; i < a.length; i++) {
				a[i]= m.getAction(id[i]);
			}
			
			ConsistencyException e= new ConsistencyException("Actions with deleted status are not listed in deleted list.", a, null, null); //$NON-NLS-1$
			if (fail) {
				throw e;
			}
			log.log(Level.WARNING, "Actions with IDs '"+Arrays.toString(id)+"' are not in deleted list.", e); //$NON-NLS-1$ //$NON-NLS-2$
			if (correct) {
				Folder f= m.getDeletedFolder();
				for (int i = 0; i < a.length; i++) {
					ActionProxy ap= m.getDataRepository().getProxy(a[i]);
					f.add(a[i],ap);
					log.log(Level.INFO,"Action with ID '"+a[i].getId()+"' added to list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			
		}
		
		if (resolved.size()>0) {
			
			Integer[] id= resolved.toArray(new Integer[resolved.size()]);
			Action[] a= new Action[id.length];
			
			for (int i = 0; i < a.length; i++) {
				a[i]= m.getAction(id[i]);
			}
			
			ConsistencyException e= new ConsistencyException("Actions with resolved status are not listed in resolved list.", a, null, null); //$NON-NLS-1$
			if (fail) {
				throw e;
			}
			log.log(Level.WARNING, "Actions with IDs '"+Arrays.toString(id)+"' are not in resolved list.", e); //$NON-NLS-1$ //$NON-NLS-2$
			if (correct) {
				Folder f= m.getResolvedFolder();
				for (int i = 0; i < a.length; i++) {
					ActionProxy ap= m.getDataRepository().getProxy(a[i]);
					f.add(a[i],ap);
					log.log(Level.INFO,"Action with ID '"+a[i].getId()+"' added to list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			
		}

		if (reminder.size()>0) {
			
			Integer[] id= reminder.toArray(new Integer[reminder.size()]);
			Action[] a= new Action[id.length];
			
			for (int i = 0; i < a.length; i++) {
				a[i]= m.getAction(id[i]);
			}
			
			ConsistencyException e= new ConsistencyException("Actions with reminder are not listed in reminder list.", a, null, null); //$NON-NLS-1$
			if (fail) {
				throw e;
			}
			log.log(Level.WARNING, "Actions with IDs '"+Arrays.toString(id)+"' are not in remind list.", e); //$NON-NLS-1$ //$NON-NLS-2$
			if (correct) {
				Folder f= m.getRemindFolder();
				for (int i = 0; i < a.length; i++) {
					ActionProxy ap= m.getDataRepository().getProxy(a[i]);
					f.add(a[i],ap);
					log.log(Level.INFO,"Action with ID '"+a[i].getId()+"' added to list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			
		}

		if (priority.size()>0) {
			
			Integer[] id= priority.toArray(new Integer[priority.size()]);
			Action[] a= new Action[id.length];
			
			for (int i = 0; i < a.length; i++) {
				a[i]= m.getAction(id[i]);
			}
			
			ConsistencyException e= new ConsistencyException("Actions with priority are not listed in priority list.", a, null, null); //$NON-NLS-1$
			if (fail) {
				throw e;
			}
			log.log(Level.WARNING, "Actions with IDs '"+Arrays.toString(id)+"' are not in priority list.", e); //$NON-NLS-1$ //$NON-NLS-2$
			if (correct) {
				Folder f= m.getPriorityFolder();
				for (int i = 0; i < a.length; i++) {
					ActionProxy ap= m.getDataRepository().getProxy(a[i]);
					f.add(a[i],ap);
					log.log(Level.INFO,"Action with ID '"+a[i].getId()+"' added to list '"+f.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			
		}

	}

	
	static class ModelListenerSupport implements GTDModelListener {
		private EventListenerList listeners= new EventListenerList();
		private GTDModel model;
		
		public ModelListenerSupport(GTDModel model) {
			this.model=model;
		}
		
		public void addlistener(GTDModelListener l) {
			listeners.add(GTDModelListener.class,l);
		}
		public void removelistener(GTDModelListener l) {
			listeners.remove(GTDModelListener.class,l);
		}
		
		void checkEvent(ActionEvent e) {
			if (e.getNewValue()==e.getOldValue()) {
				throw new RuntimeException("Internal error, property not changed: "+e.toString()); //$NON-NLS-1$
			}
		}
		
		void updateMetaAdd(FolderEvent a) {
			
			SortedElements se= a.getSortedElements();
			
			if (se.size(ActionIndex.RESOLVED)>0) {
				model.resolved.add(se.getActions(ActionIndex.RESOLVED), se.getActionProxies(ActionIndex.RESOLVED));
			}
			if (se.size(ActionIndex.DELETED)>0) {
				model.deleted.add(se.getActions(ActionIndex.DELETED), se.getActionProxies(ActionIndex.DELETED));
			}
			if (se.size(ActionIndex.REMINDER)>0) {
				model.reminder.add(se.getActions(ActionIndex.REMINDER), se.getActionProxies(ActionIndex.REMINDER));
			}
			if (se.size(ActionIndex.PRIORITY)>0) {
				model.priority.add(se.getActions(ActionIndex.PRIORITY), se.getActionProxies(ActionIndex.PRIORITY));
			}
			if (se.size(ActionIndex.QUEUE)>0 && !model.suspendedForMultipleChanges) {
				model.queue.add(se.getActions(ActionIndex.QUEUE), se.getActionProxies(ActionIndex.QUEUE));
			}
			if (se.size(ActionIndex.PROJECT)>0) {
				Action[] aac= se.getActions(ActionIndex.PROJECT);
				ActionProxy[] aap= se.getActionProxies(ActionIndex.PROJECT);
				for (int j = 0; j < aac.length; j++) {
					if (getProject(aac[j].getProject())!=null) {
						getProject(aac[j].getProject()).add(aac[j], aap[j]);
					}
				}
			}
		}
		
		private Project getProject(Integer project) {
			return model.projects.get(project);
		}
		
		void updateMetaRemove(FolderEvent a) {
			if (a.getFolder()==model.deleted) {
				model.removeDeleted(a.getSortedElements());
			}				
		}
		void updateMetaModify(ActionEvent a) {
			SortedElements se= a.getSortedElements();
			if (a.getProperty().equals(Action.RESOLUTION_PROPERTY_NAME)) {
				model.resolved.add(se.getActions(ActionIndex.RESOLVED),se.getActionProxies(ActionIndex.RESOLVED));
				model.resolved.remove(se.getActionsInv(ActionIndex.RESOLVED),se.getActionProxiesInv(ActionIndex.RESOLVED));
				model.deleted.add(se.getActions(ActionIndex.DELETED),se.getActionProxies(ActionIndex.DELETED));
				model.deleted.remove(se.getActionsInv(ActionIndex.DELETED),se.getActionProxiesInv(ActionIndex.DELETED));
			}
			if (a.getProperty().equals(Action.REMIND_PROPERTY_NAME)) {
				model.reminder.add(se.getActions(ActionIndex.REMINDER),se.getActionProxies(ActionIndex.REMINDER));
				model.reminder.remove(se.getActionsInv(ActionIndex.REMINDER),se.getActionProxiesInv(ActionIndex.REMINDER));
			}
			if (a.getProperty().equals(Action.PRIORITY_PROPERTY_NAME)) {
				model.priority.add(se.getActions(ActionIndex.PRIORITY),se.getActionProxies(ActionIndex.PRIORITY));
				model.priority.remove(se.getActionsInv(ActionIndex.PRIORITY),se.getActionProxiesInv(ActionIndex.PRIORITY));
			}
			if (a.getProperty().equals(Action.QUEUED_PROPERTY_NAME) && !model.suspendedForMultipleChanges) {
				model.queue.add(se.getActions(ActionIndex.QUEUE),se.getActionProxies(ActionIndex.QUEUE));
				model.queue.remove(se.getActionsInv(ActionIndex.QUEUE),se.getActionProxiesInv(ActionIndex.QUEUE));
			}
			if (a.getProperty().equals(Action.PROJECT_PROPERTY_NAME)) {
				if (a.getOldValue()!=null) {
					getProject((Integer)a.getOldValue()).remove(se.getActions(),se.getActionProxies());
				}
				if (a.getNewValue()!=null) {
					getProject((Integer)a.getNewValue()).add(se.getActions(),se.getActionProxies());
				}
			}
		}
		
		public void elementAdded(FolderEvent a) {
			updateMetaAdd(a);
			GTDModelListener[] l= listeners.getListeners(GTDModelListener.class);
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].elementAdded(a);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
			}
		}
		public void elementModified(ActionEvent a) {
			checkEvent(a);
			if (!((Folder)a.getSource()).isMeta()) {
				updateMetaModify(a);
				// rethrow events for meta folders
				model.queue.fireElementModified(a.getSortedElements().getActions(ActionIndex.QUEUE), a.getSortedElements().getActionProxies(ActionIndex.QUEUE),a.getProperty(),a.getOldValue(),a.getNewValue(),true);
				model.reminder.fireElementModified(a.getSortedElements().getActions(ActionIndex.REMINDER), a.getSortedElements().getActionProxies(ActionIndex.REMINDER),a.getProperty(),a.getOldValue(),a.getNewValue(),true);
				model.priority.fireElementModified(a.getSortedElements().getActions(ActionIndex.PRIORITY), a.getSortedElements().getActionProxies(ActionIndex.PRIORITY),a.getProperty(),a.getOldValue(),a.getNewValue(),true);
				Action[] aac= a.getSortedElements().getActions(ActionIndex.PROJECT);
				ActionProxy[] aap= a.getSortedElements().getActionProxies(ActionIndex.PROJECT);
				for (int j = 0; j < aac.length; j++) {
					Project p= getProject(aac[j].getProject());
					if (p!=null) {
						p.fireElementModified(aac[j], aap[j],a.getProperty(),a.getOldValue(),a.getNewValue(),true);
					}
				}
			}
			GTDModelListener[] l= listeners.getListeners(GTDModelListener.class);
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].elementModified(a);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
			}
		}
		public void elementRemoved(FolderEvent a) {
			updateMetaRemove(a);
			GTDModelListener[] l= listeners.getListeners(GTDModelListener.class);
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].elementRemoved(a);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
			}
		}
		public void folderAdded(Folder folder) {
			GTDModelListener[] l= listeners.getListeners(GTDModelListener.class);
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].folderAdded(folder);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
			}
		}
		public void folderModified(Folder f, String p, Object o, Object n, boolean recycled) {
			folderModified(new FolderEvent(f,(Action[])null,(ActionProxy[])null,p,o,n,recycled));
		}
		public void folderModified(FolderEvent folder) {
			GTDModelListener[] l= listeners.getListeners(GTDModelListener.class);
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].folderModified(folder);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
			}
		}
		public void folderRemoved(Folder folder) {
			GTDModelListener[] l= listeners.getListeners(GTDModelListener.class);
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].folderRemoved(folder);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
			}
		}
		public void orderChanged(Folder f) {
			GTDModelListener[] l= listeners.getListeners(GTDModelListener.class);
			for (int i = 0; i < l.length; i++) {
				try {
					l[i].orderChanged(f);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
			}
		}
	}
	
	
	public Action createAction(Folder f, String desc) {
		ActionProxy ap= getDataRepository().newAction(++lastActionID,new Date(),null,desc);
		Action a= ap.get();
		f.add(0, a, ap);
		return a;
	}

	public void removeDeleted(Action a, ActionProxy ap) {
		
		deleted.remove(a,ap);
		
		if (a==null || a.getRemind()!=null) {
			reminder.remove(a,ap);
		}
		
		if (a==null || (a.getPriority()!=null && a.getPriority()!=Priority.None)) {
			priority.remove(a,ap);
		}
		
		if (a==null || a.isQueued()) {
			queue.remove(a,ap);
		}
		
		if (a!=null && a.getProject()!=null && getProject(a.getProject())!=null) {
			getProject(a.getProject()).remove(a,ap);
		}
		
	}

	public void removeDeleted(SortedElements se) {
		
		deleted.remove(se.getActions(), se.getActionProxies());
		reminder.remove(se.getActions(ActionIndex.REMINDER),se.getActionProxies(ActionIndex.REMINDER));
		priority.remove(se.getActions(ActionIndex.PRIORITY),se.getActionProxies(ActionIndex.PRIORITY));
		queue.remove(se.getActions(ActionIndex.QUEUE),se.getActionProxies(ActionIndex.QUEUE));
		
		Action[] ac= se.getActions(ActionIndex.PROJECT);
		ActionProxy[] ap= se.getActionProxies(ActionIndex.PROJECT);
		
		for (int i = 0; i < ac.length; i++) {
			Project p= getProject(ac[i].getProject());
			if (p!=null) {
				p.remove(ac[i], ap[i]);
			}
		}
	}

	public Action createActionCopy(Folder f, Action aa, Integer project) {
		ActionProxy ap= getDataRepository().newAction(++lastActionID,aa, project);
		Action a= ap.get();
		f.add(0, a, ap);
		return a;
	}

	private Map<Integer,Folder> folders= new HashMap<Integer,Folder>();
	private Map<Integer,Project> projects= new HashMap<Integer,Project>();
	private int lastActionID=0; 
	private int lastFolderID=0; 
	private transient ModelListenerSupport support= new ModelListenerSupport(this);
	private Folder resolved;
	private Folder deleted;
	private Folder reminder;
	private Folder inBucket;
	private boolean suspendedForMultipleChanges = false;
	private Folder queue;
	private Folder priority;
	private transient GTDData dataRepository;
	
	/**
	 * This constructor creates empty and uninitialized instance. 
	 * Call <code>initialize()</code> before using, or "things" may happen.
	 */
	public GTDModel() {
		super();
		//Thread.dumpStack();
	}
	
	/**
	 * Crates new instance initialized to provided data storage.
	 * 
	 * @param data store for GTDModel with all elements, 
	 * if <code>null</code> then by default internal dummy storage is used, which stores model in memory.
	 */
	public GTDModel(GTDData data) {
		this();
		initialize(data);
	}
	
	
	public void initialize(GTDData data) {
		dataRepository =data;
		createMetaFolders();
	}

	/**
	 * 
	 */
	private void createMetaFolders() {
		if (resolved==null || !folders.containsKey(-1)) {
			resolved= createFolder(-1, Messages.getString("GTDModel.Resolved"), FolderType.BUILDIN_RESOLVED); //$NON-NLS-1$
			resolved.setDescription(Messages.getString("GTDModel.Resolved.desc")); //$NON-NLS-1$
			resolved.setComparator(new Comparator<Action>() {
			
				public int compare(Action o1, Action o2) {
					return o1.getId()-o2.getId();
				}
			
			});
		}
		if (reminder==null || !folders.containsKey(-2)) {
			reminder= createFolder(-2, Messages.getString("GTDModel.Tickler"), FolderType.BUILDIN_REMIND); //$NON-NLS-1$
			reminder.setDescription(Messages.getString("GTDModel.Tickler.desc")); //$NON-NLS-1$
			reminder.setComparator(new Comparator<Action>() {
			
				public int compare(Action o1, Action o2) {
					if (o1.getRemind()==null && o2.getRemind()==null) {
						return 0;
					}
					if (o1.getRemind()==null) {
						return -1;
					}
					if (o2.getRemind()==null) {
						return 1;
					}
					return o1.getRemind().compareTo(o2.getRemind());
				}
			
			});
		}
		if (inBucket==null || !folders.containsKey(-3)) {
			inBucket= createFolder(-3, Messages.getString("GTDModel.InB"), FolderType.INBUCKET); //$NON-NLS-1$
			inBucket.setDescription(Messages.getString("GTDModel.InB.desc")); //$NON-NLS-1$
		}
		if (queue==null || !folders.containsKey(-4)) {
			queue= createFolder(-4, Messages.getString("GTDModel.Queue"), FolderType.QUEUE); //$NON-NLS-1$
			queue.setDescription(Messages.getString("GTDModel.Queue.desc")); //$NON-NLS-1$
		}
		if (priority==null || !folders.containsKey(-5)) {
			priority= createFolder(-5, Messages.getString("GTDModel.Priority"), FolderType.BUILDIN_PRIORITY); //$NON-NLS-1$
			priority.setDescription(Messages.getString("GTDModel.Priority.desc")); //$NON-NLS-1$
			priority.setComparator(new Comparator<Action>() {
				public int compare(Action o1, Action o2) {
					return -o1.getPriority().compareTo(o2.getPriority());
				}
			});
		}
		if (deleted==null || !folders.containsKey(-6)) {
			deleted= createFolder(-6, Messages.getString("GTDModel.Deleted"), FolderType.BUILDIN_DELETED); //$NON-NLS-1$
			deleted.setDescription(Messages.getString("GTDModel.Deleted.desc")); //$NON-NLS-1$
			deleted.setComparator(new Comparator<Action>() {
			
				public int compare(Action o1, Action o2) {
					return o1.getId()-o2.getId();
				}
			
			});
		}
	}
	
	public void addGTDModelListener(GTDModelListener l) {
		support.addlistener(l);
	}
	
	public void removeGTDModelListener(GTDModelListener l) {
		support.removelistener(l);
	}

	public synchronized Folder createFolder(String name, FolderType type) {
		return createFolder(++lastFolderID, name, type);
	}

	synchronized Folder createFolder(int id, String name, FolderType type) {
		Folder f= folders.get(id);
		if (f==null) {
			if (lastFolderID<id) {
				lastFolderID=id;
			}
			f= getDataRepository().newFolder(id,name,type);
			if (type==FolderType.PROJECT) {
				projects.put(id, (Project)f);
			}
			f.addFolderListener(support);
			folders.put(id, f);
			support.folderAdded(f);
			getDataRepository().store();
		}
		return f;
	}
	
	public synchronized void renameFolder(Folder f, String newName) {
		String o= f.getName();
		f.setName(newName);
		support.folderModified(f,"name",o,newName,false); //$NON-NLS-1$
	}

	void fireFolderModified(Folder f, String p, Object o, Object n,boolean recycled) {
		support.folderModified(f,p,o,n,recycled);
	}

	public synchronized Folder removeFolder(int id) {
		Folder f= folders.remove(id);
		if (f!=null) {
			f.removeFolderListener(support);
			support.folderRemoved(f);
		}
		return f;
	}

	
	/**
	 * Iterates over all folders, also default and projects.
	 * @return iterator over all folders
	 */
	public Iterator<Folder> iterator() {
		return folders.values().iterator();
	}
	
	/**
	 * Iterates over all folders, also default and projects, and all actions.
	 * Returned object can be folder or project or action.
	 * @return iterator over all folders and actions
	 */
	public Iterator<Object> iterator(ActionFilter filter) {
		return new TotalIterator(folders.values().iterator(),filter);
	}

	public int size() {
		return folders.size();
	}
	
	public Action getAction(int id) {
		for (Folder f : this) {
			Action a= f.getActionByID(id);
			if (a!=null) {
				return a;
			}
		}
		return null;
	}
	
	public boolean moveAction(Action action, Folder toFolder) {
		Folder f= action.getParent();
		ActionProxy ap= getDataRepository().getProxy(action);
		if (f!=null && toFolder!=null && f!=toFolder && !toFolder.contains(ap)) {
			toFolder.add(0,action,ap);
			f.remove(action,ap);
			return true;
		}
		return false;
		
	}
	public boolean moveActions(Action[] actions, Folder toFolder) {
		
		List<Action> a= new LinkedList<Action>(Arrays.asList(actions));
		List<Folder> f= new LinkedList<Folder>();
		
		ListIterator<Action> i= a.listIterator();
		
		while (i.hasNext()) {
			Action aa = i.next();
			Folder ff= aa.getParent();
			if (f==null || toFolder==null || ff==toFolder || toFolder.contains(aa.getProxy())) {
				i.remove();
			} else {
				f.add(ff);
			}
		}
		
		if (a.size()==0) {
			return false;
		}
		
		toFolder.add(0,a.toArray(new Action[a.size()]));
		
		Iterator<Folder> fi= f.iterator();
		for (Action aa : a) {
			fi.next().remove(aa, aa.getProxy());
		}
		return true;
	}
	/**
	 * Returns array of all folders, also default and projects.
	 * @return array of all folders
	 */
	public synchronized Folder[] toFoldersArray() {
		return folders.values().toArray(new Folder[folders.size()]);
	}
	/**
	 * Returns array of all Projects.
	 * @return array of all Projects
	 */
	public synchronized Project[] toProjectsArray() {
		return projects.values().toArray(new Project[projects.size()]);
	}
	
	public synchronized void visit(Visitor v) {
		for (Folder f : folders.values()) {
			f.visit(v);
		}
	}
	
	public Project getProject(int id) {
		return projects.get(id);
	}

	public Folder getFolder(int id) {
		return folders.get(id);
	}

	public Folder getInBucketFolder() {
		return inBucket;
	}

	public Folder getResolvedFolder() {
		return resolved;
	}
	public Folder getDeletedFolder() {
		return deleted;
	}
	
	public void importData(GTDModel m) {

		getDataRepository().suspend(true);
		
		Map<Integer,Integer> folderMap= new HashMap<Integer, Integer>();
		Map<String,Folder> folderNames= new HashMap<String, Folder>();
		
		for (Folder f : this) {
			folderNames.put(f.getName()+"TYPE"+f.getType(), f); //$NON-NLS-1$
		}
		
		Folder[] pp= m.toFoldersArray();
		for (Folder inP : pp) {
			if (inP.isBuildIn()) {
				continue;
			}
			Folder f= folderNames.get(inP.getName()+"TYPE"+inP.getType()); //$NON-NLS-1$
			if (f==null) {
				f= createFolder(inP.getName(), inP.getType());
				f.setDescription(inP.getDescription());
				f.setClosed(inP.isClosed());
			}
			folderMap.put(inP.getId(), f.getId());
		}

		for (Folder inF : m) {
			if (!inF.isMeta()) {
				Folder f= getFolder(folderMap.get(inF.getId()));
				for (int i= inF.size()-1; i>-1; i--) {
				//for (int i= 0; i< inF.size(); i++) {
					Action inA=inF.get(i);
					createActionCopy(f, inA, folderMap.get(inA.getProject()));
				}
			}
		}

		getDataRepository().suspend(false);
	}
	
	public int getLastActionID() {
		return lastActionID;
	}

	public boolean isSuspendedForMultipleChanges() {
		return suspendedForMultipleChanges;
	}

	public void setSuspendedForMultipleChanges(boolean suspendedForMultipleChanges) {
		this.suspendedForMultipleChanges = suspendedForMultipleChanges;
		reminder.setSuspendedForMultipleChanges(suspendedForMultipleChanges);
		resolved.setSuspendedForMultipleChanges(suspendedForMultipleChanges);
		deleted.setSuspendedForMultipleChanges(suspendedForMultipleChanges);
		queue.setSuspendedForMultipleChanges(suspendedForMultipleChanges);
		priority.setSuspendedForMultipleChanges(suspendedForMultipleChanges);
	}
	
	public Folder getQueue() {
		return queue;
	}

	public Folder getRemindFolder() {
		return reminder;
	}

	public Folder getPriorityFolder() {
		return priority;
	}

	public void purgeDeletedActions() {
		deleted.purgeAll();
	}

	void setLastActionID(int i) {
		lastActionID=i;
	}

	public GTDData getDataRepository() {
		if (dataRepository ==null) {
			dataRepository = new GTDDataDefault(this);
		}
		return dataRepository;
	}

	void reconnect() {

		ModelListenerSupport s= support;
		support= new ModelListenerSupport(this);

		
		for (Folder f : this) {
			if (s!=null) f.removeFolderListener(s);
			f.addFolderListener(support);
			f.setParent(this);
		}
		
	}
	
	public Folder findFirstFolder(String name) {
		
		Iterator<Folder> i= folders.values().iterator();
		
		while (i.hasNext()) {
			Folder f = i.next();
			if (f.getName().equals(name)) {
				return f;
			}
		}
		
		return null;
		
	}
	
	public Project findFirstProject(String name) {
		
		Iterator<Project> i= projects.values().iterator();
		
		while (i.hasNext()) {
			Project f = i.next();
			if (f.getName().equals(name)) {
				return f;
			}
		}
		
		return null;
		
	}

	public Action collectAction(String string) {
		
		Action a = createAction(inBucket, string);
		return a;
		
	}

	public void importXMLFile(File file) throws XMLStreamException, FactoryConfigurationError, IOException {
		GTDDataXMLTools.importFile(this, file);
	}

	public void exportXML(File f) throws IOException, XMLStreamException, FactoryConfigurationError {
		GTDDataXMLTools.store(this, f);
	}

	public void importXML(File f) throws XMLStreamException, IOException {
		GTDDataXMLTools.importFile(this,f);
	}

}
