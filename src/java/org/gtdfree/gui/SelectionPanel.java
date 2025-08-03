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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.gtdfree.Messages;
import org.gtdfree.gui.SelectionModel.SelectionCriteria;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.Project;

/**
 * @author ikesan
 *
 */
public class SelectionPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;

	public final static int BORDER=11; 
	public final static int INDENT=BORDER+7; 
	
	public static void main(String[] args) {
		JFrame f= new JFrame();
		f.setContentPane(new SelectionPanel());
		f.pack();
		f.setVisible(true);
		
	}
	
	public static Border createCategoryBorder(final Color c) {
		Border border= new Border() {
			Insets i= new Insets(0,0,1,0);
			Color color=c;
			
			@Override
			public void paintBorder(Component c, Graphics g, int x, int y, int width,
					int height) {
				g.setColor(color);
				g.drawLine(0, height-1, width-1, height-1);		
			}
		
			@Override
			public boolean isBorderOpaque() {
				return false;
			}
		
			@Override
			public Insets getBorderInsets(Component c) {
				return i;
			}
		};
		return border;
	}
	
	private SelectionModel selectionModel= new SelectionModel();
	private ButtonGroup folderButtonGroup;
	private JButton selectFoldersButton;
	private ButtonGroup projectButtonGroup;
	private JButton selectProjectsButton;
	//private ButtonGroup actionButtonGroup;
	//private JButton selectActionsButton;
	private FolderSelectionDialog foldersSelectionDialog;
	private FolderSelectionDialog projectsSelectionDialog;
	private GTDModel gtdModel;
	private JLabel folderLabel1;
	private JLabel folderLabel2;
	private JLabel projectLabel1;
	private JLabel projectLabel2;

	private JCheckBox includeResolved;

	private JCheckBox includeDeleted;

	private JCheckBox skipEmptyFolders;

	private JCheckBox skipEmptyProjects;

	private JCheckBox skipWithoutProject;

	public SelectionPanel() {
		initialize();
	}
	
	public SelectionModel getSelectionModel() {
		return selectionModel;
	}

	private void initialize() {
		
		Border border= createCategoryBorder(getForeground());
		
		setLayout(new GridBagLayout());
		
		int row=0;
		
		JLabel l= new JLabel(Messages.getString("SelectionPanel.Folder")); //$NON-NLS-1$
		l.setFont(l.getFont().deriveFont(Font.ITALIC));
		l.setBorder(border);
		add(l, new GridBagConstraints(0,row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(BORDER,BORDER,2,BORDER),0,0));

		folderButtonGroup= new ButtonGroup();
		
		JRadioButton rb= new JRadioButton(Messages.getString("SelectionPanel.All")); //$NON-NLS-1$
		folderButtonGroup.add(rb);
		rb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setFolderCriteria(SelectionCriteria.ALL);
			}
		});
		add(rb, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));
		
		rb= new JRadioButton(Messages.getString("SelectionPanel.Custom")); //$NON-NLS-1$
		folderButtonGroup.add(rb);
		rb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setFolderCriteria(SelectionCriteria.CUSTOM);
			}
		});
		add(rb, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));
		
		selectFoldersButton= new JButton(Messages.getString("SelectionPanel.Select")); //$NON-NLS-1$
		selectFoldersButton.setEnabled(false);
		selectFoldersButton.setPreferredSize(new Dimension(selectFoldersButton.getPreferredSize().width,rb.getPreferredSize().height));
		selectFoldersButton.setMinimumSize(selectFoldersButton.getPreferredSize());
		selectFoldersButton.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				Folder[] f = getFoldersSelectionDialog().showFolderSelectionDialog();
				getSelectionModel().setCustomFolders(f);
			}
		});
		getSelectionModel().addPropertyChangeListener(SelectionModel.PROP_FOLDER_CRITERIA, new PropertyChangeListener() {
		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				selectFoldersButton.setEnabled(getSelectionModel().getFolderCriteria()==SelectionCriteria.CUSTOM);
			}
		});
		add(selectFoldersButton, new GridBagConstraints(1,row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,2,2,BORDER),0,0));
		
		skipEmptyFolders= new JCheckBox(Messages.getString("SelectionPanel.SkipEmpty")); //$NON-NLS-1$
		skipEmptyFolders.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setIncludeEmptyFolders(!skipEmptyFolders.isSelected());
			}
		});
		skipEmptyFolders.setSelected(true);
		add(skipEmptyFolders, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));

		folderLabel1= new JLabel(Messages.getString("SelectionPanel.Selected")); //$NON-NLS-1$
		folderLabel1.setFont(folderLabel1.getFont().deriveFont(9f).deriveFont(Font.ITALIC));
		add(folderLabel1, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,BORDER,0,BORDER),0,0));
		
		folderLabel2= new JLabel("..."); //$NON-NLS-1$
		folderLabel2.setFont(folderLabel1.getFont().deriveFont(9f).deriveFont(Font.ITALIC));
		add(folderLabel2, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,BORDER,4,BORDER),0,0));

		getSelectionModel().addPropertyChangeListener(SelectionModel.PROP_SELECTED_FOLDERS, new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				updateFolderSelection();
			}
		});

		
		l= new JLabel(Messages.getString("SelectionPanel.Proj")); //$NON-NLS-1$
		l.setFont(l.getFont().deriveFont(Font.ITALIC));
		l.setBorder(border);
		add(l, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(BORDER,BORDER,2,BORDER),0,0));
		
		projectButtonGroup= new ButtonGroup();
		
		rb= new JRadioButton(Messages.getString("SelectionPanel.All")); //$NON-NLS-1$
		projectButtonGroup.add(rb);
		rb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setProjectCriteria(SelectionCriteria.ALL);
			}
		});
		add(rb, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));
		
		rb= new JRadioButton(Messages.getString("SelectionPanel.Custom")); //$NON-NLS-1$
		projectButtonGroup.add(rb);
		rb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setProjectCriteria(SelectionCriteria.CUSTOM);
			}
		});
		add(rb, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));
		
		selectProjectsButton= new JButton(Messages.getString("SelectionPanel.Select")); //$NON-NLS-1$
		selectProjectsButton.setEnabled(false);
		selectProjectsButton.setPreferredSize(new Dimension(selectProjectsButton.getPreferredSize().width,rb.getPreferredSize().height));
		selectProjectsButton.setMinimumSize(selectProjectsButton.getPreferredSize());
		selectProjectsButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Folder[] f = getProjectsSelectionDialog().showFolderSelectionDialog();
				getSelectionModel().setCustomProjects(f);
			}
		});
		getSelectionModel().addPropertyChangeListener(SelectionModel.PROP_PROJECT_CRITERIA, new PropertyChangeListener() {
		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				selectProjectsButton.setEnabled(getSelectionModel().getProjectCriteria()==SelectionCriteria.CUSTOM);
			}
		});
		add(selectProjectsButton, new GridBagConstraints(1,row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,2,2,BORDER),0,0));

		skipWithoutProject= new JCheckBox(Messages.getString("SelectionPanel.SkipNoProj")); //$NON-NLS-1$
		skipWithoutProject.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setIncludeWithoutProject(!skipWithoutProject.isSelected());
			}
		});
		skipWithoutProject.setSelected(false);
		add(skipWithoutProject, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));

		skipEmptyProjects= new JCheckBox(Messages.getString("SelectionPanel.SkipEmptyProj")); //$NON-NLS-1$
		skipEmptyProjects.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setIncludeEmptyProjects(!skipEmptyProjects.isSelected());
			}
		});
		skipEmptyProjects.setSelected(true);
		add(skipEmptyProjects, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));

		projectLabel1= new JLabel(Messages.getString("SelectionPanel.Selected")); //$NON-NLS-1$
		projectLabel1.setFont(folderLabel1.getFont().deriveFont(9f).deriveFont(Font.ITALIC));
		add(projectLabel1, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,BORDER,0,BORDER),0,0));
		
		projectLabel2= new JLabel("..."); //$NON-NLS-1$
		projectLabel2.setFont(folderLabel1.getFont().deriveFont(9f).deriveFont(Font.ITALIC));
		add(projectLabel2, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,BORDER,4,BORDER),0,0));
		
		getSelectionModel().addPropertyChangeListener(SelectionModel.PROP_SELECTED_PROJECTS, new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				updateProjectSelection();
			}
		});

		
		l= new JLabel(Messages.getString("SelectionPanel.Act")); //$NON-NLS-1$
		l.setFont(l.getFont().deriveFont(Font.ITALIC));
		l.setBorder(border);
		add(l, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(BORDER,BORDER,2,BORDER),0,0));
		
		includeResolved= new JCheckBox(Messages.getString("SelectionPanel.Res")); //$NON-NLS-1$
		includeResolved.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				selectionModel.setIncludeResolved(includeResolved.isSelected());
			}
		});
		add(includeResolved, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));
		
		includeDeleted= new JCheckBox(Messages.getString("SelectionPanel.Del")); //$NON-NLS-1$
		includeDeleted.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				selectionModel.setIncludeDeleted(includeDeleted.isSelected());
			}
		});
		add(includeDeleted, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,BORDER,BORDER),0,0));

		/*actionButtonGroup= new ButtonGroup();
		
		rb= new JRadioButton("All");
		actionButtonGroup.add(rb);
		rb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setActionCriteria(SelectionCriteria.ALL);
			}
		});
		add(rb, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,2,BORDER),0,0));
		
		rb= new JRadioButton("Custom");
		actionButtonGroup.add(rb);
		rb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				getSelectionModel().setActionCriteria(SelectionCriteria.CUSTOM);
			}
		});
		add(rb, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,new Insets(0,INDENT,11,BORDER),0,0));
		
		selectActionsButton= new JButton("Select...");
		selectActionsButton.setEnabled(false);
		selectActionsButton.setPreferredSize(new Dimension(selectActionsButton.getPreferredSize().width,rb.getPreferredSize().height));
		selectActionsButton.setMinimumSize(selectActionsButton.getPreferredSize());
		getSelectionModel().addPropertyChangeListener(SelectionModel.PROP_ACTION_CRITERIA, new PropertyChangeListener() {
		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				selectActionsButton.setEnabled(getSelectionModel().getActionCriteria()==SelectionCriteria.CUSTOM);
			}
		});
		add(selectActionsButton, new GridBagConstraints(1,row,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,2,BORDER,BORDER),0,0));
		*/

		
		folderButtonGroup.setSelected(folderButtonGroup.getElements().nextElement().getModel(), true);
		projectButtonGroup.setSelected(projectButtonGroup.getElements().nextElement().getModel(), true);
		//actionButtonGroup.setSelected(actionButtonGroup.getElements().nextElement().getModel(), true);
	}

	protected FolderSelectionDialog getFoldersSelectionDialog() {
		if (foldersSelectionDialog == null) {
			foldersSelectionDialog = new FolderSelectionDialog(false);
			foldersSelectionDialog.setGtdModel(gtdModel);
			foldersSelectionDialog.setLocationRelativeTo(this);
		}
		return foldersSelectionDialog;
	}
	
	protected FolderSelectionDialog getProjectsSelectionDialog() {
		if (projectsSelectionDialog == null) {
			projectsSelectionDialog = new FolderSelectionDialog(true);
			projectsSelectionDialog.setGtdModel(gtdModel);
			projectsSelectionDialog.setLocationRelativeTo(this);
		}
		return projectsSelectionDialog;
	}

	/**
	 * @return the gtdModel
	 */
	public GTDModel getGtdModel() {
		return gtdModel;
	}

	/**
	 * @param gtdModel the gtdModel to set
	 */
	public void setGtdModel(GTDModel gtdModel) {
		this.gtdModel = gtdModel;
		selectionModel.setGtdModel(gtdModel);
		if (foldersSelectionDialog!=null) {
			foldersSelectionDialog.setGtdModel(gtdModel);
		}
		if (projectsSelectionDialog!=null) {
			projectsSelectionDialog.setGtdModel(gtdModel);
		}
		updateFolderSelection();
		updateProjectSelection();
	}

	/**
	 * 
	 */
	private void updateFolderSelection() {
		Iterator<Folder> i= getSelectionModel().selectedFolders();
		folderLabel1.setText(Messages.getString("SelectionPanel.Selected")+" "+getSelectionModel().getSelectedFoldersCount()+" "+Messages.getString("SelectionPanel.folders")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		StringBuilder sb= new StringBuilder();
		if (i.hasNext()) {
			sb.append(i.next().getName());
		}
		while (i.hasNext()) {
			sb.append(", "); //$NON-NLS-1$
			sb.append(i.next().getName());
		}
		if (sb.length()==0) {
			folderLabel2.setText(" "); //$NON-NLS-1$
			folderLabel2.setToolTipText(""); //$NON-NLS-1$
		} else {
			folderLabel2.setText(sb.toString());
			folderLabel2.setToolTipText(sb.toString());
		}
		folderLabel1.setToolTipText(folderLabel2.getToolTipText());

	}

	/**
	 * 
	 */
	private void updateProjectSelection() {
		Iterator<Project> i= getSelectionModel().selectedProjects();
		projectLabel1.setText(Messages.getString("SelectionPanel.Selected")+" "+getSelectionModel().getSelectedProjectsCount()+" "+Messages.getString("SelectionPanel.projects")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		StringBuilder sb= new StringBuilder();
		if (i.hasNext()) {
			sb.append(i.next().getName());
		}
		while (i.hasNext()) {
			sb.append(", "); //$NON-NLS-1$
			sb.append(i.next().getName());
		}
		if (sb.length()==0) {
			projectLabel2.setText(" "); //$NON-NLS-1$
			projectLabel2.setToolTipText(""); //$NON-NLS-1$
		} else {
			projectLabel2.setText(sb.toString());
			projectLabel2.setToolTipText(sb.toString());
		}
		projectLabel1.setToolTipText(projectLabel2.getToolTipText());
	}
	
	
}
