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

package org.gtdfree.gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.gtdfree.model.Action;
import org.gtdfree.model.ActionEvent;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.GTDModelAdapter;
import org.gtdfree.model.Project;
import org.gtdfree.model.StatusActionFilter;

/**
 * @author ikesan
 *
 */
public class SelectionModel {
	
	public static enum SelectionMode { Lists, Projects, Lists_and_projects, Actions }
	//public static enum FilterAggregator { AND, OR };
	public static enum SelectionCriteria { ALL, CUSTOM };
	
	public static final String PROP_CUSTOM_FOLDERS = "customFolders"; //$NON-NLS-1$
	public static final String PROP_CUSTOM_PROJECTS = "customFolders"; //$NON-NLS-1$
	public static final String PROP_CUSTOM_ACTIONS = "customActions"; //$NON-NLS-1$
	public static final String PROP_ACTION_CRITERIA = "actionCriteria"; //$NON-NLS-1$
	public static final String PROP_PROJECT_CRITERIA = "projectCriteria"; //$NON-NLS-1$
	public static final String PROP_FOLDER_CRITERIA = "folderCriteria"; //$NON-NLS-1$
	public static final String PROP_SELECTED_FOLDERS = "selectedFolders"; //$NON-NLS-1$
	public static final String PROP_SELECTED_PROJECTS = "selectedProjects"; //$NON-NLS-1$
	public static final String PROP_INCLUDE_RESOLVED = "includeResolves"; //$NON-NLS-1$
	public static final String PROP_INCLUDE_DELETED = "includeResolves"; //$NON-NLS-1$
	public static final String PROP_INCLUDE_EMPTY_FOLDERS = "exportEmptyFolders"; //$NON-NLS-1$
	public static final String PROP_INCLUDE_EMPTY_PROJECTS = "exportEmptyProjects"; //$NON-NLS-1$
	public static final String PROP_INCLUDE_WITHOUT_PROJECT = "exportWithoutProject"; //$NON-NLS-1$
	
	
	//private SelectionMode selectionMode= SelectionMode.Lists;
	private GTDModel gtdModel;
	
	private List<Folder>customFolders = new ArrayList<Folder>();
	private List<Project>customProjects = new ArrayList<Project>();
	private List<Action>customActions = new ArrayList<Action>();

	private List<Folder>selectedFolders = new ArrayList<Folder>();
	private List<Project>selectedProjects = new ArrayList<Project>();

	private boolean selectionDirty=true; 
	
