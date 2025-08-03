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

import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.html.TAG;
import org.gtdfree.model.Action;
import org.gtdfree.model.Action.Resolution;
import org.gtdfree.model.ActionFilter;
import org.gtdfree.model.Folder;
import org.gtdfree.model.Folder.FolderPreset;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.FolderListener;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.Priority;
import org.gtdfree.model.Project;
import org.gtdfree.model.Utils;


/**
 * @author ikesan
 *
 */
public class ActionTable extends JTable {
	
	private static final long serialVersionUID = 1L;

	public static final String JACTION_MOVE_DOWN= "actionTable.moveDown"; //$NON-NLS-1$
	public static final String JACTION_MOVE_UP= "actionTable.moveUp"; //$NON-NLS-1$
	public static final String JACTION_SELECT_NEXT = "actionTable.selectNext"; //$NON-NLS-1$
	public static final String JACTION_SELECT_PREVIOUS = "actionTable.selectPrevious"; //$NON-NLS-1$
	
	public static final String FOLDER_PROPERTY_NAME = "folder"; //$NON-NLS-1$
	public static final String SELECTED_ACTIONS_PROPERTY_NAME = "selectedActions"; //$NON-NLS-1$

	
	public static List<SortKey> EMPTY_KEYS= Collections.emptyList();

	public static enum CellAction {RESOLVE,DELETE,REOPEN};
	
	class DescriptionCellEditor extends DefaultCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;
		
		public DescriptionCellEditor() {
			super(new JTextField());
			editorComponent.addFocusListener(new FocusListener() {
			
				@Override
				public void focusLost(FocusEvent e) {
					stopCellEditing();
				}
			
				@Override
				public void focusGained(FocusEvent e) {
					//
				}
			});
			//editorComponent.setBorder(new LineBorder(UIManager.getColor("TextField.darkShadow"),1));
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			//value= model.getValueAt(convertRowIndexToModel(row), convertColumnIndexToModel(column));
			if (value!=null) {
				value=value.toString().replace('\n', '\u21B2');
			}
			return super.getTableCellEditorComponent(table, value, isSelected, row, column);
		}
		
