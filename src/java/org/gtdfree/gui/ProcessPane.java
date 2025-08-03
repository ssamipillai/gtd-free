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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable.PrintMode;
import javax.swing.border.TitledBorder;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.GTDModelAdapter;
import org.gtdfree.model.Action.Resolution;


/**
 * @author ikesan
 *
 */
public class ProcessPane extends JSplitPane implements WorkflowPane {

	private static final long serialVersionUID = 1L;

	private GTDFreeEngine engine;
	private ActionTable actionTable;
	private Action resolveAction;
	private Action deleteAction;
	private AbstractAction moveAction;
	private ActionPanel actionPanel;
	private FolderPanel folders;
	private ActionSpinner actionSpinner;

	private JSplitPane split;

	private JLabel leftLabel;

	private JLabel idLabel;

	private StopwatchPanel stopwatch;


	/**
	 * @return the engine
	 */
	public GTDFreeEngine getEngine() {
		return engine;
	}

	public ProcessPane() {
		//initialize();
	}

	private void initialize() {
		
		setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		JPanel jp1= new JPanel();
		jp1.setLayout(new GridBagLayout());
		
		JPanel jpp= new JPanel();
		jpp.setLayout(new GridBagLayout());
		jpp.setBorder(new TitledBorder(Messages.getString("ProcessPane.InB"))); //$NON-NLS-1$
		
		idLabel= new JLabel(); //$NON-NLS-1$
		idLabel.setText(Messages.getString("ActionPanel.ID")+" "+Messages.getString("ActionPanel.NA")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		jpp.add(idLabel, new GridBagConstraints(0,0,3,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,4,0,4),0,0));
		
		actionSpinner= new ActionSpinner();
		actionSpinner.addPropertyChangeListener("selectedAction", new PropertyChangeListener() { //$NON-NLS-1$
		
			public void propertyChange(PropertyChangeEvent evt) {
				boolean b= actionSpinner.getSelectedAction()!=null;
				getDeleteAction().setEnabled(b);
				getResolveAction().setEnabled(b);
				
				if (b) {
					String s= Messages.getString("ActionPanel.ID")+" "+actionSpinner.getSelectedAction().getId(); //$NON-NLS-1$ //$NON-NLS-2$
					idLabel.setText(s);
					stopwatch.reset();
				} else {
					idLabel.setText(Messages.getString("ActionPanel.ID")+" "+Messages.getString("ActionPanel.NA")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					stopwatch.stop();
				}

				b= b && folders.getSelectedFolder()!=null;
				getMoveAction().setEnabled(b);
				
			}
		
		});
		
		jpp.add(actionSpinner,new GridBagConstraints(0,1,3,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,4,0,4),0,0));

		leftLabel= new JLabel();
		leftLabel.setToolTipText(Messages.getString("ProcessPane.Left")); //$NON-NLS-1$
		jpp.add(leftLabel,new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));

