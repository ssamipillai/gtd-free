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
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.gtdfree.Messages;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.GTDModelAdapter;

/**
 * @author ikesan
 *
 */
public class FolderSelectionList extends JTable {

	
	private static final long serialVersionUID = 1L;

	class FolderModel extends AbstractTableModel {
		
		private static final long serialVersionUID = 1L;

		private List<Folder> actions = new ArrayList<Folder>();
		private List<Folder> defaults = new ArrayList<Folder>();
		private List<Folder> someday = new ArrayList<Folder>();
		private List<Folder> references = new ArrayList<Folder>();
		private Boolean[] selected = new Boolean[]{Boolean.FALSE};

		private boolean suspend;

		public FolderModel() {
		}
		
		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return Messages.getString("FolderSelectionList.Selected"); //$NON-NLS-1$
				case 1:
					return projectsMode ? Messages.getString("FolderSelectionList.Projects") : Messages.getString("FolderSelectionList.Lists"); //$NON-NLS-1$ //$NON-NLS-2$
				default:
					return ""; //$NON-NLS-1$
			}
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex==0) {
				return Boolean.class;
			} 
			return Object.class;
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex==0;
		}
		
		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (value instanceof Boolean && rowIndex>-1 && rowIndex<getRowCount() && columnIndex==0) {
				setValueAt((Boolean)value, new int[]{rowIndex});
			}
		}
		
		public void setValueAt(Boolean value, int[] rows) {
			Arrays.sort(rows);
			int first=rows[0];
			int last=rows[rows.length-1];
			for (int i = 0; i < rows.length; i++) {
				selected[rows[i]]= value;

				if (last<rows[i]) {
					last=rows[i];
				}
				
				List<Folder> l= getListIfHeader(rows[i]);
	
				if (l!=null) {
					int upto= rows[i]+l.size()+1;
					for (int j = rows[i]+1; j < upto; j++) {
						selected[j]= value;
					}
					while (i+1<rows.length && rows[i+1]<upto) {
						i++;
					}
					if (upto>last) {
						last=upto;
					}
				} else {
					int k= getParentListIndex(rows[i]);
					selected[k]= Boolean.FALSE;
					if (k<first) {
						first=k;
					}
				}
			}
			if (!suspend) {
				fireTableChanged(new TableModelEvent(this,first,last,0));
			}
		}
		
		public void setValueAtAll(Boolean value) {
			Arrays.fill(selected, value);
			fireTableChanged(new TableModelEvent(this,0,selected.length-1,0));
		}

		@Override
		public int getColumnCount() {
			return 2;
		}
		
		@Override
		public int getRowCount() {
			if (projectsMode) {
				return actions.size()+1;
			}
			
			int count=defaults.size()+1;;
			if (actions.size()>0) {
				count+=actions.size()+1;
			}
			if (references.size()>0) {
				count+=references.size()+1;
			}
			if (someday.size()>0) {
				count+=someday.size()+1;
			}
			return count;
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex==0) {
				return selected[rowIndex];
			}
			
			if (projectsMode) {
				if (rowIndex==0) {
					return Messages.getString("FolderSelectionList.Projects"); //$NON-NLS-1$
				}
				return actions.get(rowIndex-1);
			}
			
			int c= 0;
			
			if (actions.size()>0) {
				if (c==rowIndex) {
					return Messages.getString("FolderSelectionList.Actions"); //$NON-NLS-1$
				}
				c++;
				if (c+actions.size()>rowIndex) {
					return actions.get(rowIndex-c);
				}
				c+=actions.size();
			}

			if (c==rowIndex) {
				return Messages.getString("FolderSelectionList.Default"); //$NON-NLS-1$
			}
			c++;
			if (c+defaults.size()>rowIndex) {
				return defaults.get(rowIndex-c);
			}
			c+= defaults.size();
			
			if (someday.size()>0) {
				if (c==rowIndex) {
					return Messages.getString("FolderSelectionList.Someday"); //$NON-NLS-1$
				}
				c++;
				if (c+someday.size()>rowIndex) {
					return someday.get(rowIndex-c);
				}
				c+= someday.size();
			}
			if (references.size()>0) {
				if (c==rowIndex) {
					return Messages.getString("FolderSelectionList.References"); //$NON-NLS-1$
				}
				c++;
				if (c+references.size()>rowIndex) {
					return references.get(rowIndex-c);
				}
				c+= references.size();
			}

			return null;
		}
		
		private List<Folder> getListIfHeader(int row) {
			int c= 0;
			
			if (actions.size()>0) {
				if (c==row) {
					return actions;
				}
				c+= actions.size()+1;
			}

			if (c==row) {
				return defaults;
			}
			c+= defaults.size()+1;
			
			if (someday.size()>0) {
				if (c==row) {
					return someday;
				}
				c+= someday.size()+1;
			}
			if (references.size()>0) {
				if (c==row) {
					return references;
				}
				c+= references.size()+1;
			}
			
			return null;

		}
		
		private int getParentListIndex(int row) {
			int c= 0;
			
			if (actions.size()>0) {
				if (c+actions.size()>=row) {
					return c;
				}
				c+= actions.size()+1;
			}

			if (c+defaults.size()>=row) {
				return c;
			}
			c+=defaults.size()+1;
			
			if (someday.size()>0) {
				if (c+someday.size()>=row) {
					return c;
				}
				c++;
			}
			if (references.size()>0) {
				if (c+references.size()>=row) {
					return c;
				}
				c+=references.size()+1;
			}
			
			return 0;

		}

		public void rebuild(GTDModel m) {
			
			suspend=true;
			
			actions.clear();
			defaults.clear();
			references.clear();
			someday.clear();
			
			if (projectsMode) {
				for (Folder f : m) {
					if (f.isProject()) {
						actions.add(f);
					}
				}
			} else {
				for (Folder f : m) {
					if (f.isAction()) {
						actions.add(f);
					} else if (f.isDefault()) {
						defaults.add(f);
					} else if (f.isReference()) {
						references.add(f);
					} else if (f.isSomeday()) {
						someday.add(f);
					}
				}
			}
			
			Set<Folder> sel= new HashSet<Folder>(selected.length);
			
			for (int i = 0; i < selected.length; i++) {
				if (selected[i]) {
					Object o= getValueAt(i, 1);
					if (o instanceof Folder) {
						sel.add((Folder)o);
					}
				}
			}

			
			selected= new Boolean[getRowCount()];
			Arrays.fill(selected, Boolean.FALSE);
			
			Comparator<Folder> c= new Comparator<Folder>() {
			
				@Override
				public int compare(Folder o1, Folder o2) {
					return o1.getName().compareTo(o2.getName());
				}
			};
			
			Collections.sort(actions, c);
			
			if (!projectsMode) {
				Collections.sort(defaults, c);
				Collections.sort(someday, c);
				Collections.sort(references, c);
			}
			
			int count= getRowCount();
			for (int i = 0; i < count; i++) {
				Object o= getValueAt(i, 1);
				if (o instanceof Folder && sel.contains(o)) {
					setValueAt(Boolean.TRUE, new int[]{i});
				}
			}
			
			suspend=false;
			
			fireTableDataChanged();
		}

		public Folder[] getSelectedFolders() {
			
			List<Folder> l= new ArrayList<Folder>(selected.length);
			
			for (int i = 0; i < selected.length; i++) {
				if (selected[i]) {
					Object o= getValueAt(i, 1);
					if (o instanceof Folder) {
						l.add((Folder)o);
					}
				}
			}
			
			return l.toArray(new Folder[l.size()]);
		}
	}

	

	private FolderModel folderModel;
	private GTDModel gtdModel;
	private Action selectMarkedAction;
	private Action deselectMarkedAction;
	
	private boolean projectsMode=false;
	private AbstractAction selectAllAction;
	private AbstractAction deselectAllAction;
	private JPopupMenu popupMenu;
	protected boolean rebuild=true;
	

	
	public FolderSelectionList(boolean projects) {
		projectsMode=projects;
		initialize();
	}
	
	public boolean isProjectsMode() {
		return projectsMode;
	}

	private void initialize() {
		
		setAutoCreateColumnsFromModel(false);
		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setBackground(Color.WHITE);
		setShowGrid(false);
		
		folderModel= new FolderModel();
		setModel(folderModel);
		
		TableColumn c= new TableColumn();
		c.setModelIndex(0);
		c.setMinWidth(25);
		c.setPreferredWidth(25);
		c.setMaxWidth(25);
		c.setResizable(false);
		c.setHeaderValue(folderModel.getColumnName(0));
		getColumnModel().addColumn(c);
		
		c= new TableColumn();
		c.setModelIndex(1);
		c.setHeaderValue(folderModel.getColumnName(1));
		getColumnModel().addColumn(c);

		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
		
			@Override
			public void valueChanged(ListSelectionEvent e) {
				boolean b= getSelectedRowCount()>0;
				
				getSelectMarkedAction().setEnabled(b);
				getDeselectMarkedAction().setEnabled(b);
				
			}
		});
		
		setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				
				String s=null;
				
				if (value instanceof Folder) {
					StringBuilder sb= new StringBuilder(128);
					sb.append("   "); //$NON-NLS-1$
					Folder f= (Folder)value;
					sb.append(f.getName());
					
					sb.append(" ( "); //$NON-NLS-1$
					sb.append(f.getOpenCount());
					sb.append('/');
					sb.append(f.size());
					
					if (f.isClosed()) {
						sb.append(", "); //$NON-NLS-1$
						sb.append(Messages.getString("FolderSelectionList.Closed")); //$NON-NLS-1$
					}
					sb.append(" )"); //$NON-NLS-1$
					s= sb.toString();
					
				} else if (value!=null) {
					s= value.toString();
				}
				
				super.getTableCellRendererComponent(table, s, isSelected, hasFocus,
						row, column);
				
				if (value instanceof String) {
					setFont(getFont().deriveFont(Font.BOLD));
				} else {
					setFont(getFont().deriveFont(Font.PLAIN));
				}
				return this;
			}
		});
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isPopupTrigger() || (e.getClickCount()==1 && e.getButton()==MouseEvent.BUTTON3)) {
					getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		
	}
	
	protected JPopupMenu getPopupMenu() {
		if (popupMenu == null) {
			popupMenu = new JPopupMenu();
			popupMenu.add(getSelectMarkedAction());
			popupMenu.add(getDeselectMarkedAction());
			popupMenu.add(getSelectAllAction());
			popupMenu.add(getDeselectAllAction());
		}

		return popupMenu;
	}

	public void rebuild() {
		if (gtdModel!=null) {
			folderModel.rebuild(gtdModel);
		}
	}

	/**
	 * @param gtdModel the gtdModel to set
	 */
	public void setGtdModel(GTDModel gtdModel) {
		this.gtdModel = gtdModel;
		gtdModel.addGTDModelListener(new GTDModelAdapter() {
			@Override
			public void folderAdded(Folder folder) {
				rebuild=true;
			}
			@Override
			public void folderModified(FolderEvent folder) {
				rebuild=true;
			}
			@Override
			public void folderRemoved(Folder folder) {
				rebuild=true;
			}
		});
	}

	/**
	 * @return the gtdModel
	 */
	public GTDModel getGtdModel() {
		return gtdModel;
	}

	/**
	 * @return the selectMarkedAction
	 */
	public Action getSelectMarkedAction() {
		if (selectMarkedAction == null) {
			selectMarkedAction = new AbstractAction(Messages.getString("FolderSelectionList.SelMarked")) { //$NON-NLS-1$
			
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					int[] rows= getSelectedRows();
					folderModel.setValueAt(Boolean.TRUE, rows);
					clearSelection();
				}
			};
			selectMarkedAction.setEnabled(false);
			
		}

		return selectMarkedAction;
	}

	/**
	 * @return the selectMarkedAction
	 */
	public Action getSelectAllAction() {
		if (selectAllAction == null) {
			selectAllAction = new AbstractAction(Messages.getString("FolderSelectionList.SelAll")) { //$NON-NLS-1$
			
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					folderModel.setValueAtAll(Boolean.TRUE);
					clearSelection();
				}
			};
			
		}

		return selectAllAction;
	}

	/**
	 * @return the deselectMarkedAction
	 */
	public Action getDeselectMarkedAction() {
		if (deselectMarkedAction == null) {
			deselectMarkedAction = new AbstractAction(Messages.getString("FolderSelectionList.DesMarked")) { //$NON-NLS-1$
			
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					int[] rows= getSelectedRows();
					folderModel.setValueAt(Boolean.FALSE, rows);
					clearSelection();
				}
			};
			deselectMarkedAction.setEnabled(false);
		}

		return deselectMarkedAction;
	}
	
	/**
	 * @return the selectMarkedAction
	 */
	public Action getDeselectAllAction() {
		if (deselectAllAction == null) {
			deselectAllAction = new AbstractAction(Messages.getString("FolderSelectionList.DesAll")) { //$NON-NLS-1$
			
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					folderModel.setValueAtAll(Boolean.FALSE);
					clearSelection();
				}
			};
			
		}

		return deselectAllAction;
	}

	public Folder[] getSelectedFolders() {
		return folderModel.getSelectedFolders();
	}
	
	public void refresh() {
		if (rebuild) {
			rebuild=false;
			folderModel.rebuild(gtdModel);
		}
	}
	
}