		@Override
		public Object getCellEditorValue() {
			Object val = super.getCellEditorValue();
			if (val!=null) {
				val=val.toString().replace('\u21B2', '\n');
			}
			return val;
		}
	}
	class ActionCellEditor extends DefaultCellEditor implements TableCellEditor {
		private static final long serialVersionUID = -4717152933319457542L;

		public ActionCellEditor() {
			super(new JTextField());
			editorComponent.setFont(getFont());
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			editorComponent.setFont(getFont());
			return super.getTableCellEditorComponent(table, value, isSelected, row, column);
		}
		
		public void setMargin(Insets m) {
			((JTextField)editorComponent).setMargin(m);
		}
	}
	
	class ProjectCellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;
		ProjectsCombo combo;
		Project p;
		
		public ProjectCellEditor() {
			super();
			combo= new ProjectsCombo();
			combo.addPropertyChangeListener(ProjectsCombo.SELECTED_PROJECT_PROPERTY_NAME, new PropertyChangeListener() { //$NON-NLS-1$
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					stopCellEditing();
				}
			});
			combo.addPropertyChangeListener(ProjectsCombo.CANCELED_PROPERTY_NAME, new PropertyChangeListener() { //$NON-NLS-1$
			
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (isEditing()) {
						cancelCellEditing();
					}
				}
			});
			combo.setOpaque(false);
		}
		
		public void setGTDModel(GTDModel m) {
			combo.setGTDModel(m);
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			if (value instanceof Integer) {
				p= engine.getGTDModel().getProject(((Integer)value));
				combo.setSelectedProject(p);
			} else {
				p=null;
				combo.setSelectedProject(null);
			}
			return combo;
		}
		@Override
		public Object getCellEditorValue() {
			return combo.getSelectedProject() != null ? combo.getSelectedProject().getId() : null;
		}
		@Override
		public void cancelCellEditing() {
			combo.hidePopup();
			combo.setSelectedProject(p);
			super.cancelCellEditing();
		}
		@Override
		public boolean stopCellEditing() {
			combo.hidePopup();
			return super.stopCellEditing();
		}
		public Dimension getPreferredSize() {
			//return new Dimension(combo.getPreferredSize().width-combo.getPreferredSize().height,combo.getPreferredSize().height);
			return combo.getPreferredSize();
		}
		public int getPreferredWidth() {
			return combo.getPreferredWidth();
		}
		public void setShowClosedFolders(boolean show) {
			combo.setShowClosedFolders(show);
		}
	}
  
	class PriorityCellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;
		private Color editColor= new Color(232,242,254);
		PriorityPicker pp;
		Priority p;
		boolean editing= false;
		boolean setting= false;
		
		public PriorityCellEditor() {
			super();
			pp= new PriorityPicker() {
				private static final long serialVersionUID = 1L;

				@Override
				protected void paintComponent(Graphics g) {
					Point p= getMousePosition();
					if (p!=null) {
						callHover(p, false);
					}
					super.paintComponent(g);
				}
			};
			
			pp.addPropertyChangeListener("priority", new PropertyChangeListener() { //$NON-NLS-1$
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (setting) {
						return;
					}
					stopCellEditing();
				}
			});
			pp.addFocusListener(new FocusListener() {
			
				@Override
				public void focusLost(FocusEvent e) {
					cancelCellEditing();
				}
			
				@Override
				public void focusGained(FocusEvent e) {
				}
			
			});
			pp.setOpaque(true);
			pp.setBackground(editColor);
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			editing=true;
			setting=true;
			if (value instanceof Priority) {
				p= (Priority)value;
				pp.setPriority(p);
			} else {
				p=null;
				pp.setPriority(null);
			}
			setting=false;
			return pp;
		}
		
		@Override
		public Object getCellEditorValue() {
			return pp.getPriority();
		}
		@Override
		public void cancelCellEditing() {
			if (!editing) {
				return;
			}
			editing=false;
			setting=true;
			pp.setPriority(p);
			setting=false;
			super.cancelCellEditing();
		}
		public Dimension getPreferredSize() {
			return pp.getPreferredSize();
		}
		@Override
		public boolean stopCellEditing() {
			if (!editing) {
				return true;
			}
			editing=false;
			return super.stopCellEditing();
		}
	}
	
	class PriorityCellRenderer implements TableCellRenderer {
		private static final long serialVersionUID = 1L;
		PriorityPicker pp;
		
		public PriorityCellRenderer() {
			super();
			pp= new PriorityPicker() {
				private static final long serialVersionUID = 1L;
				@Override
				public void invalidate() {}
			    @Override
				public void validate() {}
			    @Override
				public void revalidate() {}
			    @Override
				public void repaint(long tm, int x, int y, int width, int height) {}
			    @Override
				public void repaint(Rectangle r) {}
			    @Override
				public void repaint() {}
			};
			
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (isSelected) {
				pp.setBackground(table.getSelectionBackground());
			} else {
				pp.setBackground(table.getBackground());
			}
			if (value instanceof Priority) {
				Priority p= (Priority)value;
				pp.setPriority(p);
			} else {
				pp.setPriority(null);
			}
			return pp;
		}
	}

	public class ActionTableModel extends AbstractTableModel implements TableModel {
		private static final String DOTS = "..."; //$NON-NLS-1$
		private static final long serialVersionUID = 1L;
		private List<Action> data= new ArrayList<Action>();
		private ArrayList<WeakReference<String>> descriptions= new ArrayList<WeakReference<String>>();
		private ArrayList<WeakReference<String>> tooltips= new ArrayList<WeakReference<String>>();

		/*private void sort() {
			if (comparator!=null) {
				Collections.sort(data, comparator);
			}
		}*/
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Action i= data.get(rowIndex);
			
			switch (columnIndex) {
				case 0:
					return i.getId();
				case 1: 
					return i.getDescription();
				case 2:
					return i.getResolution();
				case 3:
					return i.getRemind();
				case 4:
					return i;
				case 5:
					return i.getProject();
				case 6:
					return i.getParent();
				case 7:
					return i;
				case 8:
					return i.getResolved();
				case 9:
					return i.getPriority();
				default:
					return i;
			}
		}
		
		public Action getAction(int rowIndex) {
			return data.get(rowIndex);
		}

		@Override
		public int getColumnCount() {
			return 10;
		}

		@Override
		public int getRowCount() {
			return data.size();
		}
		
		public void remove(Action i) {
			int sel= getSelectedRow();
			int ix= data.indexOf(i);
			if (ix>-1) {
				data.remove(ix);
				descriptions.remove(ix);
				tooltips.remove(ix);
				fireTableRowsDeleted( ix, ix);
				if (sel>=getRowCount()) {
					sel=getRowCount()-1;
				}
				if (sel>-1) {
					getSelectionModel().setSelectionInterval(sel, sel);
				}
			}
		}
		
		public void reload(Folder f, ActionFilter a) {
			
			data.clear();
			
			int width=45;
			if (f!=null) {
				
				if (a!=null) {
					Iterator<Action> it= f.iterator(showAll ? FolderPreset.ALL : FolderPreset.OPEN);
					while (it.hasNext()) {
						Action note= it.next();
						if (a.isAcceptable(f, note)) {
							data.add(note);
							if (showFolderColumn) {
								int i= getFontMetrics(getFont()).stringWidth(note.getParent().getName());
								if (i>width) width=i;
							}
						}
					}
				} else {
					Iterator<Action> it= f.iterator(showAll ? FolderPreset.ALL : FolderPreset.OPEN);
					while (it.hasNext()) {
						Action note= it.next();
						data.add(note);
						if (showFolderColumn) {
							int i= getFontMetrics(getFont()).stringWidth(note.getParent().getName());
							if (i>width) width=i;
						}
					}
				}
				//sort();
			}
			if (showFolderColumn) {
				width+=6;
				if (width != getFolderColumn().getMaxWidth()) {
					getFolderColumn().setWidth(width);
					getFolderColumn().setMaxWidth(width);
					getFolderColumn().setMinWidth(Math.min(75,width));
					getFolderColumn().setPreferredWidth(width);
				}
			}

			descriptions.clear();
			tooltips.clear();
			descriptions.ensureCapacity(data.size());
			tooltips.ensureCapacity(data.size());
			for (int i = 0; i < data.size(); i++) {
				descriptions.add(null);
				tooltips.add(null);
			}
			
			fireTableDataChanged();
			enableSelectActions();
		}
		
		public String getDescription(int row) {
			if (descriptions.size()<=row) {
				return toDescription(data.get(row).getDescription());
			}
			WeakReference<String> w= descriptions.get(row);
			if (w!=null) {
				String s= w.get();
				if (s!=null) {
					return s;
				}
			}
			String s= toDescription(data.get(row).getDescription());
			descriptions.set(row,new WeakReference<String>(s));
			return s;
		}
		
		public String getTooltip(int row) {
			if (tooltips.size()<=row) {
				return toTooltip(data.get(row).getDescription());
			}
			WeakReference<String> w= tooltips.get(row);
			if (w!=null) {
				String s= w.get();
				if (s!=null) {
					return s;
				}
			}
			String s= toTooltip(data.get(row).getDescription());
			tooltips.set(row, new WeakReference<String>(s));
			return s;
		}
		
		private String toDescription(String s) {
			if (s==null) {
				return ApplicationHelper.EMPTY_STRING;
			}
			return s.replace('\n', ' ');
		}
		
		private String toTooltip(String s) {
			if (s==null) {
				return ApplicationHelper.EMPTY_STRING;
			}
			StringBuilder sb= new StringBuilder(s.length()+128);
			TAG.HTML.printTag(sb);
			TAG.BODY.printTag(sb);
			int line=0;
			for (int i = 0; i < s.length(); i++) {
				char c= s.charAt(i);
				if (c=='\n') {
					TAG.BR.printTag(sb);
					line++;
					if (line >= maxTooltipLines) {
						sb.append(DOTS);
						break;
					}
				} else {
					sb.append(c);
				}
			}
			TAG.BODY.printTagEnd(sb);
			TAG.HTML.printTagEnd(sb);
			return sb.toString();
		}

		public void fireActionChanged(int ix) {
			fireTableRowsUpdated(ix, ix);
		}
		public void fireActionChanged(Action note) {
			int i= data.indexOf(note);
			if (i>-1) {
				if (!showAll && !note.isOpen()) {
					int sel= getSelectedRow();
					data.remove(i);
					descriptions.remove(i);
					tooltips.remove(i);

					try {
						fireTableRowsDeleted(i, i);
					} catch (Exception e) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
					}
					if (sel>=getRowCount()) {
						sel=getRowCount()-1;
					}
					if (sel>-1) {
						getSelectionModel().setSelectionInterval(sel, sel);
					}
				} else {
					try {
						descriptions.set(i,null);
						tooltips.set(i,null);
						fireTableRowsUpdated(i, i);
					} catch (Exception e) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
					}
				}
			}
		}
		
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			Action i= data.get(rowIndex);
			descriptions.set(rowIndex,null);
			tooltips.set(rowIndex,null);
			switch (columnIndex) {
				case 0:
					return;
				case 1:
					i.setDescription(aValue.toString());
					break;
				case 5: 
					if (aValue instanceof Integer) {
						i.setProject(((Integer)aValue));
					} else {
						i.setProject(null);
					}
				break;
				case 9: 
					if (aValue instanceof Priority) {
						i.setPriority(((Priority)aValue));
					} else {
						i.setPriority(Priority.None);
					}
				break;
				default:
					return;
			}
			if (rowIndex<data.size()) {
				fireTableCellUpdated(rowIndex, columnIndex);
			}
		}
		
		public void updateRow(int row) {
			descriptions.set(row,null);
			tooltips.set(row,null);
			fireTableRowsUpdated(row, row);
		}
		
		public void updateAction(Action a) {
			int i= data.indexOf(a);
			if (i>=0) {
				updateRow(i);
			}
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex==1) {
				Action n= getAction(rowIndex);
				return n.isOpen();
			} else if (columnIndex==4 || columnIndex==5 || columnIndex==7 || columnIndex==9) {
				return true;
			}
			return false;
		}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return Integer.class;
				case 1:
					return String.class;
				case 2:
					return Resolution.class;
				case 3:
					return Date.class;
				case 4:
					return Action.class;
				case 5:
					return Project.class;
				case 6:
					return Folder.class;
				case 7:
					return Action.class;
				case 8:
					return Date.class;
				case 9:
					return Priority.class;
				default:
					return Action.class;
				}
		}
		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "ID"; //$NON-NLS-1$
				case 1:
					return "Action"; //$NON-NLS-1$
				case 2:
					return "Resolution"; //$NON-NLS-1$
				case 3:
					return "Remind"; //$NON-NLS-1$
				case 9:
					return Messages.getString("ActionTable.Priority"); //$NON-NLS-1$
				default:
					return ApplicationHelper.EMPTY_STRING;
			}
		}

		public void remove(int row) {
			int sel= getSelectedRow();
			data.remove(row);
			descriptions.remove(row);
			tooltips.remove(row);
			fireTableRowsDeleted( row, row);
			if (sel>=getRowCount()) {
				sel=getRowCount()-1;
			}
			if (sel>-1) {
				getSelectionModel().setSelectionInterval(sel, sel);
			}
		}

		public int indexOf(Action a) {
			return data.indexOf(a);
		}
	}
	
	class CellActionEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
		private static final long serialVersionUID = 1L;
		private JButton button;
		private Action note;
		private CellAction a;
		private Resolution resolution;
		
		public CellActionEditor() {
			button= new JButton();
			button.setMargin(new Insets(0,0,0,0));
			button.setBackground(getBackground());
			button.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					stopCellEditing();
					if (note!=null) {
						note.setResolution(resolution);
					}
				}
			
			});
		}
		
		private boolean setupAction(boolean isOpen) {
			if (isOpen) {
				if (a==cellAction) {
					return !(cellAction==CellAction.REOPEN);
				}
				if (cellAction==CellAction.DELETE) {
					a= CellAction.DELETE;
					resolution = Resolution.DELETED;
					button.setToolTipText(Messages.getString("ActionTable.Delete.desc")); //$NON-NLS-1$
					button.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_delete));
					return true;
				} if (cellAction== CellAction.RESOLVE) {
					a= CellAction.RESOLVE;
					resolution = Resolution.RESOLVED;
					button.setToolTipText(Messages.getString("ActionTable.Resolve")); //$NON-NLS-1$
					button.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_resolve));
					return true;
				}
				a= CellAction.REOPEN;
				resolution = Resolution.OPEN;
				button.setToolTipText(Messages.getString("ActionTable.Reopen.desc")); //$NON-NLS-1$
				button.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_undelete));
				return false;
			} 
			
			a= CellAction.REOPEN;
			resolution = Resolution.OPEN;
			button.setToolTipText(Messages.getString("ActionTable.Reopen.desc")); //$NON-NLS-1$
			button.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_undelete));
			return true;
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			if (value instanceof Action) {
				this.note=(Action)value;
				button.setEnabled(setupAction(note.isOpen()));
			} else {
				this.note=null;
				button.setEnabled(false);
			}
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			return null;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (value instanceof Action) {
				this.note=(Action)value;
				button.setEnabled(setupAction(note.isOpen()));
			} else {
				this.note=null;
				button.setEnabled(false);
			}
			return button;
		}
		@Override
		public boolean isCellEditable(EventObject e) {
			return true;
		}
	}
	
	class QueueCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
		private static final long serialVersionUID = 1L;
		private JToggleButton button;
		private Action note;
		
		public QueueCellEditor() {
			button= new JToggleButton();
			button.setMargin(new Insets(0,0,0,0));
			button.setToolTipText(Messages.getString("ActionTable.Queue.desc")); //$NON-NLS-1$
			button.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_queue_off));
			button.setSelectedIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_queue_on));
			button.setDisabledSelectedIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_queue_off));
			button.setBackground(getBackground());
			button.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					stopCellEditing();
					if (note!=null) {
						note.setQueued(button.isSelected());
					}
				}
			
			});
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			if (value instanceof Action) {
				this.note=(Action)value;
				button.setEnabled(note.isOpen());
				if (note.isOpen()) {
					button.setSelected(note.isQueued());
				} else {
					button.setSelected(false);
				}
			} else {
				this.note=null;
				button.setEnabled(false);
				button.setSelected(false);
			}
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			return null;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (value instanceof Action) {
				Action n=(Action)value;
				button.setEnabled(n.isOpen());
				if (n.isOpen()) {
					button.setSelected(n.isQueued());
				} else {
					button.setSelected(false);
				}
			} else {
				//this.note=null;
				button.setEnabled(false);
				button.setSelected(false);
			}
			return button;
		}
		@Override
		public boolean isCellEditable(EventObject e) {
			return true;
		}
	}
	
	class ActionRowSorter extends TableRowSorter<ActionTableModel> {

		public ActionRowSorter() {
			super(model);
			
			setSortsOnUpdates(true);
			setComparator(4, new Comparator<Action>() {
			
				@Override
				public int compare(Action o1, Action o2) {
					if (o1.isQueued() && !o2.isQueued()) {
						return -1;
					}
					if (!o1.isQueued() && o2.isQueued()) {
						return 1;
					}
					return 0;
				}
			
			});
			setComparator(5, new Comparator<Integer>() {
				@Override
				public int compare(Integer o1, Integer o2) {
					if (o1==null && o2==null) {
						return 0;
					}
					if (o1==null) {
						return -1;
					}
					if (o2==null) {
						return 1;
					}
					if (o1.equals(o2)) {
						return 0;
					}
					Project p1= engine.getGTDModel().getProject(o1);
					Project p2= engine.getGTDModel().getProject(o2);
					if (p1==null && p2==null) {
						return 0;
					}
					if (p1==null) {
						return -1;
					}
					if (p2==null) {
						return 1;
					}
					return p1.getName().compareTo(p2.getName());
				}
			});
			setComparator(6, new Comparator<Folder>() {
				@Override
				public int compare(Folder o1, Folder o2) {
					if (o1==null && o2==null) {
						return 0;
					}
					if (o1==null) {
						return -1;
					}
					if (o2==null) {
						return 1;
					}
					return o1.getName().compareTo(o2.getName());
				}
			});
			setComparator(7, new Comparator<Action>() {
				
				@Override
				public int compare(Action o1, Action o2) {
					if (o1.isOpen() && !o2.isOpen()) {
						return -1;
					}
					if (!o1.isOpen() && o2.isOpen()) {
						return 1;
					}
					return 0;
				}
			
			});
			
			addRowSorterListener(new RowSorterListener() {
			
				@Override
				public void sorterChanged(RowSorterEvent e) {
					enableMoveActions();
				}
			});
		}
		
		/*public boolean isTransformed() {
			return getSortKeys()!=null && getSortKeys().size()>0;
		}*/
		
		public boolean isUnsorted() {
			List<? extends SortKey> keys = getSortKeys();
	        int keySize = keys.size();
	        return (keySize == 0 || keys.get(0).getSortOrder() ==
	                SortOrder.UNSORTED);
		}
		
	    private void checkColumn(int column) {
	        if (column < 0 || column >= getModelWrapper().getColumnCount()) {
	            throw new IndexOutOfBoundsException(
	                    "column beyond range of TableModel"); //$NON-NLS-1$
	        }
	    }
		@Override
		public void toggleSortOrder(int column) {
	        checkColumn(column);
	        if (isSortable(column)) {
	            List<SortKey> keys = new ArrayList<SortKey>(getSortKeys());
	            SortKey sortKey;
	            int sortIndex;
	            for (sortIndex = keys.size() - 1; sortIndex >= 0; sortIndex--) {
	                if (keys.get(sortIndex).getColumn() == column) {
	                    break;
	                }
	            }
	            if (sortIndex == -1) {
	                // Key doesn't exist
	                sortKey = new SortKey(column, SortOrder.ASCENDING);
	                keys.add(0, sortKey);
	            }
	            else if (sortIndex == 0) {
	                // It's the primary sorting key, toggle it
	                keys.set(0, toggle(keys.get(0)));
	            }
	            else {
	                // It's not the first, but was sorted on, remove old
	                // entry, insert as first with ascending.
	                keys.remove(sortIndex);
	                keys.add(0, new SortKey(column, SortOrder.ASCENDING));
	            }
	            if (keys.size() > getMaxSortKeys()) {
	                keys = keys.subList(0, getMaxSortKeys());
	            }
	            setSortKeys(keys);
	        }
        }

		private SortKey toggle(SortKey key) {
	        if (key.getSortOrder() == SortOrder.ASCENDING) {
	            return new SortKey(key.getColumn(), SortOrder.DESCENDING);
	        }
	        if (key.getSortOrder() == SortOrder.DESCENDING) {
	            return new SortKey(key.getColumn(), SortOrder.UNSORTED);
	        }
	        return new SortKey(key.getColumn(), SortOrder.ASCENDING);
	    }

	}
	
	abstract class TicklingTableCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		private Color alarmBackground= new Color(0xf8ff89);
		
		public void decorateByDate(Date d, boolean isSelected, boolean hasFocus) {
			if (isSelected || hasFocus) {
				return;
			}
			if (d==null) {
				setForeground(ActionTable.this.getForeground());
				setBackground(ActionTable.this.getBackground());
				setFont(getFont().deriveFont(Font.PLAIN));
			} else {
				long today= Utils.today();
				if(d.getTime()<today) {
					setForeground(Color.RED);
					setBackground(ActionTable.this.getBackground());
					setFont(getFont().deriveFont(Font.PLAIN));
				} else {
					if (d.getTime()<today+Utils.MILLISECONDS_IN_DAY) {
						setForeground(Color.RED);
						setFont(getFont().deriveFont(Font.BOLD));
					} else {
						setForeground(ActionTable.this.getForeground());
						setFont(getFont().deriveFont(Font.PLAIN));
					}
					if (d.getTime()-today<3*Utils.MILLISECONDS_IN_DAY) {
						setBackground(alarmBackground);
					} else {
						setBackground(ActionTable.this.getBackground());
					}
				}
			}
		}
	}

	private int maxTooltipLines= 5;
	private boolean moveEnabled=false;
	protected Folder folder;
	private ActionTableModel model;
	private boolean showAll=false;
	private FolderListener folderListener = new FolderListener() {
		
		@Override
		public void elementRemoved(FolderEvent note) {
			model.remove(note.getAction());
			enableSelectActions();
		}
	
		@Override
		public void elementModified(org.gtdfree.model.ActionEvent note) {
			model.fireActionChanged(note.getAction());
			
			if (note.getProperty()==Action.PRIORITY_PROPERTY_NAME) {
				int i= getEditingRow();
				if (i>-1) {
					i= convertRowIndexToModel(i);
					if (i==model.indexOf(note.getAction())) {
						getPriorityColumn().getCellEditor().cancelCellEditing();
					}
				}
			}
			
			if (filter!=null) {
				if (!filter.isAcceptable(folder, note.getAction())) {
					model.remove(note.getAction());
					enableSelectActions();
				}
			}
		}
	
		@Override
		public void elementAdded(FolderEvent note) {
			updateIDColumnWidth();
			model.reload(folder,filter);
			enableSelectActions();
			int i= model.indexOf(note.getAction());
			getSelectionModel().setSelectionInterval(i, i);
		}
		
		@Override
		public void orderChanged(Folder f) {
			Action a= getSelectedAction();
			model.reload(folder,filter);
			int i= model.indexOf(a);
			getSelectionModel().setSelectionInterval(i, i);
		}
	
	};
	private AbstractAction moveUpAction;
	private AbstractAction moveDownAction;
	private Action[] selectedActions;
	private ActionFilter filter;
	private ProjectCellEditor projectEditor;
	private GTDFreeEngine engine;
	private TableColumn remindColumn;
	private TableColumn idColumn;
	private TableColumn descriptionColumn;
	private TableColumn actionColumn;
	private TableColumn projectColumn;
	private TableColumn folderColumn;
	private TableColumn resolvedColumn;
	private TableColumn priorityColumn;
	private TableColumn queueColumn;
	private boolean showResolvedColumn;
	private boolean showFolderColumn=false;
	private boolean showPriorityColumn=false;
	private boolean showQueueColumn=true;
	private boolean showProjectColumn;
	private boolean showRemindColumn=false;
	private boolean singleSelectionMode=false;
	private boolean showClosedFolders=false;

	private ActionRowSorter rowSorter;

	private AbstractAction goNextAction;

	private AbstractAction goPreviousAction;

	private CellAction cellAction= CellAction.RESOLVE;

	private ActionTransferHandler transferHandler;

	private JPopupMenu popupMenu;
	
	private AbstractAction saveRowOrder;
	
	
	public void setCellAction(CellAction a) {
		if (cellAction==a) {
			return;
		}
		cellAction=a;
	}

	/**
	 * @return the showRemindColumn
	 */
	public boolean isShowRemindColumn() {
		return showRemindColumn;
	}

	/**
	 * @param showRemindColumn the showRemindColumn to set
	 */
	public void setShowRemindColumn(boolean showRemindColumn) {
		this.showRemindColumn = showRemindColumn;
		rebuildColumns();
	}
	
	public void setShowResolvedColumn(boolean showResolvedColumn) {
		this.showResolvedColumn = showResolvedColumn;
		rebuildColumns();
	}
	public boolean isShowResolvedColumn() {
		return this.showResolvedColumn;
	}

	private void rebuildColumns() {
		TableColumnModel cm= getColumnModel();
		int i=0;
		if (cm.getColumnCount()==0) {
			cm.addColumn(getIDColumn());
		}
		i++;
		if (showFolderColumn) {
			while (cm.getColumnCount()>i && cm.getColumn(i)!=getFolderColumn()) {
				cm.removeColumn(cm.getColumn(i));
			}
			if (cm.getColumnCount()==i) {
				cm.addColumn(getFolderColumn());
			}
			i++;
		}
		if (showProjectColumn) {
			while (cm.getColumnCount()>i && cm.getColumn(i)!=getProjectColumn()) {
				cm.removeColumn(cm.getColumn(i));
			}
			if (cm.getColumnCount()==i) {
				cm.addColumn(getProjectColumn());
			}
			i++;
		}
		while (cm.getColumnCount()>i && cm.getColumn(i)!=getDescriptionColumn()) {
			cm.removeColumn(cm.getColumn(i));
		}
		if (cm.getColumnCount()==i) {
			cm.addColumn(getDescriptionColumn());
		}
		i++;
		if (showPriorityColumn) {
			while (cm.getColumnCount()>i && cm.getColumn(i)!=getPriorityColumn()) {
				cm.removeColumn(cm.getColumn(i));
			}
			if (cm.getColumnCount()==i) {
				cm.addColumn(getPriorityColumn());
			}
			i++;
		}			
		if (showRemindColumn) {
			while (cm.getColumnCount()>i && cm.getColumn(i)!=getRemindColumn()) {
				cm.removeColumn(cm.getColumn(i));
			}
			if (cm.getColumnCount()==i) {
				cm.addColumn(getRemindColumn());
			}
			i++;
		}			
		if (showResolvedColumn) {
			while (cm.getColumnCount()>i && cm.getColumn(i)!=getResolvedColumn()) {
				cm.removeColumn(cm.getColumn(i));
			}
			if (cm.getColumnCount()==i) {
				cm.addColumn(getResolvedColumn());
			}
			i++;
		}			
		if (showQueueColumn) {
			while (cm.getColumnCount()>i && cm.getColumn(i)!=getQueueColumn()) {
				cm.removeColumn(cm.getColumn(i));
			}
			if (cm.getColumnCount()==i) {
				cm.addColumn(getQueueColumn());
			}
			i++;
		}			
		while (cm.getColumnCount()>i && cm.getColumn(i)!=getActionColumn()) {
			cm.removeColumn(cm.getColumn(i));
		}
		if (cm.getColumnCount()==i) {
			cm.addColumn(getActionColumn());
		}
	}

	/**
	 * @return
	 */
	private TableColumn getRemindColumn() {
		if (remindColumn==null) {
			remindColumn= new TableColumn();
			remindColumn.setModelIndex(3);
			remindColumn.setMinWidth(110);
			remindColumn.setMaxWidth(110);
			remindColumn.setPreferredWidth(110);
			remindColumn.setResizable(false);
			remindColumn.setHeaderValue(Messages.getString("ActionTable.Reminder")); //$NON-NLS-1$
			remindColumn.setCellRenderer(new TicklingTableCellRenderer() {
				private static final long serialVersionUID = 1L;

				@Override
				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
					
					Date d=null;
					

					if (value instanceof Date) {
						d= (Date)value;
						value= ApplicationHelper.toISODateString(d);
						
						Component c= super.getTableCellRendererComponent(table,  value, isSelected, hasFocus,
								row, column);

						decorateByDate(d,isSelected,hasFocus);

						return c;
					}
					return super.getTableCellRendererComponent(table,  value, isSelected, hasFocus,
							row, column);
				}
			});
		}
		return remindColumn;
	}
	
	private TableColumn getResolvedColumn() {
		if (resolvedColumn==null) {
			resolvedColumn= new TableColumn();
			resolvedColumn.setModelIndex(8);
			resolvedColumn.setMinWidth(110);
			resolvedColumn.setMaxWidth(110);
			resolvedColumn.setPreferredWidth(110);
			resolvedColumn.setResizable(false);
			resolvedColumn.setHeaderValue(Messages.getString("ActionTable.Resolved")); //$NON-NLS-1$
			resolvedColumn.setCellRenderer(new DefaultTableCellRenderer() {
				private static final long serialVersionUID = 1L;

				@Override
				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
					if (value instanceof Date) {
						Date d= (Date)value;
						value= ApplicationHelper.toISODateString(d);
						//System.out.println(d.getTime()+" "+value);
					}
					return super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
							row, column);
				}
			});
		}
		return resolvedColumn;
	}

	private TableColumn getIDColumn() {
		if (idColumn == null) {
			idColumn = new TableColumn();
			idColumn.setMinWidth(45);
			idColumn.setMaxWidth(45);
			idColumn.setPreferredWidth(45);
			idColumn.setResizable(false);
			DefaultTableCellRenderer tce= new TicklingTableCellRenderer() {
				private static final long serialVersionUID = 1L;
				@Override
				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
					
					Component c= super.getTableCellRendererComponent(table, value instanceof Integer ? value.toString()+" " : value, isSelected, hasFocus, //$NON-NLS-1$
							row, column);
					
					if (row<model.getRowCount()) {
						decorateByDate(model.getAction(row).getRemind(),isSelected,hasFocus);
					}

					return c;
				}
			};
			tce.setHorizontalAlignment(SwingConstants.RIGHT);
			idColumn.setCellRenderer(tce);
			idColumn.setModelIndex(0);
			idColumn.setHeaderValue(Messages.getString("ActionTable.ID")); //$NON-NLS-1$
		}

		return idColumn;
	}
	private TableColumn getDescriptionColumn() {
		if (descriptionColumn == null) {
			descriptionColumn = new TableColumn();
			descriptionColumn.setResizable(true);
			DefaultTableCellRenderer tce= new DefaultTableCellRenderer() {
				private static final long serialVersionUID = 1L;
				@Override
				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
					
					int r= convertRowIndexToModel(row);
					setToolTipText(model.getTooltip(r));
					return super.getTableCellRendererComponent(table, model.getDescription(r), isSelected, hasFocus,
							row, column);
				}
			};
			tce.setHorizontalAlignment(SwingConstants.LEFT);
			descriptionColumn.setCellRenderer(tce);
			descriptionColumn.setCellEditor(new DescriptionCellEditor());
			descriptionColumn.setModelIndex(1);
			descriptionColumn.setHeaderValue(Messages.getString("ActionTable.Description")); //$NON-NLS-1$
		}

		return descriptionColumn;
	}
	private TableColumn getProjectColumn() {
		if (projectColumn == null) {
			projectColumn = new TableColumn();
			projectColumn.setResizable(true);
			DefaultTableCellRenderer tce= new DefaultTableCellRenderer() {
				private static final long serialVersionUID = -6660513206945588329L;
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Project p=null;
					if (engine!=null && value!=null) {
						p= engine.getGTDModel().getProject(((Integer)value));
					}
					if (p!=null) {
						value= p.getName();
					} else {
						value=ApplicationHelper.EMPTY_STRING;
					}
					setToolTipText(value.toString());
					Component c= super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
							row, column);
					c.setFont(c.getFont().deriveFont(Font.ITALIC));
					return c;
				}
			};
			tce.setHorizontalAlignment(SwingConstants.LEFT);
			projectColumn.setCellRenderer(tce);
			
			projectEditor = new ProjectCellEditor();
			projectEditor.setShowClosedFolders(showClosedFolders);
			
			projectColumn.setCellEditor(projectEditor);
			
			if (engine!=null) {
				projectEditor.setGTDModel(engine.getGTDModel());
			}
			
			projectColumn.setModelIndex(5);
			projectColumn.setHeaderValue(Messages.getString("ActionTable.Project")); //$NON-NLS-1$
			projectColumn.setResizable(false);
		}

		return projectColumn;
	}
	private TableColumn getPriorityColumn() {
		if (priorityColumn == null) {
			priorityColumn = new TableColumn();
			priorityColumn.setResizable(false);
			priorityColumn.setCellRenderer(new PriorityCellRenderer());
			PriorityCellEditor e= new PriorityCellEditor();
			priorityColumn.setCellEditor(e);
			priorityColumn.setModelIndex(9);
			priorityColumn.setHeaderValue(Messages.getString("ActionTable.Priority")); //$NON-NLS-1$
			priorityColumn.setWidth(e.getPreferredSize().width);
			priorityColumn.setMinWidth(e.getPreferredSize().width);
			priorityColumn.setMaxWidth(e.getPreferredSize().width);
		}

		return priorityColumn;
	}
	private TableColumn getFolderColumn() {
		if (folderColumn == null) {
			folderColumn = new TableColumn();
			folderColumn.setResizable(true);
			DefaultTableCellRenderer tce= new DefaultTableCellRenderer() {
				private static final long serialVersionUID = 1L;

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					if (value instanceof Folder) {
						value= ((Folder)value).getName();
					} else {
						value=ApplicationHelper.EMPTY_STRING;
					}
					setToolTipText(value.toString());
					return super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
							row, column);
				}
			};
			tce.setHorizontalAlignment(SwingConstants.LEFT);
			folderColumn.setCellRenderer(tce);
			
			folderColumn.setModelIndex(6);
			folderColumn.setHeaderValue(Messages.getString("ActionTable.Folder")); //$NON-NLS-1$
			folderColumn.setResizable(false);
			folderColumn.setWidth(150);
			folderColumn.setMaxWidth(150);
			folderColumn.setMinWidth(150);
			folderColumn.setPreferredWidth(150);
		}

		return folderColumn;
	}
	
	private TableColumn getActionColumn() {
		if (actionColumn == null) {
			actionColumn = new TableColumn();
			int i= getRowHeight();
			actionColumn.setMinWidth(i);
			actionColumn.setMaxWidth(i);
			actionColumn.setPreferredWidth(i);
			actionColumn.setModelIndex(7);
			actionColumn.setCellRenderer(new CellActionEditor());
			actionColumn.setCellEditor(new CellActionEditor());
		}

		int i= getRowHeight();
		if (i!=actionColumn.getMinWidth()) {
			actionColumn.setMinWidth(i);
			actionColumn.setMaxWidth(i);
			actionColumn.setPreferredWidth(i);
		}
		return actionColumn;
	}

	private TableColumn getQueueColumn() {
		if (queueColumn == null) {
			queueColumn = new TableColumn();
			queueColumn.setModelIndex(4);
			queueColumn.setCellRenderer(new QueueCellEditor());
			queueColumn.setCellEditor(new QueueCellEditor());
		}
		
		int i= getRowHeight();
		if (i!=queueColumn.getMaxWidth()) {
			queueColumn.setMinWidth(i);
			queueColumn.setMaxWidth(i);
			queueColumn.setPreferredWidth(i);
		}

		return queueColumn;
	}

	public ActionTable() {
		initialize();
	}

	private void initialize() {
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setShowHorizontalLines(false);
		setShowVerticalLines(false);
		setCellSelectionEnabled(false);
		setRowSelectionAllowed(true);
		setBackground(Color.WHITE);
		setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		setAutoCreateColumnsFromModel(false);
		
		Set<AWTKeyStroke> set= new HashSet<AWTKeyStroke>();
		set.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,0));
		set.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,InputEvent.CTRL_MASK));
		setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set);

		set= new HashSet<AWTKeyStroke>();
		set.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,InputEvent.SHIFT_DOWN_MASK));
		set.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,InputEvent.CTRL_MASK | InputEvent.SHIFT_DOWN_MASK));
		setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);

		setModel(model= new ActionTableModel());

		rebuildColumns();

		setDefaultEditor(String.class, new ActionCellEditor());

		rowSorter= new ActionRowSorter();
		setRowSorter(rowSorter);
		
		FontMetrics fm= getFontMetrics(getFont());
		setRowHeight(fm.getHeight()+3);
		
		setDragEnabled(true);
		setTransferHandler(transferHandler= new ActionTransferHandler() {
		
			private static final long serialVersionUID = -1850999423399406990L;

			@Override
			protected boolean importActions(Action[] a, Folder source, int[] indexes, TransferSupport support) {
				if (a!=null && a.length>0 ) {
					engine.getGTDModel().moveActions(a, folder);
					return true;
				}
				return false;
			}
		
			@Override
			protected Action[] exportActions() {
				Action[] a= getSelectedActions();
				if (a==null || a.length==0) {
					return null;
				}
				return a;
			}
			
			@Override
			protected int[] exportIndexes() {
				return getSelectedRows();
			}
			
			@Override
			protected Folder exportSourceFolder() {
				return getFolder();
			}
			
		});
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
		
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					Action[] old= selectedActions;
					int[] rows= getSelectedRows();
					List<Action> a= new ArrayList<Action>();
					for (int j = 0; j < rows.length; j++) {
						if (rows[j]>-1&&rows[j]<model.getRowCount()) {
							a.add(model.getAction(convertRowIndexToModel(rows[j])));
						}
					}
					if (a.size()>0) {
						selectedActions= a.toArray(new Action[a.size()]);
					} else {
						selectedActions=null;
					}
					enableMoveActions();
					try {
						firePropertyChange(SELECTED_ACTIONS_PROPERTY_NAME, old, selectedActions);
					} catch (Exception ex) {
						Logger.getLogger(this.getClass()).debug("Internal error.", ex); //$NON-NLS-1$
					}
				}
			}
		
		});
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				tryPopup(e);
			}
			@Override
			public void mousePressed(MouseEvent e) {
				tryPopup(e);
			}
			
			public void tryPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		
		getActionMap().put(JACTION_MOVE_DOWN, getMoveDownAction());
		getActionMap().put(JACTION_MOVE_UP, getMoveUpAction());
		getActionMap().put(JACTION_SELECT_NEXT, getSelectNextAction());
		getActionMap().put(JACTION_SELECT_PREVIOUS, getSelectPreviousAction());
	}
	
	private JPopupMenu getPopupMenu() {
		if (popupMenu == null) {
			popupMenu = new JPopupMenu();
			popupMenu.add(getActionMap().get(ActionPanel.JACTION_RESOLVE));
			popupMenu.add(getActionMap().get(ActionPanel.JACTION_QUEUE));
			popupMenu.add(getActionMap().get(ActionPanel.JACTION_DELETE));
			popupMenu.add(getActionMap().get(ActionPanel.JACTION_REOPEN));
			popupMenu.add(new JSeparator());
			popupMenu.add(getSelectNextAction());
			popupMenu.add(getSelectPreviousAction());
			popupMenu.add(getMoveUpAction());
			popupMenu.add(getMoveDownAction());
			popupMenu.add(new JSeparator());
			popupMenu.add(getSaveRowOrder());
		}

		return popupMenu;
	}

	private javax.swing.Action getSaveRowOrder() {
		if (saveRowOrder == null) {

			saveRowOrder = new AbstractAction(Messages.getString("ActionTable.SaveRowOrder")) { //$NON-NLS-1$
				
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					saveActionOrder();
				}
			};
			saveRowOrder.setEnabled(!rowSorter.isUnsorted());
			
			
			rowSorter.addRowSorterListener(new RowSorterListener() {
				
				@Override
				public void sorterChanged(RowSorterEvent e) {
					saveRowOrder.setEnabled(!rowSorter.isUnsorted());
				}
			});
			
		}

		return saveRowOrder;
	}

	public void setSelectedAction(Action a) {
		
		int i = model.indexOf(a);
		if (i>-1) {
			
			i= convertRowIndexToView(i);
			getSelectionModel().setSelectionInterval(i, i);
			
		}
		
	}

	/**
	 * @return the folder
	 */
	public Folder getFolder() {
		return folder;
	}

	/**
	 * @param folder the folder to set
	 */
	public void setFolder(Folder folder) {
		setFolder(folder, showAll);
	}
	/**
	 * @param folder the folder to set
	 */
	public void setFolder(Folder folder, boolean showAll) {
		if (this.folder!=null) {
			this.folder.removeFolderListener(folderListener);
		}
		Folder old = this.folder;
		this.folder = folder;
		filter=null;

		if (folder!=null) {

			updateIDColumnWidth();
			
			folder.addFolderListener(folderListener);
			
			setShowAll(showAll);
			setShowResolvedColumn(engine.getStateMachine().getShowResolvedColumn(folder.getType()));
			setShowRemindColumn(engine.getStateMachine().getShowRemindColumn(folder.getType()));
			setShowPriorityColumn(engine.getStateMachine().getShowPriorityColumn(folder.getType()));
			setShowProjectColumn(engine.getStateMachine().getShowProjectColumn(folder.getType()));
			setShowFolderColumn(engine.getStateMachine().getShowFolderColumn(folder.getType()));
			
			if (projectEditor!=null) {
				int i= projectEditor.getPreferredWidth();
				getProjectColumn().setWidth(i);
				getProjectColumn().setMaxWidth(i);
				getProjectColumn().setMinWidth(Math.min(75,i));
				getProjectColumn().setPreferredWidth(i);
			}
			
			enableMoveActions();
		}

		model.reload(folder,filter);
		rowSorter.setSortKeys(EMPTY_KEYS);
		firePropertyChange(FOLDER_PROPERTY_NAME, old, folder);
	}
	
	private void updateIDColumnWidth() {
		int id= folder.getParent().getLastActionID();
		Component c= getColumnModel().getColumn(0).getCellRenderer().getTableCellRendererComponent(this, id, false, false, 0, 0);
		int size= c.getFontMetrics(c.getFont()).stringWidth(id+" ")+10; //$NON-NLS-1$
		if (size!=getIDColumn().getPreferredWidth()) {
			getIDColumn().setWidth(size);
			getIDColumn().setMinWidth(size);
			getIDColumn().setMaxWidth(size);
			getIDColumn().setPreferredWidth(size);
		}
	}

	/**
	 * Return action at specified row index in terms of model.
	 * @param row a row index in terms of model
	 * @return action at specified row index in terms of model
	 */
	public Action getAction(int row) {
		return model.getAction(row);
	}

	/**
	 * Return action at specified row index in terms of view.
	 * @param row a row index in terms of view
	 * @return action at specified row index in terms of view
	 */
	public Action getActionAtView(int row) {
		return model.getAction(convertRowIndexToModel(row));
	}

	/**
	 * @return the showAll
	 */
	public boolean isShowAll() {
		return showAll;
	}

	/**
	 * @param showAll the showAll to set
	 */
	public void setShowAll(boolean showAll) {
		if (this.showAll == showAll) {
			return;
		}
		this.showAll = showAll;
		model.reload(folder,filter);
	}
	
	public Action getSelectedAction() {
		return selectedActions!=null ? selectedActions[0] : null;
	}

	public Action getFirstSelectedAction() {
		return selectedActions!=null ? selectedActions[0] : null;
	}

	public Action getLastSelectedAction() {
		return selectedActions!=null ? selectedActions[selectedActions.length-1] : null;
	}

	public Action[] getSelectedActions() {
		return selectedActions;
	}

	/**
	 * @return the moveEnabled
	 */
	public boolean isMoveEnabled() {
		return moveEnabled;
	}

	/**
	 * @param moveEnabled the moveEnabled to set
	 */
	public void setMoveEnabled(boolean moveEnabled) {
		this.moveEnabled = moveEnabled;
		if (moveEnabled) {
			getMoveDownAction();
			getMoveUpAction();
		}
		enableMoveActions();
	}
	
	private void enableMoveActions() {
		if (moveUpAction!=null) {
			moveUpAction.setEnabled(moveEnabled && getSelectedAction()!=null /*&& rowSorter.isUnsorted()*/ && engine.getStateMachine().getChangeActionOrder(getFolder().getType()) && getFolder().canMoveUp(getSelectedAction()));
		}
		if (moveDownAction!=null) {
			moveDownAction.setEnabled(moveEnabled && getSelectedAction()!=null /*&& rowSorter.isUnsorted()*/ && engine.getStateMachine().getChangeActionOrder(getFolder().getType()) && getFolder().canMoveDown(getSelectedAction()));
		}
	}
	private void enableSelectActions() {
		if (goNextAction!=null) {
			goNextAction.setEnabled(model.getRowCount()>0);
		}
		if (goPreviousAction!=null) {
			goPreviousAction.setEnabled(model.getRowCount()>0);
		}
	}
	
	private javax.swing.Action getMoveUpAction() {
		if (moveUpAction == null) {
			moveUpAction = new AbstractAction(Messages.getString("ActionTable.MoveUp")) { //$NON-NLS-1$
				private static final long serialVersionUID = 9040402331054547898L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (moveEnabled) {
						if (getSelectedAction()!=null) {
							if (!rowSorter.isUnsorted()) {
								saveActionOrder();
								getRowSorter().setSortKeys(EMPTY_KEYS);
							} else {
								getRowSorter().setSortKeys(EMPTY_KEYS);
								getFolder().moveUp(getFirstSelectedAction());
								enableMoveActions();
							}
						}
					}
				}
			};
			moveUpAction.putValue(javax.swing.Action.SHORT_DESCRIPTION, Messages.getString("ActionTable.MoveUp.desc")); //$NON-NLS-1$
			moveUpAction.putValue(javax.swing.Action.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_up));
			moveUpAction.putValue(javax.swing.Action.SMALL_ICON, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_up));
			moveUpAction.putValue(javax.swing.Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK));
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), JACTION_MOVE_UP);
			getActionMap().put(JACTION_MOVE_UP, moveUpAction);
			enableMoveActions();
		}

		return moveUpAction;
	}
	private javax.swing.Action getSelectNextAction() {
		if (goNextAction == null) {
			goNextAction = new AbstractAction(Messages.getString("ActionTable.Next")) { //$NON-NLS-1$

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					int[] i= getSelectedRows();
					int sel;
					if (i==null || i.length==0 || i[i.length-1]==-1 || i[i.length-1]+1>=getRowCount()) {
						sel=0;
					} else {
						sel= i[i.length-1]+1;
					}
					getSelectionModel().setSelectionInterval(sel, sel);
				}
			};
			goNextAction.putValue(javax.swing.Action.SHORT_DESCRIPTION, Messages.getString("ActionTable.Next.desc")); //$NON-NLS-1$
			goNextAction.putValue(javax.swing.Action.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_next));
			goNextAction.putValue(javax.swing.Action.SMALL_ICON, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_next));
			getActionMap().put(JACTION_SELECT_NEXT, goNextAction);
		}

		return goNextAction;
	}
	private javax.swing.Action getSelectPreviousAction() {
		if (goPreviousAction == null) {
			goPreviousAction = new AbstractAction(Messages.getString("ActionTable.Prev")) { //$NON-NLS-1$

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					int[] i= getSelectedRows();
					int sel;
					if (i== null || i.length==0 || i[0]==-1 || i[0]-1<0) {
						sel=getRowCount()-1;
					} else {
						sel= i[0]-1;
					}
					getSelectionModel().setSelectionInterval(sel, sel);
				}
			};
			goPreviousAction.putValue(javax.swing.Action.SHORT_DESCRIPTION, Messages.getString("ActionTable.Prev.desc")); //$NON-NLS-1$
			goPreviousAction.putValue(javax.swing.Action.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_previous));
			goPreviousAction.putValue(javax.swing.Action.SMALL_ICON, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_previous));
			getActionMap().put(JACTION_SELECT_PREVIOUS, goPreviousAction);
		}

		return goPreviousAction;
	}
	private javax.swing.Action getMoveDownAction() {
		if (moveDownAction == null) {
			moveDownAction = new AbstractAction(Messages.getString("ActionTable.MoveDown")) { //$NON-NLS-1$
				private static final long serialVersionUID = 4715532030674163250L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (moveEnabled) {
						if (getSelectedAction()!=null) {
							if (!rowSorter.isUnsorted()) {
								saveActionOrder();
								getRowSorter().setSortKeys(EMPTY_KEYS);
							} else {
								getRowSorter().setSortKeys(EMPTY_KEYS);
								Action a= getLastSelectedAction();
								getFolder().moveDown(a);
								setSelectedAction(a);
								enableMoveActions();
							}
						}
					}
				}
			};
			moveDownAction.putValue(javax.swing.Action.SHORT_DESCRIPTION, Messages.getString("ActionTable.MoveDown.desc")); //$NON-NLS-1$
			moveDownAction.putValue(javax.swing.Action.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_down));
			moveDownAction.putValue(javax.swing.Action.SMALL_ICON, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_down));
			moveDownAction.putValue(javax.swing.Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK));
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), JACTION_MOVE_DOWN);
			getActionMap().put(JACTION_MOVE_DOWN, moveDownAction);
			enableMoveActions();
		}

		return moveDownAction;
	}

	/**
	 * @return the filter
	 */
	public ActionFilter getFilter() {
		return filter;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(ActionFilter filter) {
		if (this.filter!=null && this.filter.equals(filter)) {
			return;
		}
		if (this.filter==null && filter==null) {
			return;
		}
		this.filter = filter;
		model.reload(folder,filter);
	}

	/**
	 * @return the showProjectColumn
	 */
	public boolean isShowProjectColumn() {
		return showProjectColumn;
	}

	/**
	 * @param showProjectColumn the showProjectColumn to set
	 */
	public void setShowProjectColumn(boolean showProjectColumn) {
		this.showProjectColumn = showProjectColumn;
		rebuildColumns();
	}
	
	public void setEngine(GTDFreeEngine e) {
		engine=e;
		transferHandler.setModel(e.getGTDModel());

		if (projectEditor!=null) {
			projectEditor.setGTDModel(engine.getGTDModel());
		}
	}

	/**
	 * @return the showQueueColumn
	 */
	public boolean isShowQueueColumn() {
		return showQueueColumn;
	}

	/**
	 * @param showQueueColumn the showQueueColumn to set
	 */
	public void setShowQueueColumn(boolean showQueueColumn) {
		this.showQueueColumn = showQueueColumn;
		rebuildColumns();
	}

	/**
	 * @return the showFolderColumn
	 */
	public boolean isShowFolderColumn() {
		return showFolderColumn;
	}

	/**
	 * @param showFolderColumn the showFolderColumn to set
	 */
	public void setShowFolderColumn(boolean showFolderColumn) {
		this.showFolderColumn = showFolderColumn;
		rebuildColumns();
	}
	/**
	 * @return the showPriorityColumn
	 */
	public boolean isShowPriorityColumn() {
		return showPriorityColumn;
	}

	/**
	 * @param showPriorityColumn the showPriorityColumn to set
	 */
	public void setShowPriorityColumn(boolean showPriorityColumn) {
		this.showPriorityColumn = showPriorityColumn;
		rebuildColumns();
	}

	/**
	 * @param singleSelectionMode the singleSelectionMode to set
	 */
	public void setSingleSelectionMode(boolean singleSelectionMode) {
		this.singleSelectionMode = singleSelectionMode;
		if (singleSelectionMode) {
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		} else {
			setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}
	}

	/**
	 * @return the singleSelectionMode
	 */
	public boolean isSingleSelectionMode() {
		return singleSelectionMode;
	}
	
	public boolean containsAction(Action a) {
		return model.indexOf(a)>-1;
	}

	public boolean isShowClosedFolders() {
		return showClosedFolders;
	}
	
	public void setShowClosedFolders(boolean showClosedFolders) {
		this.showClosedFolders = showClosedFolders;
		if (projectEditor!=null) {
			projectEditor.setShowClosedFolders(showClosedFolders);
		}
	}
	
	public void saveActionOrder() {
		if (rowSorter.isUnsorted()) {
			return;
		}
		
		Action[] order= new Action[model.getRowCount()];
		
		for (int i = 0; i < order.length; i++) {
			order[i]= model.getAction(convertRowIndexToModel(i));
		}
		
		folder.reorder(order);	
	}
	
	public void addSwingActions(ActionMap actions) {
		for (Object key : actions.allKeys()) {
			if (getActionMap().get(key)==null) {
				getActionMap().put(key, actions.get(key));
			}
		}
	}

}
