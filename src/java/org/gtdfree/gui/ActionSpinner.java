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

import javax.swing.AbstractSpinnerModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.model.Action;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.FolderListener;

/**
 * @author ikesan
 *
 */
public class ActionSpinner extends JPanel implements FolderListener {

	class FolderModel extends AbstractSpinnerModel {
		private int index;
		public void setValue(Object value) {
			int i= folder.indexOf((Action)value);
			if (i!=index) {
				index=i;
				fireStateChanged();
			}
		}
	
		public Object getValue() {
			if (folder==null || folder.size()==0) {
				return null;
			}
			if (index>=folder.size()) {
				index=folder.size()-1;
			}
			Action a= folder.get(index);
			if (!a.isOpen()) {
				a= (Action)getNextValue();
				if (a==null) {
					a= (Action)getPreviousValue();
				}
				if (a!=null) {
					index= folder.indexOf(a);
				}
			}
			return a;
		}
	
		public Object getPreviousValue() {
			return getPreviousValue(index);
		}

		private Object getPreviousValue(int i) {
			if (folder==null || folder.size()==0 || i>=folder.size()-1) {
				return null;
			}
			Action a= folder.get(i=i+1);
			if (!a.isOpen()) {
				a= (Action)getPreviousValue(i);
			}
			return a;
		}
	
		public Object getNextValue() {
			return getNextValue(index);
		}
		private Object getNextValue(int i) {
			if (folder==null || folder.size()<=0 || i==0) {
				return null;
			}
			Action a=folder.get(i=i-1);
			if (!a.isOpen()) {
				a= (Action)getNextValue(i);
			}
			return a;
		}
		
		@Override
		public void fireStateChanged() {
			super.fireStateChanged();
		}

		public void reset() {
			index=0;
			fireStateChanged();
		}

		public void removed(Action action) {
			if (action == selectedAction) {
				Object a= getPreviousValue();
				if (a==null) {
					a= getNextValue();
				}
				//index=lastIndex;
				fireStateChanged();
			} else {
				int i= folder.indexOf(selectedAction); {
					if (i<0) {
						reset();
					}
				}
			}
		}
	}
	
	private static final long serialVersionUID = -7868587669764300896L;
	private InputTextArea descText;
	private Action selectedAction;
	private Folder folder;
	private JSpinner spinner;
	private FolderModel model;
	private boolean setting;
	
	@SuppressWarnings("unused")
	private GTDFreeEngine engine;

	public ActionSpinner() {
		initialize();
	}

	private void initialize() {
		setLayout(new BorderLayout());

		descText= new InputTextArea();
		descText.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				if (setting || selectedAction==null) {
					return;
				}
				selectedAction.setDescription(descText.getText());
			}
			public void insertUpdate(DocumentEvent e) {
				if (setting || selectedAction==null) {
					return;
				}
				selectedAction.setDescription(descText.getText());
			}
			public void changedUpdate(DocumentEvent e) {
				if (setting || selectedAction==null) {
					return;
				}
				selectedAction.setDescription(descText.getText());
			}
		});
		JScrollPane sp= new JScrollPane(descText);

		spinner= new JSpinner();
		spinner.setEditor(sp);
		spinner.addChangeListener(new ChangeListener() {
		
			public void stateChanged(ChangeEvent e) {
				if (setting) {
					return;
				}
				Action a= selectedAction;
				selectedAction= (Action)spinner.getValue();
				updateGUI();
				firePropertyChange("selectedAction", a, selectedAction); //$NON-NLS-1$
			}
		
		});
		model= new FolderModel();
		spinner.setModel(model);
		add(spinner);

		updateGUI();
		//setEnabled(false);
	}

	/**
	 * @return the action
	 */
	public Action getSelectedAction() {
		return selectedAction;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		descText.setEnabled(enabled);
		spinner.setEnabled(enabled && folder!=null && folder.size()>0);
	}

	private void updateGUI() {
		Action a=selectedAction;
		if (a==null) {
			descText.setEnabled(false);
			descText.setEditable(false);
			descText.setText(ApplicationHelper.EMPTY_STRING);
		} else {
			setting=true;
			descText.setEnabled(true);
			descText.setEditable(true);
			String s= a.getDescription()==null ? ApplicationHelper.EMPTY_STRING : a.getDescription();
			if (!s.equals(descText.getText())) {
				descText.setText(s);
				descText.setCaretPosition(0);
			}
			setting=false;
		}
	}
	
	public static void main(String[] args) {
		try {
			
			JFrame f= new JFrame();
			ActionSpinner ap= new ActionSpinner();
			ap.setEnabled(true);
			f.setContentPane(ap);
			f.setSize(300, 300);
			f.setVisible(true);
			
		} catch (Exception e) {
			e.toString();
		}
	}

	public void elementAdded(FolderEvent note) {
		if (selectedAction==null) {
			model.reset();
		} else {
			setting=true;
			model.fireStateChanged();
			setting=false;
		}
	}

	public void elementModified(org.gtdfree.model.ActionEvent note) {
		if (model.getValue()==selectedAction) {
			updateGUI();
		} else {
			model.fireStateChanged();
		}
	}

	public void elementRemoved(FolderEvent note) {
		model.removed(note.getAction());
	}
	
	public void orderChanged(Folder f) {
		int i= folder.indexOf(selectedAction); {
			if (i<0) {
				model.reset();
			}
		}
	}

	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder f) {
		this.folder = f;
		folder.addFolderListener(this);
		model.reset();
	}
	
	public void setEngine(GTDFreeEngine engine) {
		this.engine = engine;
		descText.setEngine(engine);
	}

}
