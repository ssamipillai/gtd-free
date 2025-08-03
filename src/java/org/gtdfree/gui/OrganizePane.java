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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTable.PrintMode;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;
import org.gtdfree.gui.ActionTable.CellAction;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.Folder;
import org.gtdfree.model.Folder.FolderType;


/**
 * @author ikesan
 *
 */
public class OrganizePane extends JPanel implements WorkflowPane {

	private static final long serialVersionUID = 1L;
	private FolderPanel folders;
	private ActionTable actions;
	private ActionPanel actionPanel;
	private GTDFreeEngine engine;
	private AbstractFilterPanel filterPanel;
	private JSplitPane split;
	private JSplitPane split1;
	private FoldingPanel actionsPanel;
	private JTextArea description;
	private boolean setting=false;
	private TickleFilterPanel ticlePanel;
	private AbstractAction purgeDeletedAction;
	private JButton purgeDeletedButton;

	/**
	 * @return the engine
	 */
	public GTDFreeEngine getEngine() {
		return engine;
	}

	/**
	 * @param engine the engine to set
	 */
	public void setEngine(GTDFreeEngine engine) {
		this.engine = engine;
		folders.setEngine(engine);
		actionPanel.setEngine(engine);
		filterPanel.setEngine(engine);
		actions.setEngine(engine);
		actions.setShowAll(engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
		engine.getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_ALL_ACTIONS, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Folder f= actions.getFolder();
				if (f!=null) {
					actions.setShowAll(getEngine().getStateMachine().getShowAllActions(f.getType()) || getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
				} else {
					actions.setShowAll(OrganizePane.this.engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
				}
			}
		});
		folders.setShowClosedFolders(engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_CLOSED_FOLDERS));
		engine.getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_CLOSED_FOLDERS, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				folders.setShowClosedFolders(OrganizePane.this.engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_CLOSED_FOLDERS));
			}
		});
		folders.setShowEmptyFolders(engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_EMPTY_FOLDERS,true));
		engine.getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_EMPTY_FOLDERS, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				folders.setShowEmptyFolders(OrganizePane.this.engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_EMPTY_FOLDERS,true));
			}
		});
		try {
			engine.getGlobalProperties().connectBooleanProperty(
					GlobalProperties.SHOW_CLOSED_FOLDERS, 
					actions, 
					"showClosedFolders",  //$NON-NLS-1$
					null, 
					"setShowClosedFolders",  //$NON-NLS-1$
					false);
		} catch (Exception e) {
			Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
		}
	}

	public OrganizePane() {
		//initialize();
	}

	private void initialize() {
		
		setLayout(new BorderLayout());
		
		split= new JSplitPane();
		
		JPanel jp= new JPanel();
		jp.setBorder(new TitledBorder(Messages.getString("OrganizePane.Lists"))); //$NON-NLS-1$
		jp.setLayout(new GridBagLayout());

		folders= new FolderPanel();
		folders.addPropertyChangeListener("selectedFolder", new PropertyChangeListener() { //$NON-NLS-1$
		
			public void propertyChange(PropertyChangeEvent evt) {
				Folder f= folders.getSelectedFolder();
				if (f!=null) {
					if (f.getType()==FolderType.INBUCKET) {
						actions.setCellAction(CellAction.DELETE);
					} else {
						actions.setCellAction(CellAction.RESOLVE);
					}
					actions.setCellAction(CellAction.RESOLVE);
					actions.setFolder(f,engine.getStateMachine().getShowAllActions(f.getType()) || engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
					int i= folders.getLastDroppedActionIndex();
					if (i>-1 && actions.getRowCount()>0) {
						if (i>=actions.getRowCount()) i= actions.getRowCount()-1;
						actions.setRowSelectionInterval(i, i);
					}
					
					if (f.isProject()) {
						actionsPanel.setTitle(Messages.getString("OrganizePane.Project")+" "+f.getName()); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						actionsPanel.setTitle(Messages.getString("OrganizePane.List")+" "+f.getName()); //$NON-NLS-1$ //$NON-NLS-2$
						if (f.isTickler()) {
							actionsPanel.setFoldingState(Messages.getString("OrganizePane.Tickler"), true); //$NON-NLS-1$
							ticlePanel.selectToday();
						}
					}
					setting=true;
					description.setText(f.getDescription());
					setting=false;
					actionPanel.setReopenButtonVisible(f.getType()==FolderType.BUILDIN_DELETED || f.getType()==FolderType.BUILDIN_RESOLVED);
				} else {
					actions.setFolder(f,engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
					description.setText(""); //$NON-NLS-1$
					actionsPanel.setTitle(""); //$NON-NLS-1$
					actions.setCellAction(CellAction.RESOLVE);
					actionPanel.setReopenButtonVisible(false);
				}
				description.setEditable(f!=null && (f.isUserFolder() || f.isProject()));
				purgeDeletedButton.setVisible(f==getEngine().getGTDModel().getDeletedFolder());
			}
		
		});
		jp.add(folders,new GridBagConstraints(0,0,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
		split.setLeftComponent(jp);
		
		split1= new JSplitPane();
		split1.setResizeWeight(1.0);
		split.setRightComponent(split1);
		split.setDividerLocation(250);
		
		actions= new ActionTable();
		actions.setMoveEnabled(true);
		actions.setCellAction(CellAction.RESOLVE);
		actions.addPropertyChangeListener(ActionTable.SELECTED_ACTIONS_PROPERTY_NAME, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				actionPanel.setActions(actions.getSelectedActions());
			}
		});
		//jsppp.setBackground(actions.getCellRenderer(0,0).getTableCellRendererComponent(actions, ApplicationHelper.EMPTY_STRING, false, false, 0, 0).getBackground());
		//jsppp.setOpaque(false);
		
		jp= new JPanel();
		jp.setLayout(new GridBagLayout());
		//jp.setBorder(new TitledBorder("Selected List"));

		actionsPanel = new FoldingPanel();
		
		JPanel dp= new JPanel();
		dp.setLayout(new GridBagLayout());
		dp.add(new JLabel(Messages.getString("OrganizePane.Description.1")),new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0)); //$NON-NLS-1$
		
		description= new JTextArea();
		description.setEditable(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		description.setMargin(new Insets(2,4,2,4));
		description.getDocument().addDocumentListener(new DocumentListener() {
		
			@Override
			public void removeUpdate(DocumentEvent e) {
				update(description.getText());
			}
		
			@Override
			public void insertUpdate(DocumentEvent e) {
				update(description.getText());
			}
		
			@Override
			public void changedUpdate(DocumentEvent e) {
				update(description.getText());
			}
			
			private void update(String d) {
				if (setting || actions.getFolder()==null) {
					return;
				}
				setting=true;
				actions.getFolder().setDescription(d);
				setting=false;
			}
		});
		JScrollPane jsp= new JScrollPane();
		jsp.setViewportView(description);
		jsp.setPreferredSize(new Dimension(100,description.getFont().getSize()*3+7+8));
		jsp.setMinimumSize(jsp.getPreferredSize());
		dp.add(jsp,new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
		actionsPanel.addFold(Messages.getString("OrganizePane.Description"), dp, true, false); //$NON-NLS-1$
		
		filterPanel= new FilterPanel();
		filterPanel.setTable(actions);
		actionsPanel.addFold(Messages.getString("OrganizePane.Filter"), filterPanel, false, false); //$NON-NLS-1$

		ticlePanel= new TickleFilterPanel();
		ticlePanel.setTable(actions);
		actionsPanel.addFold(Messages.getString("OrganizePane.Tickler"), ticlePanel, false, false); //$NON-NLS-1$
		
		jp.add(actionsPanel, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,2,0,2),0,0));

		JScrollPane jsppp= new JScrollPane(actions);
		jp.add(jsppp, new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(2,2,2,2),0,0));

		JPanel jpp= new JPanel();
		jpp.setLayout(new GridBagLayout());
		
		purgeDeletedButton= new JButton();
		purgeDeletedButton.setAction(getPurgeDeletedAction());
		purgeDeletedButton.setVisible(false);
		jpp.add(purgeDeletedButton, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		jp.add(jpp, new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
		
		split1.setLeftComponent(jp);
		
		actionPanel= new ActionPanel();
		actionPanel.addSwingActions(actions.getActionMap());
		actions.addSwingActions(actionPanel.getActionMap());
		actionPanel.setBorder(new TitledBorder(Messages.getString("OrganizePane.Sel"))); //$NON-NLS-1$
		split1.setRightComponent(actionPanel);
		//jsp1.setDividerLocation(jsp1.getWidth()-500);
		
		add(split);
		
	}
	
	private Action getPurgeDeletedAction() {
		if (purgeDeletedAction == null) {
			purgeDeletedAction = new AbstractAction(Messages.getString("OrganizePane.Rem")) { //$NON-NLS-1$
			
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					getEngine().getGTDModel().purgeDeletedActions();
				}
			};
			purgeDeletedAction.putValue(Action.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_delete));
			purgeDeletedAction.putValue(Action.SHORT_DESCRIPTION, Messages.getString("OrganizePane.Rem.desc")); //$NON-NLS-1$
		}

		return purgeDeletedAction;
	}

	public static void main(String[] args) {
		try {
			
			JFrame f= new JFrame();
			OrganizePane p= new OrganizePane();
			f.setContentPane(p);
			f.pack();
			f.setVisible(true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void store(GlobalProperties p) {
		p.putProperty("organize.dividerLocation1",split.getDividerLocation()); //$NON-NLS-1$
		p.putProperty("organize.dividerLocation2",split1.getDividerLocation()); //$NON-NLS-1$
		p.putProperty("organize.tree.openNodes",folders.getExpendedNodes()); //$NON-NLS-1$
		p.putProperty("organize.tree.foldingStates", folders.getFoldingStates()); //$NON-NLS-1$
	}

	public void restore(GlobalProperties p) {
		Integer i= p.getInteger("organize.dividerLocation1"); //$NON-NLS-1$
		if (i!=null) {
			split.setDividerLocation(i);
		}
		i= p.getInteger("organize.dividerLocation2"); //$NON-NLS-1$
		if (i!=null) {
			split1.setDividerLocation(i);
		}
		int[] ii= p.getIntegerArray("organize.tree.openNodes"); //$NON-NLS-1$
		if (ii!=null) {
			folders.setExpendedNodes(ii);
		}
		boolean[] bb= p.getBooleanArray("organize.tree.foldingStates"); //$NON-NLS-1$
		if (bb!=null) {
			folders.setFoldingStates(bb);
		}
	}

	public void openTicklerForPast() {
		folders.setSelectedFolder(getEngine().getGTDModel().getRemindFolder());
		actionsPanel.setFoldingState(Messages.getString("OrganizePane.Tickler"), true); //$NON-NLS-1$
		ticlePanel.selectPast();
	}

	public void openTicklerForToday() {
		folders.setSelectedFolder(getEngine().getGTDModel().getRemindFolder());
		actionsPanel.setFoldingState(Messages.getString("OrganizePane.Tickler"), true); //$NON-NLS-1$
		ticlePanel.selectToday();
	}
	
	@Override
	public ActionsCollection getActionsInView() {
		return new ActionsCollection(actions);
	}
	
	public void printTable() throws PrinterException {
		if (actions.getFolder()!=null) {
			actions.print(PrintMode.FIT_WIDTH, new MessageFormat("GTD-Free Data - "+actions.getFolder().getName()+" - "+ApplicationHelper.toISODateTimeString(new Date())), new MessageFormat("Page - {0}")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Override
	public void initialize(GTDFreeEngine engine) {
		initialize();
		setEngine(engine);
		restore(engine.getGlobalProperties());
	}
	
	@Override
	public boolean isInitialized() {
		return engine!=null;
	}

	@Override
	public Folder getSelectedFolder() {
		return folders.getSelectedFolder();
	}

}
