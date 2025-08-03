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

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.model.DescriptionFilter;
import org.gtdfree.model.FilterList;
import org.gtdfree.model.Priority;
import org.gtdfree.model.PriorityFilter;
import org.gtdfree.model.ProjectFilter;


/**;
 * @author ikesan
 *
 */
public class FilterPanel extends AbstractFilterPanel {
	private static final long serialVersionUID = 1L;

	static final Color UNCLEAN= Color.YELLOW;
	
	JTextField criterionText;
	JButton clearButton;
	Color bg;
	JButton searchOptionsButton;
	JPopupMenu searchOptionsMenu;
	JCheckBoxMenuItem caseSensitiveMenuItem;
	JCheckBox filterProject;
	ProjectsCombo projects;
	JCheckBox filterPriority;
	PriorityPicker priority;
	
	public FilterPanel() {
		initialize();
	}
	
	protected void initialize() {
		setLayout(new GridBagLayout());
		//setPreferredSize(new Dimension(200,24));
		//setMinimumSize(new Dimension(200,24));
		//setMaximumSize(new Dimension(200,24));
		
		int col=0;
		
		String tt= Messages.getString("FilterPanel.Desc"); //$NON-NLS-1$
		setToolTipText(tt);
		
		JLabel jl= new JLabel(Messages.getString("FilterPanel.Filter")+" "); //$NON-NLS-1$ //$NON-NLS-2$
		jl.setToolTipText(tt);
		//jl.setMinimumSize(new Dimension(0,0));
		add(jl,new GridBagConstraints(col,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
		
		filterProject= new JCheckBox(Messages.getString("FilterPanel.Project")+" "); //$NON-NLS-1$ //$NON-NLS-2$
		int w= filterProject.getPreferredSize().height-5;
		filterProject.setMinimumSize(new Dimension(w,w));
		tt= Messages.getString("FilterPanel.Project.desc"); //$NON-NLS-1$
		filterProject.setToolTipText(tt);
		filterProject.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				projects.setEnabled(filterProject.isSelected());
				fireSearch();
			}
		});
		add(filterProject,new GridBagConstraints(++col,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
	
		projects= new ProjectsCombo();
		projects.setEditable(false);
		projects.setEnabled(false);
		projects.setToolTipText(tt);
		projects.addPropertyChangeListener(ProjectsCombo.SELECTED_PROJECT_PROPERTY_NAME,new PropertyChangeListener() {
		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (isEnabled() && filterProject.isSelected()) {
					fireSearch();
				}
			}
		});
		add(projects,new GridBagConstraints(++col,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,8),0,0));
		
