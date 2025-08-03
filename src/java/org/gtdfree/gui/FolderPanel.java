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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDModelAdapter;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public class FolderPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private FolderTree folderTree;
	private AbstractAction addFolderAction;
	private JTextField folderNameField;
	private AbstractAction renameFolderAction;
	private GTDFreeEngine engine;
	private AbstractAction closeFolderAction;
	private AbstractAction reopenFolderAction;
	private FoldingPanel foldingPanel;
	private UndoManager undoManager;
	private AbstractAction undoAction;
	private AbstractAction redoAction;

	public FolderPanel() {
		initialize();
	}
	
	private void initialize() {
		setLayout(new GridBagLayout());
		
		folderTree= new FolderTree();
		folderTree.addPropertyChangeListener("selectedFolder", new PropertyChangeListener() { //$NON-NLS-1$
		
			public void propertyChange(PropertyChangeEvent evt) {
				firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
				
				folderNameField.setEnabled(folderTree.isFolderRenamePossible() || folderTree.isFolderAddPossible());
				
				getRenameFolderAction().setEnabled(folderTree.isFolderRenamePossible());
				getAddFolderAction().setEnabled(folderTree.isFolderAddPossible());
				getCloseFolderAction().setEnabled(getSelectedFolder()!=null && !getSelectedFolder().isBuildIn() && getSelectedFolder().getType()!=FolderType.INBUCKET && getSelectedFolder().getType()!=FolderType.QUEUE && !getSelectedFolder().isClosed() && getSelectedFolder().getOpenCount()==0);
				getReopenFolderAction().setEnabled(getSelectedFolder()!=null && getSelectedFolder().isClosed());
		
				if (getSelectedFolder()!=null) {
					folderNameField.setText(getSelectedFolder().getName());
					undoManager.discardAllEdits();
				}
			}
		
		});
		add(new JScrollPane(folderTree), new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
		
		JPanel p= new JPanel();
		p.setLayout(new GridBagLayout());
		
		folderNameField= new JTextField();
		folderNameField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (getRenameFolderAction().isEnabled()) {
					getRenameFolderAction().actionPerformed(e);
				} else if (getAddFolderAction().isEnabled()) {
					getAddFolderAction().actionPerformed(e);
				}
			}
		});
		p.add(folderNameField,new GridBagConstraints(0,0,4,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));

		InputMap imap= folderNameField.getInputMap(); 
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "Add"); //$NON-NLS-1$
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "Add"); //$NON-NLS-1$
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo"); //$NON-NLS-1$
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo"); //$NON-NLS-1$

		ActionMap amap= folderNameField.getActionMap();  
		
		undoManager= new UndoManager();
		undoManager.setLimit(100);
		
		undoAction= new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					undoManager.undo();
				} catch (Exception ex) {
					Logger.getLogger(this.getClass()).debug("Internal error.", ex); //$NON-NLS-1$
				}
				updateUndoRedoActions();
			}
		};
		amap.put("Undo", undoAction); //$NON-NLS-1$
		
		redoAction= new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					undoManager.redo();
				} catch (Exception ex) {
					Logger.getLogger(this.getClass()).debug("Internal error.", ex); //$NON-NLS-1$
				}
				updateUndoRedoActions();
			}
		};
		amap.put("Redo", redoAction); //$NON-NLS-1$
		
		amap.put(Messages.getString("FolderPanel.Add"), getAddFolderAction()); //$NON-NLS-1$
		
		folderNameField.getDocument().addUndoableEditListener(new UndoableEditListener() {
			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				undoManager.addEdit(e.getEdit());
		        updateUndoRedoActions();
			}
		});
		
        updateUndoRedoActions();
		
		
		
		JButton b= new JButton(getRenameFolderAction());
		b.setText(null);
		b.setMargin(new Insets(1,1,1,1));
		p.add(b,new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));

		b= new JButton(getAddFolderAction());
		b.setText(null);
		b.setMargin(new Insets(1,1,1,1));
		p.add(b,new GridBagConstraints(1,1,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));
		
		b= new JButton(getCloseFolderAction());
		b.setText(null);
		b.setMargin(new Insets(1,1,1,1));
		p.add(b,new GridBagConstraints(2,1,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));

		b= new JButton(getReopenFolderAction());
		b.setText(null);
		b.setMargin(new Insets(1,1,1,1));
		p.add(b,new GridBagConstraints(3,1,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));

		foldingPanel= new FoldingPanel();
		foldingPanel.addFold(Messages.getString("FolderPanel.Edit"), p, true, false); //$NON-NLS-1$
		add(foldingPanel, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
	}

	public Folder getSelectedFolder() {
		return folderTree.getSelectedFolder();
	}

	private Action getAddFolderAction() {
		if (addFolderAction == null) {
			addFolderAction = new AbstractAction(Messages.getString("FolderPanel.Add"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_add)) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 0L;
				
				public void actionPerformed(ActionEvent e) {
					folderTree.addFolder(folderNameField.getText());
				}
			};
			addFolderAction.putValue(Action.SHORT_DESCRIPTION, Messages.getString("FolderPanel.Add.desc")); //$NON-NLS-1$
			addFolderAction.setEnabled(false);
		}
	
		return addFolderAction;
	}

	private Action getRenameFolderAction() {
		if (renameFolderAction == null) {
			renameFolderAction = new AbstractAction(Messages.getString("FolderPanel.Ren"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_rename)) { //$NON-NLS-1$
				private static final long serialVersionUID = 0L;
				public void actionPerformed(ActionEvent e) {
					getSelectedFolder().rename(folderNameField.getText());
				}
			};
			renameFolderAction.putValue(Action.SHORT_DESCRIPTION, Messages.getString("FolderPanel.Ren.desc")); //$NON-NLS-1$
			renameFolderAction.setEnabled(false);
		}
	
		return renameFolderAction;
	}

	private Action getCloseFolderAction() {
		if (closeFolderAction == null) {
			closeFolderAction = new AbstractAction(Messages.getString("FolderPanel.Close"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_delete)) { //$NON-NLS-1$
				private static final long serialVersionUID = 0L;
				public void actionPerformed(ActionEvent e) {
					if (getSelectedFolder().getOpenCount()==0) {
						getSelectedFolder().setClosed(true);
					}
				}
			};
			closeFolderAction.putValue(Action.SHORT_DESCRIPTION, Messages.getString("FolderPanel.Close.desc")); //$NON-NLS-1$
			closeFolderAction.setEnabled(false);
		}
		return closeFolderAction;
	}

	private Action getReopenFolderAction() {
		if (reopenFolderAction == null) {
			reopenFolderAction = new AbstractAction(Messages.getString("FolderPanel.Reop"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_undelete)) { //$NON-NLS-1$
				private static final long serialVersionUID = 0L;
				public void actionPerformed(ActionEvent e) {
					getSelectedFolder().setClosed(false);
				}
			};
			reopenFolderAction.putValue(Action.SHORT_DESCRIPTION, Messages.getString("FolderPanel.Reop.desc")); //$NON-NLS-1$
			reopenFolderAction.setEnabled(false);
		}
		return reopenFolderAction;
	}

	/**
	 * @param engine the engine to set
	 */
	public void setEngine(GTDFreeEngine engine) {
		this.engine = engine;
		folderTree.setEngine(this.engine);
		engine.getGTDModel().addGTDModelListener(new GTDModelAdapter() {
			@Override
			public void elementModified(org.gtdfree.model.ActionEvent a) {
				if (a.getAction().getParent()==getSelectedFolder() || (getSelectedFolder()!=null && a.getAction().getProject()!=null && getSelectedFolder().getId()==a.getAction().getProject())) {
					getCloseFolderAction().setEnabled(getSelectedFolder()!=null && !getSelectedFolder().isBuildIn() && getSelectedFolder().getType()!=FolderType.INBUCKET && getSelectedFolder().getType()!=FolderType.QUEUE && !getSelectedFolder().isClosed() && getSelectedFolder().getOpenCount()==0);
					getReopenFolderAction().setEnabled(getSelectedFolder()!=null && getSelectedFolder().isClosed());
				}
			}
		});
	}

	public void setDefaultFoldersVisible(boolean b) {
		folderTree.setDefaultFoldersVisible(b);
	}
	
	public boolean isDefaultFoldersVisible() {
		return folderTree.isDefaultFoldersVisible();
	}
	
	public void setShowClosedFolders(boolean b) {
		folderTree.setShowClosedFolders(b);
	}

	public boolean isShowClosedFolders() {
		return folderTree.isShowClosedFolders();
	}

	public void setExpendedNodes(int[] ii) {
		folderTree.setExpendedNodes(ii);
	}
	public int[] getExpendedNodes() {
		return folderTree.getExpendedNodes();
	}
	public boolean[] getFoldingStates() {
		return foldingPanel.getFoldingStates();
	}
	public void setFoldingStates(boolean[] b) {
		foldingPanel.setFoldingStates(b);
	}

	public void setSelectedFolder(Folder f) {
		folderTree.setSelectedFolder(f,-1);
	}

	public void setShowEmptyFolders(boolean b) {
		folderTree.setShowEmptyFolders(b);
	}
	
	public int getLastDroppedActionIndex() {
		return folderTree.getLastDroppedActionIndex();
	}
	
	private void updateUndoRedoActions() {
		undoAction.setEnabled(undoManager.canUndo());
		redoAction.setEnabled(undoManager.canRedo());
	}
}