	private SelectionCriteria folderCriteria= SelectionCriteria.ALL;
	private SelectionCriteria projectCriteria= SelectionCriteria.ALL;
	private SelectionCriteria actionCriteria= SelectionCriteria.ALL;
	
	
	private PropertyChangeSupport prop= new PropertyChangeSupport(this);
	private boolean includeResolved=false;
	private boolean includeDeleted=false;
	private boolean includeEmptyFolders=false;
	private boolean includeEmptyProjects=false;
	private boolean includeWithoutProject=true;
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		prop.addPropertyChangeListener(l);
	}
	public void removePropertyChangeListener(PropertyChangeListener l) {
		prop.removePropertyChangeListener(l);
	}
	public void addPropertyChangeListener(String p, PropertyChangeListener l) {
		prop.addPropertyChangeListener(p, l);
	}
	public void removePropertyChangeListener(String p, PropertyChangeListener l) {
		prop.removePropertyChangeListener(p, l);
	}
	
	public void setGtdModel(GTDModel model) {
		this.gtdModel = model;
		model.addGTDModelListener(new GTDModelAdapter() {
		
			@Override
			public void elementRemoved(FolderEvent a) {
				removeCustomAction(a.getAction());
			}
		
			@Override
			public void elementModified(ActionEvent a) {
				/*if (!isAcceptable(a.getAction().getFolder(),a.getAction())) {
					removeCustomAction(a.getAction());
				}*/
			}
		
			@Override
			public void folderRemoved(Folder folder) {
				removeCustomFolder(folder);
			}
		
			@Override
			public void folderModified(FolderEvent folder) {
				/*if (!isAcceptable(folder.getFolder(),null)) {
					removeCustomFolder(folder.getFolder());
				}*/
			}
		
		});
	}
	
	public GTDModel getGtdModel() {
		return gtdModel;
	}
	
	private void firePropertyChangeEvent(String p, Object val) {
		prop.firePropertyChange(p, null, val);
	}

	protected void removeCustomFolder(Folder folder) {
		if (customFolders.remove(folder)){
			firePropertyChangeEvent(PROP_CUSTOM_FOLDERS,customFolders);
		}
	}
	
	protected void removeCustomProject(Project folder) {
		if (customProjects.remove(folder)){
			firePropertyChangeEvent(PROP_CUSTOM_PROJECTS,customProjects);
		}
	}

	/*protected boolean isAcceptable(Folder folder, Action action) {
		if (selectionFilters.size()>0) {
			for (ActionFilter f : selectionFilters) {
				if (!f.isAcceptable(folder, action)) {
					return false;
				}
			}
			return true;
		}
		return true;
	}*/
	
	protected void removeCustomAction(Action action) {
		if (customActions.remove(action)){
			firePropertyChangeEvent(PROP_CUSTOM_ACTIONS,customActions);
		}
	}
	/**
	 * @return the folderCriteria
	 */
	public SelectionCriteria getFolderCriteria() {
		return folderCriteria;
	}
	/**
	 * @param folderCriteria the folderCriteria to set
	 */
	public void setFolderCriteria(SelectionCriteria folderCriteria) {
		if (this.folderCriteria == folderCriteria) {
			return;
		}
		this.folderCriteria = folderCriteria;
		selectionDirty= true;
		firePropertyChangeEvent(PROP_FOLDER_CRITERIA, folderCriteria);
		firePropertyChangeEvent(PROP_SELECTED_FOLDERS, 1);
	}
	/**
	 * @return the projectCriteria
	 */
	public SelectionCriteria getProjectCriteria() {
		return projectCriteria;
	}
	/**
	 * @param projectCriteria the projectCriteria to set
	 */
	public void setProjectCriteria(SelectionCriteria projectCriteria) {
		if (this.projectCriteria == projectCriteria) {
			return;
		}
		this.projectCriteria = projectCriteria;
		selectionDirty= true;
		firePropertyChangeEvent(PROP_PROJECT_CRITERIA, projectCriteria);
		firePropertyChangeEvent(PROP_SELECTED_PROJECTS, 1);
	}
	/**
	 * @return the actionCriteria
	 */
	public SelectionCriteria getActionCriteria() {
		return actionCriteria;
	}
	/**
	 * @param actionCriteria the actionCriteria to set
	 */
	public void setActionCriteria(SelectionCriteria actionCriteria) {
		if (this.actionCriteria == actionCriteria) {
			return;
		}
		this.actionCriteria = actionCriteria;
		selectionDirty=true;
		firePropertyChangeEvent(PROP_ACTION_CRITERIA, actionCriteria);
	}
	
	public void setCustomFolders(Folder[] f) {
		customFolders.clear();
		for (Folder folder : f) {
			customFolders.add(folder);
		}
		selectionDirty= true; 
		firePropertyChangeEvent(PROP_CUSTOM_FOLDERS, customFolders);
		if (selectionDirty) {
			firePropertyChangeEvent(PROP_SELECTED_FOLDERS, 1);
		}
	}
	
	public void setCustomProjects(Folder[] f) {
		customProjects.clear();
		for (Folder folder : f) {
			customProjects.add((Project)folder);
		}
		selectionDirty= true;
		firePropertyChangeEvent(PROP_CUSTOM_PROJECTS, customProjects);
		if (selectionDirty) {
			firePropertyChangeEvent(PROP_SELECTED_PROJECTS, 1);
		}
	}
	public Folder[] getSelectedFolders() {
		updateSeletion();
		return selectedFolders.toArray(new Folder[selectedFolders.size()]);
	}

	public int getSelectedFoldersCount() {
		updateSeletion();
		return selectedFolders.size();
	}

	public Iterator<Folder> selectedFolders() {
		updateSeletion();
		return selectedFolders.iterator();
	}
	
	public Folder[] getSelectedProjects() {
		updateSeletion();
		return selectedProjects.toArray(new Folder[selectedProjects.size()]);
	}

	public int getSelectedProjectsCount() {
		updateSeletion();
		return selectedProjects.size();
	}

	public Iterator<Project> selectedProjects() {
		updateSeletion();
		return selectedProjects.iterator();
	}
	
	private void updateSeletion() {
		
		if (!selectionDirty) {
			return;
		}
		
		if (gtdModel==null) {
			return;
		}
		
		selectionDirty=false;
		
		selectedFolders.clear();
		selectedProjects.clear();
		
		boolean done=true;
		
		if (folderCriteria==SelectionCriteria.CUSTOM) {
			for (Folder f : customFolders) {
				if (includeEmptyFolders || f.getOpenCount()>0 || (f.size()>0 && (includeDeleted || includeResolved))) {
					selectedFolders.add(f);
				}
			}
		} else {
			done=false;
		}
		if (projectCriteria==SelectionCriteria.CUSTOM) {
			for (Folder f : customProjects) {
				if (includeEmptyProjects || f.getOpenCount()>0 || (f.size()>0 && (includeDeleted || includeResolved))) {
					selectedProjects.add((Project)f);
				}
			}
		} else {
			done=false;
		}
		
		if (!done) {
			
			for (Folder f : gtdModel) {
				if (f.isProject()) {
					if (projectCriteria == SelectionCriteria.ALL) {
						if (includeEmptyProjects || f.getOpenCount()>0 || (f.size()>0 && (includeDeleted || includeResolved))) {
							selectedProjects.add((Project)f);
						}
					}
				} else {
					if (folderCriteria == SelectionCriteria.ALL) {
						if (includeEmptyFolders || f.getOpenCount()>0 || (f.size()>0 && (includeDeleted || includeResolved))) {
							selectedFolders.add(f);
						}
					}
				}
			}
			
		}
		
		Collections.sort(selectedFolders, new Comparator<Folder>() {
		
			@Override
			public int compare(Folder o1, Folder o2) {
				int i= o1.getName().compareTo(o2.getName());
				if (i==0) {
					return o1.getId()-o2.getId();
				}
				return i;
			}
		});

		Collections.sort(selectedProjects, new Comparator<Project>() {
			
			@Override
			public int compare(Project o1, Project o2) {
				int i= o1.getName().compareTo(o2.getName());
				if (i==0) {
					return o1.getId()-o2.getId();
				}
				return i;
			}
		});
	}
	
	public ActionsCollection getSelection() {
		updateSeletion();
		
		return new ActionsCollection(
				selectedFolders,
				selectedProjects, 
				includeEmptyFolders,
				includeEmptyProjects,
				includeWithoutProject,
				new StatusActionFilter( includeResolved, includeDeleted));
		
	}
	
	public void setIncludeResolved(boolean selected) {
		includeResolved=selected;
		
		firePropertyChangeEvent(PROP_INCLUDE_RESOLVED, includeResolved);
	}
	
	public boolean isIncludeResolved() {
		return includeResolved;
	}
	
	public void setIncludeDeleted(boolean selected) {
		includeDeleted=selected;
		
		firePropertyChangeEvent(PROP_INCLUDE_DELETED, includeDeleted);
	}
	
	public boolean isIncludeDeleted() {
		return includeDeleted;
	}
	
	public void setIncludeEmptyFolders(boolean selected) {
		if (includeEmptyFolders == selected) {
			return;
		}
		includeEmptyFolders= selected;
		selectionDirty= true;
		firePropertyChangeEvent(PROP_INCLUDE_EMPTY_FOLDERS, includeEmptyFolders);
		firePropertyChangeEvent(PROP_SELECTED_PROJECTS, 1);
	}
	
	public boolean isIncludeEmptyFolders() {
		return includeEmptyFolders;
	}

	public void setIncludeEmptyProjects(boolean selected) {
		if (includeEmptyProjects == selected) {
			return;
		}
		includeEmptyProjects= selected;
		selectionDirty= true;
		firePropertyChangeEvent(PROP_INCLUDE_EMPTY_PROJECTS, includeEmptyProjects);
		firePropertyChangeEvent(PROP_SELECTED_PROJECTS, 1);
	}
	
	public boolean isIncludeEmptyProjects() {
		return includeEmptyProjects;
	}
	
	public void setIncludeWithoutProject(boolean b) {
		if (includeWithoutProject== b) {
			return;
		}
		includeWithoutProject= b;
		selectionDirty= true;
		firePropertyChangeEvent(PROP_INCLUDE_WITHOUT_PROJECT, includeWithoutProject);
		firePropertyChangeEvent(PROP_SELECTED_PROJECTS, 1);
	}
	public boolean isIncludeWithoutProject() {
		return includeWithoutProject;
	}
}
