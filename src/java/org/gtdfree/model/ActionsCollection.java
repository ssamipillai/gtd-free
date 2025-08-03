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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gtdfree.Messages;
import org.gtdfree.addons.ExportAddOn;
import org.gtdfree.gui.ActionTable;


/**
 * @author ikesan
 *
 */
public class ActionsCollection {
	
	class ActionTableIterator implements Iterator<Object> {
		
		int index=-1;
		
		@Override
		public boolean hasNext() {
			return actionTable!=null && actionTable.getFolder()!=null && index<actionTable.getRowCount();
		}
		@Override
		public Object next() {
			if (index<0) {
				index=0;
				return actionTable.getFolder();
			}
			return actionTable.getActionAtView(index++);
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	class FoldersActionsIterator implements Iterator<Object> {
		
		int folderIndex=0;
		int actionIndex=0;
		ActionFilter filter;
		// if true means next returned object should be folder
		boolean nextFolderReady=true;
		
		
		public FoldersActionsIterator(ActionFilter f) {
			filter=f;
		}
		
		@Override
		public boolean hasNext() {
			if (folderIndex>=folders.size()) {
				// no more folders
				return false;
			}
			Folder f=folders.get(folderIndex); 
			if (!filter.isAcceptable(f, null)) {
				// current folder is not acceptable, shifting to next
				folderIndex++;
				// next returned object should be folder
				nextFolderReady=true;
				// resetting action index 
				actionIndex=0;
				// find also next action
				return hasNext();
			}
			if (actionIndex>=f.size()) {
				// end of actions
				if (nextFolderReady && includeEmptyFolders) {
					// previous folder was not consumed, folder is empty 
					// it's OK to consume empty folder  
					return true;
				} 
				
				// we are really at the end of folder, moving to next
				folderIndex++;
				// next returned object should be folder
				nextFolderReady=true;
				// resetting action index 
				actionIndex=0;
				// find also next action
				return hasNext();
			}
			
			if (!filter.isAcceptable(f, f.get(actionIndex)) || !isInRange(f,f.get(actionIndex))) {
				// current action is not acceptable, move to next
				actionIndex++;
				// test next action
				return hasNext();
			}
			
			return true;
		}
		
		@Override
		public Object next() {
			// prepare next items
			if (!hasNext()) {
				return null;
			}
			if (nextFolderReady) {
				nextFolderReady=false;
				return folders.get(folderIndex);
			}
			return folders.get(folderIndex).get(actionIndex++);
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	class ActionsIterator implements Iterator<Object> {
		
		int folderIndex=0;
		int actionIndex=0;
		ActionFilter filter;
		
		public ActionsIterator(ActionFilter f) {
			filter=f;
		}
		
		@Override
		public boolean hasNext() {
			if (folderIndex>=folders.size()) {
				return false;
			}
			Folder f=folders.get(folderIndex); 
			if (!filter.isAcceptable(f, null)) {
				folderIndex++;
				return hasNext();
			}
			if (actionIndex>=f.size()) {
				folderIndex++;
				actionIndex=0;
				return hasNext();
			}
			if (!filter.isAcceptable(f, f.get(actionIndex)) || !isInRange(f,f.get(actionIndex))) {
				actionIndex++;
				return hasNext();
			}
			
			return true;
		}
		
		@Override
		public Object next() {
			if (!hasNext()) {
				return null;
			}
			return folders.get(folderIndex).get(actionIndex++);
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

	class FoldersProjectsActionsIterator implements Iterator<Object> {
		
		int folderIndex=0;
		ProjectsInFolderIterator byProjects;
		// if true means next returned object should be folder
		boolean nextFolderReady=true;
		
		@Override
		public boolean hasNext() {
			if (folderIndex>=folders.size()) {
				// no more folders
				return false;
			}
			Folder f=folders.get(folderIndex); 
			if (!filter.isAcceptable(f, null)) {
				// current folder is not acceptable, shifting to next
				folderIndex++;
				// next returned object should be folder
				nextFolderReady=true;
				// find also next action
				byProjects=null;
				return hasNext();
			}
			if (byProjects==null) {
				byProjects= new ProjectsInFolderIterator(folderIndex);
			}
			if (!byProjects.hasNext()) {
				// end of actions
				if (nextFolderReady && includeEmptyFolders) {
					// previous folder was not consumed, folder is empty 
					// it's OK to consume empty folder  
					return true;
				} 
				
				// we are really at the end of folder, moving to next
				folderIndex++;
				// next returned object should be folder
				nextFolderReady=true;
				// resetting action index 
				byProjects=null;
				// find also next action
				return hasNext();
			}
			
			return true;
		}
		
		@Override
		public Object next() {
			if (!hasNext()) {
				return null;
			}
			if (nextFolderReady) {
				nextFolderReady=false;
				return folders.get(folderIndex);
			}
			return byProjects.next();
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

	class ProjectsActionsIterator implements Iterator<Object> {
		
		int projectIndex=0;
		int actionIndex=0;
		boolean nextProjectReady=true;
		ActionsIterator noProject;
		
		@Override
		public boolean hasNext() {
			if (projectIndex>=projects.size()) {
				// no more projects
				// check actions with no project defined
				if (noProject==null) {
					nextProjectReady=true;
					noProject= new ActionsIterator(new ANDActionFilter(new ProjectFilter(null),filter));
				}
				return noProject.hasNext();
			}
			Project f=projects.get(projectIndex); 
			if (!filter.isAcceptable(f, null)) {
				// current folder is not acceptable, shifting to next
				projectIndex++;
				// next returned object should be folder
				nextProjectReady=true;
				// resetting action index 
				actionIndex=0;
				// find also next action
				return hasNext();
			}
			if (actionIndex>=f.size()) {
				// end of actions
				if (nextProjectReady && includeEmptyFolders) {
					// previous folder was not consumed, folder is empty 
					// it's OK to consume empty folder  
					return true;
				} 
				// we are really at the end of folder, moving to next
				projectIndex++;
				// next returned object should be folder
				nextProjectReady=true;
				// resetting action index 
				actionIndex=0;
				// find also next action
				return hasNext();
			}
			Action a= f.get(actionIndex);
			if (!filter.isAcceptable(f, a) || !isInRange(a.getParent(), a)) {
				// current action is not acceptable, move to next
				actionIndex++;
				// test next action
				return hasNext();
			}
			
			return true;
		}
		
		@Override
		public Object next() {
			if (!hasNext()) {
				return null;
			}
			if (noProject!=null) {
				if (nextProjectReady) {
					nextProjectReady=false;
					return ACTIONS_WITHOUT_PROJECT;
				}
				return noProject.next();
			}
			if (nextProjectReady) {
				nextProjectReady=false;
				return projects.get(projectIndex);
			}
			return projects.get(projectIndex).get(actionIndex++);
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	class ProjectsFoldersActionsIterator implements Iterator<Object> {
		
		int projectIndex=0;
		FoldersInProjectIterator byFolders;
		boolean nextProjectReady=true;
		Iterator<Object> noProject;
		
		@Override
		public boolean hasNext() {
			
			if (projectIndex>=projects.size()) {
				// no more projects
				// check actions with no project defined
				if (noProject==null) {
					nextProjectReady=true;
					noProject= new FoldersActionsIterator(new ANDActionFilter(new ProjectFilter(null),filter));
				}
				return noProject.hasNext();
			}
			Folder f=projects.get(projectIndex); 
			if (!filter.isAcceptable(f, null)) {
				// current folder is not acceptable, shifting to next
				projectIndex++;
				// next returned object should be folder
				nextProjectReady=true;
				// find also next action
				byFolders=null;
				return hasNext();
			}
			if (byFolders==null) {
				byFolders= new FoldersInProjectIterator(projectIndex);
			}
			if (!byFolders.hasNext()) {
				// end of actions
				if (nextProjectReady && includeEmptyProjects) {
					// previous folder was not consumed, folder is empty 
					// it's OK to consume empty folder  
					return true;
				} 
				
				// we are really at the end of folder, moving to next
				projectIndex++;
				// next returned object should be folder
				nextProjectReady=true;
				// resetting action index 
				byFolders=null;
				// find also next action
				return hasNext();
			}
			
			return true;
		}
		
		@Override
		public Object next() {
			if (!hasNext()) {
				return null;
			}
			if (noProject!=null) {
				if (nextProjectReady) {
					nextProjectReady=false;
					return ACTIONS_WITHOUT_PROJECT;
				}
				return noProject.next();
			}
			if (nextProjectReady) {
				nextProjectReady=false;
				return projects.get(projectIndex);
			}
			return byFolders.next();
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

	class Node {
		
		public Object obj;
		public Node next;
		
		public Node(Object obj) {
			this.obj=obj;
		}
		
		public Node add(Object obj) {
			if (next!=null) {
				return next.add(obj);
			}
			return next=new Node(obj);
		}
		
	}

	class ProjectsInFolderIterator implements Iterator<Object> {
		
		Node node;
		
		public ProjectsInFolderIterator(int folder) {
			
			Node[] first= new Node[projects.size()+1];
			Node[] last= new Node[first.length];

			Folder f= folders.get(folder);
			
			for (Action a : f) {
				
				if (filter.isAcceptable(f, a)) {

					Integer i= null;
					if (a.getProject()==null) {
						if (includeWithoutProject) {
							i= projects.size();
							if (first[i]==null) {
								addNode(ACTIONS_WITHOUT_PROJECT,i,first,last);
							}
						}
					} else {
						i= getProjectKey2Index().get(a.getProject());
					}
					if (i!=null) {
						if (first[i]==null) {
							addNode(projects.get(i),i,first,last);
						}
						addNode(a,i,first,last);
					}
					
				}
			}
			
			node= joinNodes(first, last);
			
		}
		
		@Override
		public boolean hasNext() {
			return node!=null;
		}
		
		@Override
		public Object next() {
			if (!hasNext()) {
				return null;
			}
			Node n= node;
			node=node.next;
			return n.obj;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	class FoldersInProjectIterator implements Iterator<Object> {
		
		Node node;
		
		public FoldersInProjectIterator(int project) {
			
			Node[] first= new Node[folders.size()];
			Node[] last= new Node[first.length];

			Folder f= projects.get(project);
			
			for (Action a : f) {
				
				if (filter.isAcceptable(a.getParent(), a)) {
					
					Integer i= getFoldersKeys2Index().get(a.getParent().getId());
					if (i!=null) {
						if (first[i]==null) {
							addNode(a.getParent(),i,first,last);
						}
						addNode(a,i,first,last);
					}
					
				}
			}
			
			node= joinNodes(first, last);
			
		}
		
		@Override
		public boolean hasNext() {
			return node!=null;
		}
		
		@Override
		public Object next() {
			if (!hasNext()) {
				return null;
			}
			Node n= node;
			node=node.next;
			return n.obj;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	
	public static final String ACTIONS_WITHOUT_PROJECT= Messages.getString("ActionsCollection.Actions"); //$NON-NLS-1$
	
	private List<Folder> folders;
	private List<Project> projects;
	private Map<Integer,Integer> folderKeys;
	private Map<Integer,Integer> projectKeys;
	private ActionFilter filter;
	/* returns lists with no actions */
	private boolean includeEmptyFolders=false;
	/* returns projects with no actions */
	private boolean includeEmptyProjects=false;
	/* returns actions with no project defined*/
	private boolean includeWithoutProject=false;

	private ActionTable actionTable;

	
	public ActionsCollection(ActionTable at) {
		actionTable=at;
		folders= new ArrayList<Folder>();
		projects= new ArrayList<Project>();
		
		for (int i=0; i<at.getRowCount(); i++) {
			Action a= at.getAction(i);
			if (!folders.contains(a.getParent())) {
				folders.add(a.getParent());
			}
			if (a.getProject()!=null) {
				Project p= a.getParent().getParent().getProject(a.getProject());
				if (!projects.contains(p)) {
					projects.add(p);
				}
			} else {
				includeWithoutProject=true;
			}
		}
		filter= new ActionFilter() {
			
			@Override
			public boolean isAcceptable(Folder f, Action a) {
				if (f instanceof Project) {
					if (!getProjectKeys().contains(f.getId())) {
						return false;
					}
				} else {
					if (!getFoldersKeys().contains(f.getId())) {
						return false;
					}
				}
				if (a!=null) {
					return actionTable.containsAction(a);
				}
				return true;
			}
		};
	}
	/**
	 * @param folders folders with actions to iterate over
	 * @param projects projects with actions to iterate over
	 * @param includeEmptyFolders
	 * @param includeEmptyProjects
	 * @param includeWithoutProject
	 * @param filter additional filter
	 */
	public ActionsCollection(List<Folder> folders, List<Project> projects, boolean includeEmptyFolders, boolean includeEmptyProjects, boolean includeWithoutProject, ActionFilter filter) {
		super();
		this.folders = folders;
		this.projects = projects;
		this.filter = filter;
		this.includeEmptyFolders=includeEmptyFolders;
		this.includeEmptyProjects=includeEmptyProjects;
		this.includeWithoutProject=includeWithoutProject;
	}

	public boolean isInRange(Folder f, Action a) {
		if (a.getProject()!=null) {
			if (includeWithoutProject) {
				return getFoldersKeys().contains(f.getId());
			}
			return getProjectKeys().contains(a.getProject()) && getFoldersKeys().contains(f.getId());
		}
		return includeWithoutProject && getFoldersKeys().contains(f.getId());
	}

	public ActionFilter getFilter() {
		return filter;
	}
	
	public List<Folder> getFolders() {
		return folders;
	}
	
	public Map<Integer,Integer> getProjectKey2Index() {
		if (projectKeys==null) {
			projectKeys= new HashMap<Integer,Integer>(projects.size());
			
			for (int i=0; i<projects.size(); i++) {
				projectKeys.put(projects.get(i).getId(),i);
			}
		}
		return projectKeys;
	}

	public Set<Integer> getProjectKeys() {
		return getProjectKey2Index().keySet();
	}
	
	public Map<Integer,Integer> getFoldersKeys2Index() {
		if (folderKeys==null) {
			folderKeys= new HashMap<Integer,Integer>(folders.size());
			
			for (int i=0; i< folders.size(); i++) {
				folderKeys.put(folders.get(i).getId(),i);
			}
		}
		return folderKeys;
	}

	public Set<Integer> getFoldersKeys() {
		return getFoldersKeys2Index().keySet();
	}

	public List<Project> getProjects() {
		return projects;
	}

	public Iterator<Object> iterator(ExportAddOn.ExportOrder order) {
		
		if (actionTable!=null) {
			return new ActionTableIterator();
		}
		
		if (order== ExportAddOn.ExportOrder.FoldersActions) {
			return new FoldersActionsIterator(filter);
		}
		if (order== ExportAddOn.ExportOrder.FoldersProjectsActions) {
			return new FoldersProjectsActionsIterator();
		}
		if (order== ExportAddOn.ExportOrder.ProjectsFoldersActions) {
			return new ProjectsFoldersActionsIterator();
		}
		if (order== ExportAddOn.ExportOrder.ProjectsActions) {
			return new ProjectsActionsIterator();
		}
		if (order== ExportAddOn.ExportOrder.Actions) {
			return new ActionsIterator(filter);
		}
		
		return new ActionsIterator(filter);
		
	}

	private final void addNode(Object a, int i, Node[] first, Node[] last) {
		if (first[i]==null) {
			first[i]=last[i]= new Node(a);
		} else {
			last[i]=last[i].add(a);
		}
	}
		
	private final Node joinNodes(Node[] first, Node[] last) {
		
		Node n=null;
		Node nn=null;
		
		for (int i = 0; i < first.length; i++) {
			if (first[i]!=null) {
				if (first[i].next==null && first[i].obj!=null) {
					Folder f= (Folder)first[i].obj;
					if (!includeEmptyFolders && !f.isProject()) {
						continue;
					}
					if (!includeEmptyProjects && f.isProject()) {
						continue;
					}
				}
				if (n==null) {
					n=first[i];
					nn=last[i];
				} else {
					nn.next=first[i];
					nn= last[i];
				}
			}
		}
		
		return n;
		
	}
	
	public boolean isIncludeEmptyFolders() {
		return includeEmptyFolders;
	}
	
	public boolean isIncludeEmptyProjects() {
		return includeEmptyProjects;
	}
	
	public boolean isIncludeWithoutProject() {
		return includeWithoutProject;
	}
	
}