		filterPriority= new JCheckBox(Messages.getString("FilterPanel.Priority")+" "); //$NON-NLS-1$ //$NON-NLS-2$
		w= filterPriority.getPreferredSize().height-5;
		filterPriority.setMinimumSize(new Dimension(w,w));
		tt= Messages.getString("FilterPanel.Priority.desc"); //$NON-NLS-1$
		filterPriority.setToolTipText(tt);
		filterPriority.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				priority.setEnabled(filterPriority.isSelected());
				fireSearch();
			}
		});
		add(filterPriority,new GridBagConstraints(++col,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
		
		priority= new PriorityPicker();
		priority.setEnabled(false);
		priority.addPropertyChangeListener(PriorityPicker.PRIORITY_PROPERTY_NAME, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (isEnabled() && filterPriority.isSelected()) {
					fireSearch();
				}
			}
		});
		add(priority,new GridBagConstraints(++col,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,8),0,0));
		
		searchOptionsMenu= new JPopupMenu();
		caseSensitiveMenuItem= new JCheckBoxMenuItem(Messages.getString("FilterPanel.CaseSensitive")); //$NON-NLS-1$
		searchOptionsMenu.add(caseSensitiveMenuItem);
		
		searchOptionsButton= new JButton();
		searchOptionsButton.setIcon(new Icon() {
			Icon find= ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_search);		
			Icon down= ApplicationHelper.getIcon(ApplicationHelper.icon_name_tiny_options);
			
			public void paintIcon(Component c, Graphics g, int x, int y) {
				if (find==null || down==null) {
					return;
				}
				find.paintIcon(c, g, x, y);
				down.paintIcon(c, g, x+find.getIconWidth()+3, y+(int)((find.getIconHeight()-down.getIconHeight())/2.0));
			}
		
			public int getIconWidth() {
				return find.getIconWidth()+3+down.getIconWidth();
			}
		
			public int getIconHeight() {
				return find.getIconHeight();
			}
		
		});
		searchOptionsButton.setDisabledIcon(new Icon() {
			Icon find= ApplicationHelper.getCachedDisabledIcon(ApplicationHelper.icon_name_small_search);		
			Icon down= ApplicationHelper.getCachedDisabledIcon(ApplicationHelper.icon_name_tiny_options);
			
			public void paintIcon(Component c, Graphics g, int x, int y) {
				if (find==null || down==null) {
					return;
				}
				find.paintIcon(c, g, x, y);
				down.paintIcon(c, g, x+find.getIconWidth()+3, y+(int)((find.getIconHeight()-down.getIconHeight())/2.0));
			}
		
			public int getIconWidth() {
				return find.getIconWidth()+3+down.getIconWidth();
			}
		
			public int getIconHeight() {
				return find.getIconHeight();
			}
		
		});
		searchOptionsButton.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				searchOptionsMenu.show(searchOptionsButton, 0, searchOptionsButton.getHeight());
			}
		
		});
		searchOptionsButton.setMargin(new Insets(0,0,0,0));
		add(searchOptionsButton,new GridBagConstraints(++col,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
		
		criterionText= new JTextField() {
			private static final long serialVersionUID = 1L;
			@Override
			public Dimension getPreferredSize() {
				if (isPreferredSizeSet()) {
					return super.getPreferredSize();
				}
				
				Dimension d=getUI().getPreferredSize(this);
				if (d.width<200) {
					return new Dimension(200,d.height);
				}
				return d;
			}
			@Override
			public Dimension getMinimumSize() {
				if (isMinimumSizeSet()) {
					return super.getMinimumSize();
				}
				Dimension d=getUI().getMinimumSize(this);
				if (d.width<75) {
					return new Dimension(75,d.height);
				}
				return d;
			}
		};
		bg= criterionText.getBackground();
		criterionText.setOpaque(true);
		criterionText.setToolTipText(Messages.getString("FilterPanel.Text")); //$NON-NLS-1$
		criterionText.getDocument().addDocumentListener(new DocumentListener() {
		
			public void removeUpdate(DocumentEvent e) {
				if (criterionText.getBackground()!=UNCLEAN) {
					criterionText.setBackground(UNCLEAN);
				}
			}
		
			public void insertUpdate(DocumentEvent e) {
				criterionText.setBackground(UNCLEAN);
				if (criterionText.getBackground()!=UNCLEAN) {
					criterionText.setBackground(UNCLEAN);
				}
			}
		
			public void changedUpdate(DocumentEvent e) {
				if (criterionText.getBackground()!=UNCLEAN) {
					criterionText.setBackground(UNCLEAN);
				}
			}
		
		});
		criterionText.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				fireSearch();
			}
		
		});
		add(criterionText,new GridBagConstraints(++col,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
		
		clearButton = new JButton();
		clearButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				criterionText.setText(ApplicationHelper.EMPTY_STRING);
				fireSearch();
			}
		});
		clearButton.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_clear));
		clearButton.setMargin(new Insets(0,0,0,0));
		add(clearButton,new GridBagConstraints(++col,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,2,0,0),0,0));
		
		//setEnabled(false);
		//validate();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		criterionText.setEditable(enabled);
		clearButton.setEnabled(enabled);
		searchOptionsButton.setEnabled(enabled);
		projects.setEnabled(enabled && filterProject.isSelected());
		priority.setEnabled(enabled && filterPriority.isSelected());
		filterPriority.setEnabled(enabled);
		filterProject.setEnabled(enabled);
	}

	@Override
	protected void clearFilters() {
		filterProject.setSelected(false);
		projects.setSelectedIndex(-1);
		filterPriority.setSelected(false);
		priority.setPriority(Priority.None);
		criterionText.setText(ApplicationHelper.EMPTY_STRING);
		criterionText.setBackground(bg);
	}
	
	@Override
	public void setEngine(GTDFreeEngine engine) {
		super.setEngine(engine);
		projects.setGTDModel(engine.getGTDModel());
	}
	
	protected void fireSearch() {
		criterionText.setBackground(bg);
	
		if (getTable()==null) {
			return;
		}

		FilterList l= new FilterList();		
		
		if (filterProject.isSelected() && projects.getSelectedIndex()!=-1) {
			l.add(new ProjectFilter(projects.getSelectedProject()!=null ? projects.getSelectedProject().getId() : null));
		}
		
		if (filterPriority.isSelected()) {
			l.add(new PriorityFilter(priority.getPriority(),false));
		}
				
		if (criterionText.getText()!=null && criterionText.getText().length()>0) {
			l.add(new DescriptionFilter(criterionText.getText(),caseSensitiveMenuItem.isSelected()));
		}
		
		if (l.size()>0) {
			fireSearch(l);
		} else {
			fireSearch(null);
		}
		
	}

}