		JButton b = new JButton(getResolveAction());
		b.setMargin(ApplicationHelper.getDefaultFatButtonMargin());
		jpp.add(b,new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));
		
		b = new JButton(getDeleteAction());
		b.setMargin(ApplicationHelper.getDefaultFatButtonMargin());
		jpp.add(b,new GridBagConstraints(1,2,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));

		b = new JButton(getMoveAction());
		b.setMargin(ApplicationHelper.getDefaultFatButtonMargin());
		jpp.add(b,new GridBagConstraints(2,2,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));

		stopwatch= new StopwatchPanel();
		jpp.add(stopwatch,new GridBagConstraints(2,2,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));
		
		
		jp1.add(jpp,new GridBagConstraints(0,0,3,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(4,4,0,4),0,0));
		jp1.setMinimumSize(new Dimension(200,100));
		jp1.setPreferredSize(new Dimension(200,100));
		
		
		split= new JSplitPane();
		split.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		split.setResizeWeight(0);
		
		JPanel jp2= new JPanel();
		jp2.setBorder(new TitledBorder(Messages.getString("ProcessPane.Lists"))); //$NON-NLS-1$
		jp2.setLayout(new GridBagLayout());
		
		folders= new FolderPanel();
		folders.setDefaultFoldersVisible(false);
		folders.addPropertyChangeListener("selectedFolder", new PropertyChangeListener() { //$NON-NLS-1$
		
			public void propertyChange(PropertyChangeEvent evt) {
				actionTable.setFolder(folders.getSelectedFolder());
				int i= folders.getLastDroppedActionIndex();
				if (i>-1 && actionTable.getRowCount()>0) {
					if (i>=actionTable.getRowCount()) i= actionTable.getRowCount()-1;
					actionTable.setRowSelectionInterval(i, i);
				}
				getMoveAction().setEnabled(actionSpinner.getSelectedAction()!=null && folders.getSelectedFolder()!=null);
			}
		
		});
		jp2.add(folders,new GridBagConstraints(0,0,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(4,4,4,4),0,0));
		
		split.setLeftComponent(jp2);
		
		
		JPanel jp3 = new JPanel();
		jp3.setLayout(new GridBagLayout());
		
		actionPanel= new ActionPanel(false);
		actionPanel.setBorder(new TitledBorder(Messages.getString("ProcessPane.Sel"))); //$NON-NLS-1$
		actionPanel.setDescriptionTextMinimumHeight(48);
		jp3.add(actionPanel,new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,4,0,0),0,0));

		actionTable= new ActionTable();
		actionTable.setMoveEnabled(true);
		actionTable.addPropertyChangeListener(ActionTable.SELECTED_ACTIONS_PROPERTY_NAME,new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				actionPanel.setActions(actionTable.getSelectedActions());
			}
		});
		JScrollPane jsp = new JScrollPane(actionTable);
		jp3.add(jsp,new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(4,4,4,4),0,0));
		
		split.setRightComponent(jp3);
		split.setDividerLocation(210);
		split.setPreferredSize(new Dimension(200,200));
		split.setMinimumSize(new Dimension(200,200));
		
		
		actionPanel.addSwingActions(actionTable.getActionMap());
		actionTable.addSwingActions(actionPanel.getActionMap());

		setLeftComponent(jp1);
		setRightComponent(split);
		setDividerLocation(140);

	}

	public void packLayout() {
		setDividerLocation((int)((JSplitPane)getRightComponent()).getLeftComponent().getMinimumSize().getHeight()+68);
	}

	private Action getMoveAction() {
		if (moveAction == null) {
			moveAction = new AbstractAction(Messages.getString("ProcessPane.Move"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_move)) { //$NON-NLS-1$
				private static final long serialVersionUID = -8908528493980828208L;

				public void actionPerformed(ActionEvent e) {
					org.gtdfree.model.Action n= actionSpinner.getSelectedAction();
					Folder f= folders.getSelectedFolder();
					if (n!=null && f!=null) {
						engine.getGTDModel().moveAction(n, f);
						/*if (noteTable.getRowCount()>0) {
							noteTable.getSelectionModel().setSelectionInterval(0, 0);
						}*/
					}
				}
			
			};
			moveAction.setEnabled(false);
		}

		return moveAction;
	}

	private Action getDeleteAction() {
		if (deleteAction == null) {
			deleteAction = new AbstractAction(Messages.getString("ProcessPane.Delete"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_delete)) { //$NON-NLS-1$
				private static final long serialVersionUID = 5483273497818329289L;

				public void actionPerformed(ActionEvent e) {
					org.gtdfree.model.Action n= actionSpinner.getSelectedAction();
					if (n!=null) {
						n.setResolution(Resolution.DELETED);
/*						if (noteTable.getRowCount()>0) {
							noteTable.getSelectionModel().setSelectionInterval(0, 0);
						}*/
					}
				}
			
			};
			deleteAction.setEnabled(false);
		}

		return deleteAction;
	}

	private Action getResolveAction() {
		if (resolveAction == null) {
			resolveAction = new AbstractAction(Messages.getString("ProcessPane.Resolve"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_resolve)){ //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					org.gtdfree.model.Action n= actionSpinner.getSelectedAction();
					if (n!=null) {
						n.setResolution(Resolution.RESOLVED);
						/*if (noteTable.getRowCount()>0) {
							noteTable.getSelectionModel().setSelectionInterval(0, 0);
						}*/
					}
				}
			};
			resolveAction.setEnabled(false);
		}

		return resolveAction;
	}

	public void setEngine(GTDFreeEngine engine) {
		this.engine=engine;
		actionSpinner.setFolder(engine.getGTDModel().getInBucketFolder());
		folders.setEngine(getEngine());
		actionPanel.setEngine(engine);
		actionTable.setEngine(engine);
		actionSpinner.setEngine(engine);
		
		engine.addPropertyChangeListener("aborting", new PropertyChangeListener() { //$NON-NLS-1$
		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				stopwatch.stop();
			}
		});
		
		engine.getGTDModel().addGTDModelListener(new GTDModelAdapter() {
		
			@Override
			public void elementRemoved(FolderEvent a) {
				checkSelection();
				if (a.getFolder().isInBucket()) {
					checkLeft();
				}
			}
		
			@Override
			public void elementAdded(FolderEvent a) {
				checkSelection();
				if (a.getFolder().isInBucket()) {
					checkLeft();
				}
			}
			
			@Override
			public void elementModified(org.gtdfree.model.ActionEvent a) {
				if (a.getAction().getParent().isInBucket()) {
					checkLeft();
				}
			}
		});
		
		checkSelection();
		checkLeft();
	}
	
	private void checkSelection() {
		/*if (actionSpinner.getSelectedAction()==null && actionSpinner.getRowCount()>0) {
			actionSpinner.getSelectionModel().setSelectionInterval(0, 0);
		}*/
		if (actionTable.getSelectedAction()==null && actionTable.getRowCount()>0) {
			actionTable.getSelectionModel().setSelectionInterval(0, 0);
		}
	}
	
	private void checkLeft() {
		leftLabel.setText(Messages.getString("ProcessPane.Items")+" "+engine.getGTDModel().getInBucketFolder().getOpenCount()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void store(GlobalProperties p) {
		p.putProperty("process.dividerLocation1",getDividerLocation()); //$NON-NLS-1$
		p.putProperty("process.dividerLocation2",split.getDividerLocation()); //$NON-NLS-1$
		p.putProperty("process.tree.openNodes",folders.getExpendedNodes()); //$NON-NLS-1$
		p.putProperty("organize.tree.foldingStates", folders.getFoldingStates()); //$NON-NLS-1$
	}

	public void restore(GlobalProperties p) {
		Integer i= p.getInteger("process.dividerLocation1"); //$NON-NLS-1$
		if (i!=null) {
			setDividerLocation(i);
		}
		i= p.getInteger("process.dividerLocation2"); //$NON-NLS-1$
		if (i!=null) {
			split.setDividerLocation(i);
		}
		int[] ii= p.getIntegerArray("process.tree.openNodes"); //$NON-NLS-1$
		if (ii!=null) {
			folders.setExpendedNodes(ii);
		}
		boolean[] bb= p.getBooleanArray("organize.tree.foldingStates"); //$NON-NLS-1$
		if (bb!=null) {
			folders.setFoldingStates(bb);
		}
	}
	
	@Override
	public ActionsCollection getActionsInView() {
		return new ActionsCollection(actionTable);
	}
	
	public void printTable() throws PrinterException {
		if (actionTable.getFolder()!=null) {
			actionTable.print(PrintMode.FIT_WIDTH, new MessageFormat("GTD-Free Data - "+actionTable.getFolder().getName()+" - "+ApplicationHelper.toISODateTimeString(new Date())), new MessageFormat("Page - {0}")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
