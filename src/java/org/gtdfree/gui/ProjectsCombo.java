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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.Messages;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.GTDModelAdapter;
import org.gtdfree.model.Project;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public class ProjectsCombo extends JComboBox {
	
	class ProjectEditor extends JTextField implements ComboBoxEditor {
		
		private static final long serialVersionUID = 1L;
		Object item;
		
		public ProjectEditor() {
			addKeyListener(new KeyAdapter() {
			
				@Override
				public void keyTyped(KeyEvent e) {
					if (e.getKeyChar()==KeyEvent.VK_ESCAPE) {
						cancelEditing();
					}
				}
			
			});
			addFocusListener(new FocusListener() {
			
				@Override
				public void focusLost(FocusEvent e) {
					projectEditor.cancelEditing();
				}
			
				@Override
				public void focusGained(FocusEvent e) {
					// 
				}
			});
			setMargin(new Insets(0,0,0,0));
			//setBorder(null);
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxEditor#getEditorComponent()
		 */
		public Component getEditorComponent() {
			return this;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxEditor#getItem()
		 */
		public Object getItem() {
			if (item instanceof Project && getText().length()>0) {
				if (!getText().equals(((Project)item).getName())) {
					((Project)item).rename(getText());
				}
			} else if (getText().length()>0) {
				item= getText();
			}
			
			return item;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxEditor#selectAll()
		 */
		@Override
		public void selectAll() {
			select(0, getText().length()-1);
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxEditor#setItem(java.lang.Object)
		 */
		public void setItem(Object anObject) {
			item= anObject;
			if (item instanceof Project) {
				setText(((Project)item).getName());
			} else {
				setText(item!=null && item!=NONE ? item.toString() : ApplicationHelper.EMPTY_STRING);
			}
		}
		
		private void cancelEditing() {
			hidePopup();
			setItem(item);
			ProjectsCombo.this.firePropertyChange(CANCELED_PROPERTY_NAME, false, true); //$NON-NLS-1$
		}
		
	}
	
	class ProjectsComboBoxModel extends DefaultComboBoxModel {
		
		private static final long serialVersionUID = 1L;
		private boolean reloading=false;
		
		@Override
		public void setSelectedItem(Object sel) {
			if (sel instanceof String && sel!=NONE ) {
				sel= gtdModel.createFolder((String)sel, FolderType.PROJECT);
				reload();
			}
			super.setSelectedItem(sel);
		}
		
		@Override
		protected void fireContentsChanged(Object source, int index0, int index1) {
			if (reloading) return;
			super.fireContentsChanged(source, index0, index1);
		}
		@Override
		protected void fireIntervalAdded(Object source, int index0, int index1) {
			if (reloading) return;
			super.fireIntervalAdded(source, index0, index1);
		}
		@Override
		protected void fireIntervalRemoved(Object source, int index0, int index1) {
			if (reloading) return;
			super.fireIntervalRemoved(source, index0, index1);
		}
		
		private void reload() {
			reloading=true;
			
			Object selected= getSelectedItem();
			
			comboModel.removeAllElements();
			
			comboModel.addElement(NONE);
			
			if (gtdModel==null) {
				return;
			}
			
			Project[] p= gtdModel.toProjectsArray();
			
			Arrays.sort(p, new Comparator<Project>() {
			
				public int compare(Project o1, Project o2) {
					return o1.getName().compareTo(o2.getName());
				}
			
			});

			boolean contains=false;
			for (int i = 0; i < p.length; i++) {
				if (showClosedFolders || !p[i].isClosed()) {
					comboModel.addElement(p[i]);
					contains= contains || p[i].equals(selected); 
				}
			}
			
			if (contains) {
				setSelectedItem(selected);
			}
			
			reloading=false;
			
			fireIntervalAdded(this, 0, getSize());
			
			if (!contains) {
				setSelectedProject(null);
			}
		}
	}
	
	private static final long serialVersionUID = 1L;
	//private static final String NEW= "<New>";
	private static final String NONE= Messages.getString("ProjectsCombo.None"); //$NON-NLS-1$
	
	public static final String CANCELED_PROPERTY_NAME= "canceled"; //$NON-NLS-1$
	public static final String SELECTED_PROJECT_PROPERTY_NAME = "selectedProject"; //$NON-NLS-1$
	
	private GTDModel gtdModel;
	private ProjectsComboBoxModel comboModel;
	private Object last;
	private ProjectEditor projectEditor;
	private boolean showClosedFolders=false;
			
	public ProjectsCombo() {
		initialize();
	}
	
	private void initialize() {
		
		setFont(getFont().deriveFont(Font.ITALIC));
		
		comboModel= new ProjectsComboBoxModel();
		
		setModel(comboModel);
		
		setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				if (value instanceof Project) {
					value= ((Project)value).getName();
				};
				return super.getListCellRendererComponent(list, value, index,
						isSelected, cellHasFocus);
			}
		
		});
		
		setEditor(projectEditor= new ProjectEditor());
		
		setEditable(true);
		setBorder(null);
		
		addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				fireProjectChanged();
			}
		});
		
		addPopupMenuListener(new PopupMenuListener() {
		
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				//
			}
		
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				//
			}
		
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				projectEditor.cancelEditing();
			}
		});
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
		firePropertyChange("preferedSize", null, getPreferredSize()); //$NON-NLS-1$
	}
	
	
	public boolean isInitialized() {
		return comboModel.getSelectedItem()!=null;
	}

	/**
	 * @return the gtdModel
	 */
	public GTDModel getGTDModel() {
		return gtdModel;
	}

	/**
	 * @param m the gtdModel to set
	 */
	public void setGTDModel(GTDModel m) {
		this.gtdModel = m;
		comboModel.reload();
		m.addGTDModelListener(new GTDModelAdapter() {
			@Override
			public void folderRemoved(Folder folder) {
				comboModel.reload();
			}
		
			@Override
			public void folderModified(FolderEvent folder) {
				comboModel.reload();
			}
		
			@Override
			public void folderAdded(Folder folder) {
				comboModel.reload();
			}
		
		});
	}

	public Project getSelectedProject() {
		
		Object o= comboModel.getSelectedItem();
		
		if (o instanceof Project) {
			return (Project)o;
		}
		
		return null;
		
	}
	
	public void fireProjectChanged() {
		if (last == comboModel.getSelectedItem()) {
			return;
		}
		Project old= last instanceof Project ? (Project)last : null;
		last= comboModel.getSelectedItem();
		
		firePropertyChange(SELECTED_PROJECT_PROPERTY_NAME, old, getSelectedProject());
	}

	/**
	 * @param selectedProject the selectedProject to set
	 */
	public void setSelectedProject(Project selectedProject) {
		if (selectedProject == getSelectedProject()) {
			return;
		}
		
		if (selectedProject==null) {
			comboModel.setSelectedItem(NONE);
		} else {
			comboModel.setSelectedItem(selectedProject);
		}
		
		fireProjectChanged();
	}
	
	public int getPreferredWidth() {
		Dimension d= getUI().getPreferredSize(this);
		return d.width-d.height+getInsets().bottom+getInsets().top+4;
	}
	
	@Override
	public Dimension getPreferredSize() {
		if (isPreferredSizeSet()) {
			return super.getPreferredSize();
		}

		return new Dimension(getPreferredWidth(),ApplicationHelper.getDefaultFieldHeigth());
		
	}
	
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}
	
	@Override
	public void setOpaque(boolean isOpaque) {
		super.setOpaque(isOpaque);
		Component[] c= getComponents();
		if (c!=null) {
			for (int i = 0; i < c.length; i++) {
				if (c[i] instanceof JComponent) {
					((JComponent)c[i]).setOpaque(isOpaque);
				}
			}
		}
	}
	
	public boolean isShowClosedFolders() {
		return showClosedFolders;
	}
	
	public void setShowClosedFolders(boolean showClosedFolders) {
		this.showClosedFolders = showClosedFolders;
		comboModel.reload();
	}

}
