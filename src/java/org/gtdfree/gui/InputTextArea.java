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

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public class InputTextArea extends JTextArea {

	private static final long serialVersionUID = 1L;
	private UndoManager undoManager;
	private AbstractAction undoAction;
	private AbstractAction redoAction;
	private JPopupMenu popup;
	private AbstractAction selectionAsNewItem;
	private GTDFreeEngine engine;
	private AbstractAction selectionAsNewActionList;
	private AbstractAction selectionAsNewSomedayList;
	private AbstractAction selectionAsNewReferencelist;
	private AbstractAction selectionAsNewProject;
	
	
	public static void main(String[] args) {
		try {
			JFrame f= new JFrame();
			f.setContentPane(new JScrollPane(new InputTextArea()));
			f.setSize(500, 500);
			f.setLocationRelativeTo(null);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public InputTextArea() {
		initialize();
	}

	private void initialize() {
		
		setWrapStyleWord(true);
		setLineWrap(true);
		setMargin(new Insets(2,4,2,4));
		
		InputMap imap= getInputMap(); 
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "NewLine"); //$NON-NLS-1$
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "NewLine"); //$NON-NLS-1$
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo"); //$NON-NLS-1$
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo"); //$NON-NLS-1$
		
		ActionMap amap= getActionMap();  
		amap.put("NewLine", new AbstractAction() { //$NON-NLS-1$
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				insert("\n", getCaretPosition()); //$NON-NLS-1$
			}
		});
		
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
		undoAction.putValue(AbstractAction.NAME,  Messages.getString("InputTextArea.Undo")); //$NON-NLS-1$
		undoAction.putValue(AbstractAction.LONG_DESCRIPTION,  Messages.getString("InputTextArea.Undo.desc")); //$NON-NLS-1$
		undoAction.putValue(AbstractAction.SMALL_ICON,ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_undo));
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
		redoAction.putValue(AbstractAction.NAME,  Messages.getString("InputTextArea.Redo")); //$NON-NLS-1$
		redoAction.putValue(AbstractAction.LONG_DESCRIPTION,  Messages.getString("InputTextArea.Redo.desc")); //$NON-NLS-1$
		redoAction.putValue(AbstractAction.SMALL_ICON,ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_redo));
		amap.put("Redo", redoAction); //$NON-NLS-1$
		
		getDocument().addUndoableEditListener(new UndoableEditListener() {
			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				undoManager.addEdit(e.getEdit());
		        updateUndoRedoActions();
			}
		});
		
        updateUndoRedoActions();
        
        JMenuItem jm= new JMenuItem();
        jm.setAction(amap.get("cut-to-clipboard")); //$NON-NLS-1$
        jm.setText(Messages.getString("InputTextArea.Cut")); //$NON-NLS-1$
        jm.setToolTipText(Messages.getString("InputTextArea.Cut.desc")); //$NON-NLS-1$
        jm.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_cut));
        getPopupMenu().add(jm);
        
        jm= new JMenuItem();
        jm.setAction(amap.get("copy-to-clipboard")); //$NON-NLS-1$
        jm.setText(Messages.getString("InputTextArea.Copy")); //$NON-NLS-1$
        jm.setToolTipText(Messages.getString("InputTextArea.Copy.desc")); //$NON-NLS-1$
        jm.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_copy));
        getPopupMenu().add(jm);

        jm= new JMenuItem();
        jm.setAction(amap.get("paste-from-clipboard")); //$NON-NLS-1$
        jm.setText(Messages.getString("InputTextArea.Paste")); //$NON-NLS-1$
        jm.setToolTipText(Messages.getString("InputTextArea.Paste.desc")); //$NON-NLS-1$
        jm.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_paste));
        getPopupMenu().add(jm);

        jm= new JMenuItem();
        jm.setAction(amap.get("select-all")); //$NON-NLS-1$
        jm.setText(Messages.getString("InputTextArea.SelAll")); //$NON-NLS-1$
        jm.setToolTipText(Messages.getString("InputTextArea.SelAll.desc")); //$NON-NLS-1$
        jm.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_select_all));
        getPopupMenu().add(jm);
        
        getPopupMenu().add(new JSeparator());
        getPopupMenu().add(undoAction);
        getPopupMenu().add(redoAction);
        
        
        getPopupMenu().add(new JSeparator());

        selectionAsNewItem = new AbstractAction() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (getSelectedText()!=null && getSelectedText().length()>0) {
					engine.getGTDModel().collectAction(getSelectedText());
				}
			}
		};
		selectionAsNewItem.putValue(Action.NAME, Messages.getString("InputTextArea.NewItem")); //$NON-NLS-1$
		selectionAsNewItem.putValue(AbstractAction.LONG_DESCRIPTION,  Messages.getString("InputTextArea.NewItem.desc")); //$NON-NLS-1$
        getPopupMenu().add(selectionAsNewItem);
        
        selectionAsNewActionList = new AbstractAction() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (getSelectedText()!=null && getSelectedText().length()>0) {
					engine.getGTDModel().createFolder(getSelectedText(),FolderType.ACTION);
				}
			}
		};
		selectionAsNewActionList.putValue(Action.NAME, Messages.getString("InputTextArea.NewActionList")); //$NON-NLS-1$
		selectionAsNewActionList.putValue(AbstractAction.LONG_DESCRIPTION,  Messages.getString("InputTextArea.NewList.desc")); //$NON-NLS-1$
        getPopupMenu().add(selectionAsNewActionList);

        selectionAsNewSomedayList = new AbstractAction() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (getSelectedText()!=null && getSelectedText().length()>0) {
					engine.getGTDModel().createFolder(getSelectedText(),FolderType.SOMEDAY);
				}
			}
		};
		selectionAsNewSomedayList.putValue(Action.NAME, Messages.getString("InputTextArea.NewSomedayList")); //$NON-NLS-1$
		selectionAsNewSomedayList.putValue(AbstractAction.LONG_DESCRIPTION,  Messages.getString("InputTextArea.NewList.desc")); //$NON-NLS-1$
        getPopupMenu().add(selectionAsNewSomedayList);

        selectionAsNewReferencelist = new AbstractAction() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (getSelectedText()!=null && getSelectedText().length()>0) {
					engine.getGTDModel().createFolder(getSelectedText(),FolderType.REFERENCE);
				}
			}
		};
		selectionAsNewReferencelist.putValue(Action.NAME, Messages.getString("InputTextArea.NewReferenceList")); //$NON-NLS-1$
		selectionAsNewReferencelist.putValue(AbstractAction.LONG_DESCRIPTION,  Messages.getString("InputTextArea.NewList.desc")); //$NON-NLS-1$
        getPopupMenu().add(selectionAsNewReferencelist);

        selectionAsNewProject = new AbstractAction() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (getSelectedText()!=null && getSelectedText().length()>0) {
					engine.getGTDModel().createFolder(getSelectedText(),FolderType.PROJECT);
				}
			}
		};
		selectionAsNewProject.putValue(Action.NAME, Messages.getString("InputTextArea.NewProject")); //$NON-NLS-1$
		selectionAsNewSomedayList.putValue(AbstractAction.LONG_DESCRIPTION,  Messages.getString("InputTextArea.NewProject.desc")); //$NON-NLS-1$
        getPopupMenu().add(selectionAsNewProject);

        //System.out.println(Arrays.toString(amap.allKeys()));
	}

	private void updateUndoRedoActions() {
		undoAction.setEnabled(undoManager.canUndo());
		redoAction.setEnabled(undoManager.canRedo());
	}
	
	private JPopupMenu getPopupMenu() {
		if (popup == null) {
			popup = new JPopupMenu();
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
			        maybeShowPopup(e);
			    }
			    @Override
				public void mouseReleased(MouseEvent e) {
			        maybeShowPopup(e);
			    }
			});
		}
		return popup;
	}
	
    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
        	boolean b= getSelectedText()!=null && getSelectedText().length()>0;
        	selectionAsNewActionList.setEnabled(b);
        	selectionAsNewItem.setEnabled(b);
        	selectionAsNewProject.setEnabled(b);
        	selectionAsNewReferencelist.setEnabled(b);
        	selectionAsNewSomedayList.setEnabled(b);
        	
            popup.show(e.getComponent(),
                       e.getX(), e.getY());
        }
    }

    public void clearUndoHistory() {
		undoManager.discardAllEdits();
	}
	
	public void setEngine(GTDFreeEngine engine) {
		this.engine = engine;
	}

}
