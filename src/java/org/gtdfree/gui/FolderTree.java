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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.model.Action;
import org.gtdfree.model.ActionEvent;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.GTDModelListener;
import org.gtdfree.model.Project;
import org.gtdfree.model.Action.Resolution;
import org.gtdfree.model.Folder.FolderPreset;
import org.gtdfree.model.Folder.FolderType;


/**
 * @author ikesan
 *
 */
public class FolderTree extends JTree {
	
	private static final long serialVersionUID = 1L;

	private final class FolderTreeCellEditor extends DefaultTreeCellEditor {
		private FolderTreeCellEditor(JTree tree,
				DefaultTreeCellRenderer renderer) {
			super(tree, renderer);
		}

		@Override
		public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
			if (value instanceof DefaultMutableTreeNode && ((DefaultMutableTreeNode)value).getUserObject() instanceof Folder) {
				value= ((Folder)((DefaultMutableTreeNode)value).getUserObject()).getName();
			}
			return super.getTreeCellEditorComponent(tree, value, isSelected, expanded,
					leaf, row);
		}
		
		@Override
		public boolean shouldSelectCell(EventObject event) {
			return true;
		}
		@Override
		public boolean isCellEditable(EventObject event) {
			Boolean b= super.isCellEditable(event); 
			if (event instanceof MouseEvent) {
				MouseEvent m= (MouseEvent)event;
				return m.getClickCount()>1;
			}
			return b;
		}
		
	}

	private final class FolderTreeCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;

		public FolderTreeCellRenderer() {
			/*addMouseListener(new MouseAdapter() {
				private void popup(MouseEvent e) {
					if (e.isPopupTrigger()) {
						TreePath t= getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
						showPopup(t,e.getPoint());
					}
				}
				@Override
				public void mousePressed(MouseEvent e) {
					if (getPathForLocation(e.getPoint().x, e.getPoint().y)!=null || e.getPoint().x<30) {
						return;
					}
					TreePath t= getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
					setSelectionPath(t);
					popup(e);
				}
				@Override
				public void mouseReleased(MouseEvent e) {
					if (getPathForLocation(e.getPoint().x, e.getPoint().y)!=null || e.getPoint().x<30) {
						return;
					}
					TreePath t= getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
					setSelectionPath(t);
					popup(e);
				}
			});*/
		}
		
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			DefaultMutableTreeNode node= (DefaultMutableTreeNode)value;
			super.getTreeCellRendererComponent(tree, "", sel, expanded, leaf, //$NON-NLS-1$
					row, hasFocus);
			
			Font font= FolderTree.this.getFont();
			
			Folder f=null;
			if (node.getUserObject() instanceof Folder) {
				f = (Folder)node.getUserObject();
				StringBuilder sb= new StringBuilder(32);
				sb.append(f.getName());
				sb.append(' ');
				sb.append('(');
				if (f.getType() == FolderType.BUILDIN_RESOLVED || f.getType() == FolderType.BUILDIN_DELETED) {
					sb.append(f.size());
				} else {
					sb.append(f.getOpenCount());
				}
				sb.append(')');
				value= sb.toString();
			}
			if (f != null) {
				if (f.isClosed()) {
					setForeground(Color.LIGHT_GRAY);
				}
			}

			String s= String.valueOf(value);
			setText(s);
			setToolTipText(s);

			if (node.getLevel()==1) {
				super.setFont(font.deriveFont(Font.BOLD).deriveFont(font.getSize()+2f));
				//super.setPreferredSize(null);
			} else {
				//super.setPreferredSize(null);
				if (f != null && f.getOpenCount()>0) {
					super.setFont(font.deriveFont(Font.BOLD));
				} else {
					super.setFont(font);
				}
				//setBackground(FolderTree.this.getBackground());
			}
			
			return this;
		}

		@Override
		public void setFont(Font font) {
			// ignore
		}
		@Override
		public Icon getIcon() {
			return null;
		}
		@Override
		public void setIcon(Icon icon) {
			// no
		}
	}

	private final class FolderTreeTransferHandler extends ActionTransferHandler {
		private static final long serialVersionUID = 0L;

		@Override
		protected boolean importActions(Action[] a, Folder source, int[] indexes, TransferSupport support) {
			
			try {
				TreePath tp= getClosestPathForLocation(support.getDropLocation().getDropPoint().x,support.getDropLocation().getDropPoint().y);
				DefaultMutableTreeNode n= (DefaultMutableTreeNode)tp.getLastPathComponent();
				if (n!=null && n.getUserObject() instanceof Folder && a!=null) {
					Folder target= (Folder)n.getUserObject();
					if (target.isProject()) {
						for (int i = 0; i < a.length; i++) {
							a[i].setProject(target.getId());
						}
					} else {
						gtdModel.moveActions(a, target);
					}
					setSelectedFolder(source,indexes[indexes.length-1]+1-indexes.length);
					lastDroppedActionIndex=-1;
					return true;
				}
			} catch (Throwable t) {
				Logger.getLogger(this.getClass()).debug("Internal error.", t); //$NON-NLS-1$
			}
			setSelectedFolder(source,-1);
			return false;
		}

		@Override
		protected Action[] exportActions() {
			return null;
		}
		@Override
		protected Folder exportSourceFolder() {
			return null;
		}
		@Override
		protected int[] exportIndexes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean canImport(TransferSupport support) {
			TreePath tp= getClosestPathForLocation(support.getDropLocation().getDropPoint().x,support.getDropLocation().getDropPoint().y);
			DefaultMutableTreeNode n= (DefaultMutableTreeNode)tp.getLastPathComponent();
			if (n!=null && n.getUserObject() instanceof Folder && !((Folder)n.getUserObject()).isMeta() && ((Folder)n.getUserObject()).getType()!=FolderType.INBUCKET) {
				return super.canImport(support);
			} 
			if (n!=null && n.getUserObject() instanceof Project) {
				return super.canImport(support);
			} 
			/*if (n==references || n==actions) {
				expandPath(new TreePath(n.getPath()));
			}*/
			return false;
		}
	}
	
	final class FolderTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 1L;
		private Folder folder;
		
		public FolderTreeNode(Folder f) {
			super(f,false);
			folder=f;
		}
		@Override
		public void setUserObject(Object userObject) {
			if (userObject instanceof String) {
				folder.rename((String)userObject);
			} else if (userObject instanceof Folder) {
				folder=(Folder)userObject;
				super.setUserObject(userObject);
			}
		}
	}

	final class SortedTreeNode extends DefaultMutableTreeNode implements Comparator<DefaultMutableTreeNode> {
		
		
		private static final long serialVersionUID = 1L;
		public SortedTreeNode() {
			super();
		}
		/**
		 * @param userObject
		 * @param allowsChildren
		 */
		public SortedTreeNode(Object userObject, boolean allowsChildren) {
			super(userObject, allowsChildren);
		}
		/**
		 * @param userObject
		 */
		public SortedTreeNode(Object userObject) {
			super(userObject);
		}
		@SuppressWarnings("unchecked")
		@Override
		public void add(MutableTreeNode newChild) {
			super.add(newChild);
			
			Collections.sort(children, this);
		}
		public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
			Object f1= o1.getUserObject();
			Object f2= o2.getUserObject();
			if (f1 instanceof Folder && f2 instanceof Folder) {
				return ((Folder)f1).getName().compareTo(((Folder)f2).getName());
			}
			return 0;
		}
		
		@SuppressWarnings("unchecked")
		public void sort() {
			if (children!=null) {
				Collections.sort(children, this);
			}
		}
	}
	
	private GTDModel gtdModel;
	private GTDFreeEngine engine;
	private DefaultMutableTreeNode root;
	private DefaultTreeModel model;
	private SortedTreeNode actions;
	private SortedTreeNode meta;
	private SortedTreeNode references;
	private SortedTreeNode projects;
	private SortedTreeNode someday;
	private Folder selectedFolder;
	private boolean defaultFoldersVisible=true;
	private boolean showClosedFolders=false;
	private ActionTransferHandler transferHandler;
	private boolean showEmptyFolders=true;
	private JPopupMenu popupMenu;
	private AbstractAction deleteAllAction;
	private AbstractAction unqueueAllAction;
	private AbstractAction queueAllAction;
	private AbstractAction resolveAllAction;
	private AbstractAction renameListAction;
	private AbstractAction renameProjectAction;
	private AbstractAction addProjectAction;
	private AbstractAction addListAction;
	private AbstractAction addReferenceListAction;
	private AbstractAction addSomedayListAction;
	private AbstractAction closeListAction;
	private AbstractAction closeProjectAction;
	private int lastDroppedActionIndex=-1;

	/**
	 * @return the selectedFolder
	 */
	public Folder getSelectedFolder() {
		return selectedFolder;
	}

	public FolderTree() {
		initialize();
	}

	private void initialize() {
		
		root= new DefaultMutableTreeNode(null,true);
		model  = new DefaultTreeModel(root, true);
		setModel(model);
		
		setRootVisible(false);
		setExpandsSelectedPaths(true);
		setToggleClickCount(0);
		setEditable(true);
		setShowsRootHandles(true);
		
		actions= new SortedTreeNode(Messages.getString("FolderTree.Act"),true); //$NON-NLS-1$
		meta= new SortedTreeNode(Messages.getString("FolderTree.Def"),true); //$NON-NLS-1$
		references= new SortedTreeNode(Messages.getString("FolderTree.Ref"),true); //$NON-NLS-1$
		someday= new SortedTreeNode(Messages.getString("FolderTree.Some"),true); //$NON-NLS-1$
		projects= new SortedTreeNode(Messages.getString("FolderTree.Proj"),true); //$NON-NLS-1$
		
		addTreeSelectionListener(new TreeSelectionListener() {
		
			public void valueChanged(TreeSelectionEvent e) {
				Folder old= selectedFolder;
				selectedFolder= null;
				if (e.getNewLeadSelectionPath()!=null) {
					selectedFolder= treePath2Folder(e.getNewLeadSelectionPath());
				}
				firePropertyChange("selectedFolder", old, selectedFolder); //$NON-NLS-1$
			}
		
		});
		
		setCellRenderer(new FolderTreeCellRenderer());
		
		setCellEditor(new FolderTreeCellEditor(this, (DefaultTreeCellRenderer)getCellRenderer()));
		
		setTransferHandler(transferHandler= new FolderTreeTransferHandler());
		
		addMouseListener(new MouseAdapter() {
			private void popup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					TreePath t= getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
					showPopup(t,e.getPoint());
				}
			}
			@Override
			public void mousePressed(MouseEvent e) {
				/*if (getPathForLocation(e.getPoint().x, e.getPoint().y)!=null || e.getPoint().x<30) {
					return;
				}*/
				TreePath t= getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
				setSelectionPath(t);
				popup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				/*if (getPathForLocation(e.getPoint().x, e.getPoint().y)!=null || e.getPoint().x<30) {
					return;
				}*/
				TreePath t= getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
				setSelectionPath(t);
				popup(e);
			}
		});
		
		addTreeExpansionListener(new TreeExpansionListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				TreePath tp= event.getPath();
				
				Enumeration<DefaultMutableTreeNode> en= ((DefaultMutableTreeNode)tp.getLastPathComponent()).children();
				
				while(en.hasMoreElements()) {
					DefaultMutableTreeNode n = en.nextElement();
					Object o= n.getUserObject();
					if (o instanceof Folder) {
						checkIfShow((Folder)o);
					}
				}
			}
			
			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				//
			}
		});
		
		rebuildTree();
		
		
	}
	
	public Folder treePath2Folder(TreePath tp) {
		Object o= ((DefaultMutableTreeNode)tp.getLastPathComponent()).getUserObject();
		if (o instanceof Folder) {
			return (Folder)o;
		}
		return null;
	}

	protected void showPopup(TreePath t, Point point) {
		if (popupMenu == null) {
			popupMenu = new JPopupMenu();
		}
		
		popupMenu.removeAll();
		
		Folder f= treePath2Folder(t);
		
		if (f!=null) {
			if (f.isProject()) {
				popupMenu.add(getRenameProjectAction());
				popupMenu.add(getAddProjectAction());
				popupMenu.add(getCloseProjectAction());
				getCloseProjectAction().setEnabled(f.getOpenCount()==0);
				popupMenu.add(new JSeparator());
			} else if (!f.isDefault()) {
				popupMenu.add(getRenameListAction());
				if (f.isReference()) {
					popupMenu.add(getAddReferenceListAction());
				} else if (f.isSomeday()) {
					popupMenu.add(getAddSomedayListAction());
				} else {
					popupMenu.add(getAddListAction());
				}
				popupMenu.add(getCloseListAction());
				getCloseListAction().setEnabled(f.getOpenCount()==0);
				popupMenu.add(new JSeparator());
			}
			if (!f.isQueue() && !f.isInBucket() && f.getType()!=FolderType.BUILDIN_RESOLVED && f.getType()!=FolderType.BUILDIN_DELETED) {
				popupMenu.add(getQueueAllAction());
			}
			if (!f.isInBucket() && f.getType()!=FolderType.BUILDIN_RESOLVED && f.getType()!=FolderType.BUILDIN_DELETED) {
				popupMenu.add(getUnqueueAllAction());
			}
			if (f.getType()!=FolderType.BUILDIN_RESOLVED && f.getType()!=FolderType.BUILDIN_DELETED) {
				popupMenu.add(getResolveAllAction());
			}
			if (f.getType()!=FolderType.BUILDIN_DELETED) {
				popupMenu.add(getDeleteAllAction());
			}
		} else if (t.getPathCount()>0) {
			Object o= t.getPathComponent(1);
			
			if (o==actions) {
				popupMenu.add(getAddListAction());
			} else if (o==someday) {
				popupMenu.add(getAddSomedayListAction());
			} else if (o==references) {
				popupMenu.add(getAddReferenceListAction());
			} else if (o==projects) {
				popupMenu.add(getAddProjectAction());
			}	
		}
		popupMenu.show(this, point.x, point.y);
	}

	private javax.swing.Action getAddProjectAction() {
		if (addProjectAction == null) {
			addProjectAction = new AbstractAction(Messages.getString("FolderTree.AddProject"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_add)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Folder f= getGTDModel().createFolder("", FolderType.PROJECT); //$NON-NLS-1$
					for (int i = 0; i < getRowCount(); i++) {
						TreePath tp= getPathForRow(i);
						if (tp != null && tp.getLastPathComponent()==projects ) {
							expandRow(i);
						} else if (tp != null && tp.getLastPathComponent() instanceof FolderTreeNode && ((FolderTreeNode)tp.getLastPathComponent()).getUserObject()==f ) {
							startEditingAtPath(tp);
							return;
						}
					}
				}
			};
			
		}

		return addProjectAction;
	}

	private javax.swing.Action getAddListAction() {
		if (addListAction == null) {
			addListAction = new AbstractAction(Messages.getString("FolderTree.AddList"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_add)) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Folder f= getGTDModel().createFolder("", FolderType.ACTION); //$NON-NLS-1$
					for (int i = 0; i < getRowCount(); i++) {
						TreePath tp= getPathForRow(i);
						if (tp != null && tp.getLastPathComponent()==actions ) {
							expandRow(i);
						} else if (tp != null && tp.getLastPathComponent() instanceof FolderTreeNode && ((FolderTreeNode)tp.getLastPathComponent()).getUserObject()==f ) {
							startEditingAtPath(tp);
							return;
						}
					}
				}
			};
			
		}

		return addListAction;
	}
	
	private javax.swing.Action getAddReferenceListAction() {
		if (addReferenceListAction == null) {
			addReferenceListAction = new AbstractAction(Messages.getString("FolderTree.AddRefList"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_add)) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Folder f= getGTDModel().createFolder("", FolderType.REFERENCE); //$NON-NLS-1$
					for (int i = 0; i < getRowCount(); i++) {
						TreePath tp= getPathForRow(i);
						if (tp != null && tp.getLastPathComponent()==references ) {
							expandRow(i);
						} else if (tp != null && tp.getLastPathComponent() instanceof FolderTreeNode && ((FolderTreeNode)tp.getLastPathComponent()).getUserObject()==f ) {
							startEditingAtPath(tp);
							return;
						}
					}
				}
			};
			
		}

		return addReferenceListAction;
	}

	private javax.swing.Action getAddSomedayListAction() {
		if (addSomedayListAction == null) {
			addSomedayListAction = new AbstractAction(Messages.getString("FolderTree.AddSomeList"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_add)) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Folder f= getGTDModel().createFolder("", FolderType.SOMEDAY); //$NON-NLS-1$
					for (int i = 0; i < getRowCount(); i++) {
						TreePath tp= getPathForRow(i);
						if (tp != null && tp.getLastPathComponent()==someday ) {
							expandRow(i);
						} else if (tp != null && tp.getLastPathComponent() instanceof FolderTreeNode && ((FolderTreeNode)tp.getLastPathComponent()).getUserObject()==f ) {
							startEditingAtPath(tp);
							return;
						}
					}
				}
			};
			
		}

		return addSomedayListAction;
	}

	private javax.swing.Action getResolveAllAction() {
		if (resolveAllAction == null) {
			resolveAllAction = new AbstractAction(Messages.getString("FolderTree.ResolveActions"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_resolve)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Iterator<Action> i= getSelectedFolder().iterator(FolderPreset.OPEN);
					while (i.hasNext()) {
						Action a = i.next();
						a.setResolution(Resolution.RESOLVED);
					}
				}
			};
		}

		return resolveAllAction;
	}

	private javax.swing.Action getUnqueueAllAction() {
		if (unqueueAllAction == null) {
			unqueueAllAction = new AbstractAction(Messages.getString("FolderTree.UnqueueActions"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_queue_off)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Iterator<Action> i= getSelectedFolder().iterator(FolderPreset.OPEN);
					while (i.hasNext()) {
						Action a = i.next();
						a.setQueued(false);
					}
				}
			};
		}

		return unqueueAllAction;
	}

	private javax.swing.Action getQueueAllAction() {
		if (queueAllAction == null) {
			queueAllAction = new AbstractAction(Messages.getString("FolderTree.QueueActions"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_queue_on)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Iterator<Action> i= getSelectedFolder().iterator(FolderPreset.OPEN);
					while (i.hasNext()) {
						Action a = i.next();
						a.setQueued(true);
					}
				}
			};
		}

		return queueAllAction;
	}

	private javax.swing.Action getRenameListAction() {
		if (renameListAction == null) {
			renameListAction = new AbstractAction(Messages.getString("FolderTree.RenameList"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_rename)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					startEditingAtPath(getSelectionPath());
				}
			};
		}

		return renameListAction;
	}

	private javax.swing.Action getCloseListAction() {
		if (closeListAction == null) {
			closeListAction = new AbstractAction(Messages.getString("FolderTree.CloseList"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_resolve)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (getSelectedFolder().getOpenCount()==0) {
						getSelectedFolder().setClosed(true);
					}
				}
			};
		}

		return closeListAction;
	}

	private javax.swing.Action getCloseProjectAction() {
		if (closeProjectAction == null) {
			closeProjectAction = new AbstractAction(Messages.getString("FolderTree.CloseProject"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_resolve)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (getSelectedFolder().getOpenCount()==0) {
						getSelectedFolder().setClosed(true);
					}
				}
			};
		}

		return closeProjectAction;
	}

	private javax.swing.Action getRenameProjectAction() {
		if (renameProjectAction == null) {
			renameProjectAction = new AbstractAction(Messages.getString("FolderTree.RenameProject"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_rename)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					startEditingAtPath(getSelectionPath());
				}
			};
		}

		return renameProjectAction;
	}

	private javax.swing.Action getDeleteAllAction() {
		if (deleteAllAction == null) {
			deleteAllAction = new AbstractAction(Messages.getString("FolderTree.DeleteActions"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_delete)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Iterator<Action> i= getSelectedFolder().iterator(FolderPreset.ALL);
					while (i.hasNext()) {
						Action a = i.next();
						a.setResolution(Resolution.DELETED);
					}
				}
			};
		}

		return deleteAllAction;
	}


	/**
	 * @return the gtdModel
	 */
	GTDModel getGTDModel() {
		return gtdModel;
	}

	/**
	 * @param gtdModel the gtdModel to set
	 */
	private void setGTDModel(GTDModel gtdModel) {
		this.gtdModel = gtdModel;
		transferHandler.setModel(gtdModel);
		gtdModel.addGTDModelListener(new GTDModelListener() {
		
			public void elementRemoved(FolderEvent note) {
				model.nodeChanged(folderToNode(note.getFolder()));
				repaint();
			}
		
			public void elementModified(ActionEvent note) {
				int[] i= new int[meta.getChildCount()];
				for (int j = 0; j < i.length; j++) {
					i[j]=j;
				}
				model.nodesChanged(meta, i);
				repaint();
				
				if (note.getProperty()==Action.RESOLUTION_PROPERTY_NAME) {
					HashSet<Integer> checked= new HashSet<Integer>();
					for (Action a : note.getActions()) {
						Folder f= a.getParent();
						if (!checked.contains(f.getId())) {
							checked.add(f.getId());
							checkIfShow(f);
						}
					}
				}
			}
		
			public void elementAdded(FolderEvent note) {
				//if (!checkIfShow(note.getFolder())) {
					model.nodeChanged(folderToNode(note.getFolder()));
					repaint();
				//}
			}
		
			public void folderRemoved(Folder folder) {
				removeFromTree(folder, true);
			}
		
			public void folderModified(FolderEvent f) {
				if (f.getProperty()=="closed") { //$NON-NLS-1$
					checkIfShow(f.getFolder());
				} else if (f.getProperty()=="name") { //$NON-NLS-1$
					Folder sel= getSelectedFolder();
					SortedTreeNode p= folderToParentNode(f.getFolder());
					if (p!=null) {
						p.sort();
						model.nodeStructureChanged(p);
					}
					if (sel!=null) {
						setSelectedFolder(sel,-1);
					}
				} else {
					DefaultMutableTreeNode p= folderToNode(f.getFolder());
					if (p!=null) {
						model.nodeChanged(p);
					}
				}
				repaint();
			}
		
			public void folderAdded(Folder folder) {
				addToTree(folder, true);
			}
			public void orderChanged(Folder f) {
				//
			}
		
		});		
		rebuildTree();
	}
	
	private void rebuildTree() {
		//root.removeAllChildren();

		boolean rootChange=false;
		int pos=0;
		if (root.getChildCount()>pos && root.getChildAt(pos)!=actions) {
			root.removeAllChildren();
			rootChange=true;
		}
		if (root.getChildCount()==pos) {
			root.add(actions);
			rootChange=true;
		}
		pos++;
		
		if (defaultFoldersVisible) {
			while (root.getChildCount()>pos && root.getChildAt(pos)!=meta) {
				root.remove(root.getChildCount()-1);
				rootChange=true;
			}
			if (root.getChildCount()==pos) {
				root.add(meta);
				rootChange=true;
			}
			pos++;
			while (root.getChildCount()>pos && root.getChildAt(pos)!=projects) {
				root.remove(root.getChildCount()-1);
				rootChange=true;
			}
			if (root.getChildCount()==pos) {
				root.add(projects);
				rootChange=true;
			}
			pos++;
		}
		while (root.getChildCount()>pos && root.getChildAt(pos)!=someday) {
			root.remove(root.getChildCount()-1);
			rootChange=true;
		}
		if (root.getChildCount()==pos) {
			root.add(someday);
			rootChange=true;
		}
		pos++;
		while (root.getChildCount()>pos && root.getChildAt(pos)!=references) {
			root.remove(root.getChildCount()-1);
			rootChange=true;
		}
		if (root.getChildCount()==pos) {
			root.add(references);
			rootChange=true;
		}

		actions.removeAllChildren();
		someday.removeAllChildren();
		references.removeAllChildren();
		projects.removeAllChildren();
		meta.removeAllChildren();
		
		if (gtdModel!=null) {
			for (Folder f : gtdModel) {
				if (canShowFolder(f)) {
					addToTree(f, false);
				}
			}
		}
		
		if (rootChange) {
			model.nodeStructureChanged(root);
		} else {
			model.nodeStructureChanged(actions);
			model.nodeStructureChanged(someday);
			model.nodeStructureChanged(references);
			model.nodeStructureChanged(projects);
			model.nodeStructureChanged(meta);
			
		}
	}
	private DefaultMutableTreeNode addToTree(Folder f, boolean notify) {
		TreeNode n=null;
		DefaultMutableTreeNode r=null;
		if (f.isAction()) {
			actions.add(r=new FolderTreeNode(f));
			n=actions;
		} else if (f.isReference()) {
			references.add(r=new FolderTreeNode(f));
			n=references;
		} else if (f.isSomeday()) {
			someday.add(r=new FolderTreeNode(f));
			n=someday;
		} else if (defaultFoldersVisible && f.isDefault()) {
			meta.add(r=new FolderTreeNode(f));
			n=meta;
		} else if (defaultFoldersVisible && f.isProject()) {
			projects.add(r=new FolderTreeNode(f));
			n=projects;
		}
		if (notify && n!=null) {
			model.nodeStructureChanged(n);
		}
		return r;
	}
	private DefaultMutableTreeNode removeFromTree(Folder f, boolean notify) {
		DefaultMutableTreeNode dn= folderToNode(f);
		if (dn!=null && dn.getUserObject()==f) {
			DefaultMutableTreeNode p= (DefaultMutableTreeNode)dn.getParent(); 
			int[] id= {p.getIndex(dn)};
			p.remove(id[0]);
			if (notify) {
				//model.nodeStructureChanged(p);
				model.nodesWereRemoved(p, id, new Object[]{dn});
				repaint();
			}
			return dn;
		}
		return null;
	}
	
	/**
	 * Checks if folder could be shown in tree
	 * @param f
	 * @return <code>true</code> if folder could be shown in tree
	 */
	private boolean canShowFolder(Folder f) {
		return f.isDefault() || (showClosedFolders && f.isClosed()) ||
		(!f.isClosed() && (showEmptyFolders || f.getOpenCount()>0));
	}
	
	/**
	 * Checks if folder could be shown or not and adds/removes if necessary.
	 * @param f
	 * @return <code>true</code> if folder was added or removed, otherwise false
	 */
	private boolean checkIfShow(Folder f) {
		DefaultMutableTreeNode node= folderToNode(f);
		boolean show= canShowFolder(f);
		if (show && node==null) {
			addToTree(f, true);
			return true;
		} 
		if (!show && node!=null) {
			removeFromTree(f, true);
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public DefaultMutableTreeNode folderToNode(Folder f) {
		Enumeration<DefaultMutableTreeNode> en=null;
		DefaultMutableTreeNode p= folderToParentNode(f);
		if (p!=null) {
			en= p.children();
		}
		if (en!=null) {
			while (en.hasMoreElements()) {
				DefaultMutableTreeNode n= en.nextElement();
				if (n.getUserObject()==f) {
					return n;
				}
			}
		}
		return null;
	}

	public SortedTreeNode folderToParentNode(Folder f) {
		if (f.getType()==FolderType.ACTION) {
			return actions;
		} else if (f.isReference()) {
			return references;
		} else if (f.getType()==FolderType.SOMEDAY) {
			return someday;
		} else if (engine.getStateMachine().getDefaultTreeBranch(f.getType())) {
			return meta;
		} else if (f.getType()==FolderType.PROJECT ) {
			return projects;
		}
		
		return null;
	}
	
	public void addFolder(String name) {
		
		FolderType type= FolderType.ACTION;
		
		if (getSelectionPath()!=null && getSelectionPath().getPathCount()>0 ) {
			if (getSelectionPath().getPathComponent(1)==references) {
				type=FolderType.REFERENCE;
			} else if (getSelectionPath().getPathComponent(1)==someday) {
				type=FolderType.SOMEDAY;
			} else if (getSelectionPath().getPathComponent(1)==projects) {
				type=FolderType.PROJECT;
			}
		}

		Folder f= gtdModel.createFolder(name, type);
		setSelectionPath(new TreePath(model.getPathToRoot(folderToNode(f))));
	}

	public boolean isFolderRenamePossible() {
		return getSelectedFolder()!=null && (getSelectedFolder().isUserFolder() || getSelectedFolder().isProject());
	}
	
	public boolean isFolderAddPossible() {
		if (getSelectionPath()!=null && getSelectionPath().getPathCount()>0 ) {
			if (getSelectionPath().getPathComponent(1)==someday ||getSelectionPath().getPathComponent(1)==references || getSelectionPath().getPathComponent(1)==actions || getSelectionPath().getPathComponent(1)==projects) {
				return true;
			}
		}
		return false;
	}

	
	/**
	 * @return the defaultFoldersVisible
	 */
	public boolean isDefaultFoldersVisible() {
		return defaultFoldersVisible;
	}

	/**
	 * @param defaultFoldersVisible the defaultFoldersVisible to set
	 */
	public void setDefaultFoldersVisible(boolean defaultFoldersVisible) {
		this.defaultFoldersVisible = defaultFoldersVisible;
		rebuildTree();
	}

	/**
	 * @return the showClosedFolders
	 */
	public boolean isShowClosedFolders() {
		return showClosedFolders;
	}

	/**
	 * @param showClosedFolders the showClosedFolders to set
	 */
	public void setShowClosedFolders(boolean showClosedFolders) {
		this.showClosedFolders = showClosedFolders;
		rebuildTree();
	}

	public void setExpendedNodes(int[] ii) {
		if (ii==null || ii.length==0) {
			return;
		}
		
		int i= 0;
		while (i<getRowCount()) {
			collapseRow(i++);
		}
		
		Arrays.sort(ii);
		
		for (i = 0; i < ii.length; i++) {
			if (i>-1) {
				expandRow(ii[i]);
			}
		}
	}

	public int[] getExpendedNodes() {
		
		List<Integer> i= new ArrayList<Integer>();
		Enumeration<TreePath> en= getExpandedDescendants(new TreePath(root.getPath()));
		
		if (en==null) {
			return null;
		}
		
		while(en.hasMoreElements()) {
			TreePath tp= en.nextElement();
			int r= getRowForPath(tp);
			if (r>-1) {
				i.add(r);
			}
		}
		int[] ii= new int[i.size()];
		
		for (int j = 0; j < ii.length; j++) {
			ii[j]=i.get(j);
		}
		
		Arrays.sort(ii);
		return ii;
	}

	/**
	 * @param engine the engine to set
	 */
	public void setEngine(GTDFreeEngine engine) {
		this.engine = engine;
		setGTDModel(engine.getGTDModel());
	}

	/**
	 * @return the engine
	 */
	public GTDFreeEngine getEngine() {
		return engine;
	}

	public void setSelectedFolder(Folder f, int i) {
		lastDroppedActionIndex=i;
		DefaultMutableTreeNode n= folderToNode(f);
		if (n!=null) {
			setSelectionPath(new TreePath(folderToNode(f).getPath()));
		}
	}

	public void setShowEmptyFolders(boolean b) {
		this.showEmptyFolders = b;
		rebuildTree();
	}
	
	public int getLastDroppedActionIndex() {
		return lastDroppedActionIndex;
	}
	
	@Override
	public boolean isPathEditable(TreePath path) {
		if (!isEditable()) {
			return false;
		}
	    if (path!=null) {
			Object value = path.getLastPathComponent();
			if (value instanceof FolderTreeNode) {
				return !((FolderTreeNode)value).folder.isDefault();
			}
	    }
		return false;
	}

}
