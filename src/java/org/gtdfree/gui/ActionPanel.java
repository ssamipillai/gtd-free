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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Calendar;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;
import org.gtdfree.model.Action;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.FolderListener;
import org.gtdfree.model.Priority;
import org.gtdfree.model.Action.Resolution;

import de.wannawork.jcalendar.JCalendarComboBox;

/**
 * @author ikesan
 *
 */
public class ActionPanel extends JPanel implements FolderListener {

	private static final long serialVersionUID = -7868587669764300896L;
	
	public static final String JACTION_DELETE = "actionPanel.delete"; //$NON-NLS-1$
	public static final String JACTION_QUEUE = "actionPanel.queue"; //$NON-NLS-1$
	public static final String JACTION_RESOLVE = "actionPanel.resolve"; //$NON-NLS-1$
	public static final String JACTION_REOPEN = "actionPanel.reopen"; //$NON-NLS-1$
	
	private JLabel idLabel;
	private JLabel createdLabel;
	private InputTextArea descText;
	private JTextField urlText;
	private Action[] actions;
	private JButton moveDownButton;
	private JButton moveUpButton;
	private boolean setting=false;
	private AbstractAction deleteAction;
	private AbstractAction resolveAction;
	//private DatePicker datePicker;
	private JCalendarComboBox datePicker;
	private ProjectsCombo projectCombo;
	private GTDFreeEngine engine;
	private AbstractAction reopenAction;
	private JLabel folderLabel;
	private AbstractAction queuedAction;
	private JToggleButton queueActionButton;
	private AbstractAction openURLAction;
	private JScrollPane descTextScroll;
	private PriorityPicker priorityPanel;
	private JButton selectNextButton;
	private JButton selectPreviousButton;
	private JButton reopenButton;
	private JPanel buttonsPanel;
	//private JButton previousDayButton;
	//private JButton nextDayButton;
	private JButton clearDateButton;
	private boolean reopenButtonVisible=false;

	private AbstractAction queuedButtonAction;

	public ActionPanel() {
		initialize(true);
	}

	public ActionPanel(boolean packHor) {
		initialize(packHor);
	}

	private void initialize(boolean packHorizontal) {
		
		setLayout(new GridBagLayout());

		int marginL=3;
		int row=0;
		
		JPanel p=null;
		
		/*
		 * ID
		 */
		JLabel jl= new JLabel(Messages.getString("ActionPanel.ID")); //$NON-NLS-1$
		if (packHorizontal) {
			add(jl, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0));
		} else {
			p= new JPanel();
			p.setLayout(new GridBagLayout());
			p.add(jl, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0));
		}

		idLabel= new JLabel(Messages.getString("ActionPanel.NA")); //$NON-NLS-1$
		Dimension d= new Dimension(100,21);
		idLabel.setPreferredSize(d);
		idLabel.setMinimumSize(d);
		if (packHorizontal) {
			add(idLabel, new GridBagConstraints(1,row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		} else {
			p.add(idLabel, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		}

		/*
		 * Created
		 */
		jl= new JLabel(Messages.getString("ActionPanel.Created")); //$NON-NLS-1$
		if (packHorizontal) {
			add(jl, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0));
		} else {
			p.add(jl, new GridBagConstraints(2,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0));
		}

		createdLabel= new JLabel(Messages.getString("ActionPanel.NA")); //$NON-NLS-1$
		createdLabel.setPreferredSize(d);
		createdLabel.setMinimumSize(d);
		if (packHorizontal) {
			add(createdLabel, new GridBagConstraints(1,row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		} else {
			p.add(createdLabel, new GridBagConstraints(3,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		}
		
		/*
		 * Folder
		 */
		jl= new JLabel(Messages.getString("ActionPanel.Folder")); //$NON-NLS-1$
		if (packHorizontal) {
			add(jl, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0));
		} else {
			p.add(jl, new GridBagConstraints(4,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0));
		}

		folderLabel= new JLabel(Messages.getString("ActionPanel.NA")); //$NON-NLS-1$
		folderLabel.setPreferredSize(d);
		folderLabel.setMinimumSize(d);
		if (packHorizontal) {
			add(folderLabel, new GridBagConstraints(1,row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		} else {
			p.add(folderLabel, new GridBagConstraints(5,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
			add(p, new GridBagConstraints(0,row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
		}

		/*
		 * Description
		 */
		descText= new InputTextArea();
		descText.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				if (setting || actions==null) {
					return;
				}
				actions[0].setDescription(descText.getText());
			}
			public void insertUpdate(DocumentEvent e) {
				if (setting || actions==null) {
					return;
				}
				actions[0].setDescription(descText.getText());
			}
			public void changedUpdate(DocumentEvent e) {
				if (setting || actions==null) {
					return;
				}
				actions[0].setDescription(descText.getText());
			}
		});
		descTextScroll= new JScrollPane(descText);
		if (packHorizontal) {
			add(descTextScroll, new GridBagConstraints(0,++row,3,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,1,1,1),0,17));
		} else {
			add(descTextScroll, new GridBagConstraints(0,++row,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,1,1,1),0,17));
		}
		
		/*
		 * URL
		 */
		jl=new JLabel(Messages.getString("ActionPanel.URL")); //$NON-NLS-1$
		if (packHorizontal) {
			add(jl, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0)); //$NON-NLS-1$
		}
		urlText= new JTextField();
		urlText.getDocument().addDocumentListener(new DocumentListener() {
			private void update() {
				URL url=null;
				
				try {
					String s= urlText.getText().trim();
					StringBuilder sb= new StringBuilder(s.length());
					for (int i = 0; i < s.length(); i++) {
						char c= s.charAt(i);
						if (!Character.isISOControl(c)) {
							sb.append(c);
						}
					}
					url= new URL(sb.toString());
				} catch (Exception e) {
					//
				}
				if (url!=null) {
					urlText.setForeground(Color.black);
					if (actions!=null) {
						setting=true;
						actions[0].setUrl(url);
						setting=false;
					}
				} else {
					urlText.setForeground(Color.red);
				}
			}
			public void removeUpdate(DocumentEvent e) {
				update();
			}
		
			public void insertUpdate(DocumentEvent e) {
				update();
			}
		
			public void changedUpdate(DocumentEvent e) {
				update();
			}
		});
		
		Dimension defFiled= new Dimension(ApplicationHelper.getDefaultFieldHeigth(),ApplicationHelper.getDefaultFieldHeigth());

		urlText.setMaximumSize(defFiled);
		urlText.setMinimumSize(defFiled);
		urlText.setPreferredSize(defFiled);

		if (packHorizontal) {
			add(urlText, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		}
		
		
		JButton b= new JButton();
		b.setMargin(ApplicationHelper.getDefaultSlimButtonMargin());
		b.setAction(getOpenURLAction());
		b.setPreferredSize(defFiled);
		b.setMaximumSize(defFiled);
		b.setMinimumSize(defFiled);
		if (packHorizontal) {
			add(b, new GridBagConstraints(2,row,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		} else {
			p= new JPanel();
			p.setLayout(new GridBagLayout());
			p.add(jl, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,1),0,0)); //$NON-NLS-1$
			p.add(urlText, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,1,0,1),0,0));
			p.add(b, new GridBagConstraints(2,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,1,0,0),0,0));
			add(p, new GridBagConstraints(0,++row,2,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0));
		}


		/*
		 * Project
		 */
		jl= new JLabel(Messages.getString("ActionPanel.Project")); //$NON-NLS-1$
		if (packHorizontal) {
			add(jl, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0)); //$NON-NLS-1$
		} else {
			p= new JPanel();
			p.setLayout(new GridBagLayout());
			p.add(jl, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(1,marginL,1,1),0,0)); //$NON-NLS-1$
		}
		
		projectCombo= new ProjectsCombo();
		projectCombo.addPropertyChangeListener("selectedProject", new PropertyChangeListener() { //$NON-NLS-1$
		
			public void propertyChange(PropertyChangeEvent evt) {
				if (setting || actions==null) {
					return;
				}
				setting= true;
				if (projectCombo.getSelectedProject()!=null) {
					for (Action a : actions) {
						a.setProject(projectCombo.getSelectedProject().getId());
					}
				} else {
					for (Action a : actions) {
						a.setProject(null);
					}
				}
				setting=false;
			}
		
		});
		if (packHorizontal) {
			add(projectCombo, new GridBagConstraints(1,row,2,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		} else {
			p.add(projectCombo, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		}
		
		/*
		 * Reminder
		 */
		if (packHorizontal) {
			add(new JLabel(Messages.getString("ActionPanel.Reminder")), new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,marginL,1,1),0,0)); //$NON-NLS-1$
		} else {
			p.add(new JLabel(Messages.getString("ActionPanel.Reminder")), new GridBagConstraints(2,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,marginL,1,1),0,0)); //$NON-NLS-1$
		}
		
		JPanel pp= new JPanel();
		pp.setLayout(new GridBagLayout());
		
		/*datePicker= new JCalendarComboBox() {
			private static final long serialVersionUID = 1L;
			Dimension d= null;
			@Override
			public Dimension getPreferredSize() {
				if (d==null) {
					Insets in= ApplicationHelper.getDefaultSlimButtonMargin();
					d= new Dimension(super.getPreferredSize().width+6,super.getPreferredSize().height+in.top+in.bottom-4);
				}
				int w= super.getPreferredSize().width+6;
				if (w>d.width) {
					d= new Dimension(w,d.height);
				}
				return d;
			}
			@Override
			public Dimension getMinimumSize() {
				return getPreferredSize();
			}
		};*/
		datePicker= new JCalendarComboBox();
		datePicker.setDateFormat(ApplicationHelper.defaultDateFormat);
		datePicker.setDate(null);
		d= new Dimension(datePicker.getPreferredSize().width+(int)(1.5*ApplicationHelper.getDefaultFieldHeigth()),ApplicationHelper.getDefaultFieldHeigth());
		datePicker.setPreferredSize(d);
		datePicker.setMinimumSize(d);
		datePicker.setSpiningCalendarField(Calendar.DAY_OF_MONTH);
		datePicker.addChangeListener(new ChangeListener() {
		
			@Override
			public void stateChanged(ChangeEvent e) {
				if (setting) {
					return;
				}
				if (actions!=null) {
					for (Action a : actions) {
						a.setRemind(datePicker.getDate());
					}
				}
			}
		
		});
		pp.add(datePicker, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,2),0,0));
		
		

		clearDateButton= new JButton();
		clearDateButton.setMargin(ApplicationHelper.getDefaultSlimButtonMargin());
		clearDateButton.setToolTipText(Messages.getString("ActionPanel.Clear")); //$NON-NLS-1$
		clearDateButton.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_clear));
		clearDateButton.setPreferredSize(defFiled);
		clearDateButton.setMaximumSize(defFiled);
		clearDateButton.setMinimumSize(defFiled);
		clearDateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				datePicker.setDate(null);
			}
		});
		pp.add(clearDateButton, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));

		if (packHorizontal) {
			add(pp, new GridBagConstraints(1,row,2,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		} else {
			p.add(pp, new GridBagConstraints(3,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		}

		/*
		 * Priority
		 */
		jl= new JLabel(Messages.getString("ActionPanel.Priority")); //$NON-NLS-1$
		if (packHorizontal) {
			add(jl, new GridBagConstraints(0,++row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(1,marginL,1,1),0,0));
		} else {
			p.add(jl, new GridBagConstraints(4,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(1,marginL,1,1),0,0));
		}
		
		priorityPanel= new PriorityPicker();
		priorityPanel.addPropertyChangeListener("priority",new PropertyChangeListener() { //$NON-NLS-1$
		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (setting || actions==null) {
					return;
				}
				setting= true;
				for (Action a : actions) {
					a.setPriority(priorityPanel.getPriority());
				}
				setting=false;
			}
		});
		if (packHorizontal) {
			add(priorityPanel, new GridBagConstraints(1,row,2,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(4,1,4,1),0,0));
		} else {
			p.add(priorityPanel, new GridBagConstraints(5,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(4,1,4,1),0,0));
			add(p, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
		}

		/*
		 * Navigate and move buttons
		 */
		
		JPanel jp= new JPanel();
		jp.setLayout(new GridBagLayout());
		
		int buttonIndex=0;
		
		jp.add(new JLabel(Messages.getString("ActionPanel.Go")), new GridBagConstraints(buttonIndex++,0,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(1,marginL,1,4),0,0)); //$NON-NLS-1$

		selectPreviousButton= new JButton();
		selectPreviousButton.setHideActionText(true);
		selectPreviousButton.setEnabled(false);
		selectPreviousButton.setMargin(ApplicationHelper.getDefaultSlimButtonMargin());
		jp.add(selectPreviousButton, new GridBagConstraints(buttonIndex++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));

		selectNextButton= new JButton();
		selectNextButton.setHideActionText(true);
		selectNextButton.setEnabled(false);
		selectNextButton.setMargin(ApplicationHelper.getDefaultSlimButtonMargin());
		jp.add(selectNextButton, new GridBagConstraints(buttonIndex++,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));

		
		
		jp.add(new JLabel(Messages.getString("ActionPanel.Move")), new GridBagConstraints(buttonIndex++,0,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(1,marginL,1,4),0,0)); //$NON-NLS-1$

		moveDownButton= new JButton();
		moveDownButton.setHideActionText(true);
		moveDownButton.setEnabled(false);
		moveDownButton.setMargin(ApplicationHelper.getDefaultSlimButtonMargin());
		jp.add(moveDownButton, new GridBagConstraints(buttonIndex++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));

		moveUpButton= new JButton();
		moveUpButton.setHideActionText(true);
		moveUpButton.setEnabled(false);
		moveUpButton.setMargin(ApplicationHelper.getDefaultSlimButtonMargin());
		jp.add(moveUpButton, new GridBagConstraints(buttonIndex++,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));

		if (packHorizontal) {
			add(jp, new GridBagConstraints(0,++row,3,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,1,1,1),0,0));
		} else {
			add(jp, new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,1,1,1),0,0));
		}

		
		
		/*
		 * buttons at bottom
		 */
		buttonsPanel= new JPanel();
		buttonsPanel.setLayout(new GridBagLayout());
		
		buttonIndex=0;
		
		queueActionButton= new JToggleButton();
		queueActionButton.setMargin(ApplicationHelper.getDefaultFatButtonMargin());
		queueActionButton.setAction(getQueuedButtonAction());
		queueActionButton.setRolloverEnabled(false);
		queueActionButton.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_queue_off));
		queueActionButton.setSelectedIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_queue_on));
		queueActionButton.setDisabledSelectedIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_queue_off));
		buttonsPanel.add(queueActionButton,new GridBagConstraints(buttonIndex++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));

		b = new JButton();
		b.setMargin(ApplicationHelper.getDefaultFatButtonMargin());
		b.setAction(getResolveAction());
		buttonsPanel.add(b,new GridBagConstraints(buttonIndex++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		//jp.add(b);
		
		b = new JButton();
		b.setMargin(ApplicationHelper.getDefaultFatButtonMargin());
		b.setAction(getDeleteAction());
		buttonsPanel.add(b,new GridBagConstraints(buttonIndex++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		//jp.add(b);

		reopenButton = new JButton();
		reopenButton.setMargin(ApplicationHelper.getDefaultFatButtonMargin());
		reopenButton.setAction(getReopenAction());
		reopenButton.setVisible(false);
		buttonsPanel.add(reopenButton,new GridBagConstraints(buttonIndex++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		//jp.add(b);

		//d= new Dimension(150,jp.getPreferredSize().height);
		//jp.setPreferredSize(d);
		//jp.setMinimumSize(jp.getPreferredSize());

		//d= new Dimension(150,getPreferredSize().height);
		//setPreferredSize(jp.getPreferredSize());
		//setMinimumSize(jp.getMinimumSize());

		if (packHorizontal) {
			add(buttonsPanel, new GridBagConstraints(0,++row,3,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,1,1,1),0,0));
		} else {
			add(buttonsPanel, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,1,1,1),0,0));
		}

		getActionMap().put(JACTION_DELETE, getDeleteAction());
		getActionMap().put(JACTION_QUEUE, getQueuedAction());
		getActionMap().put(JACTION_REOPEN, getReopenAction());
		getActionMap().put(JACTION_RESOLVE, getResolveAction());


		setEnabled(false);
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(buttonsPanel.getPreferredSize().width,super.getPreferredSize().height);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(buttonsPanel.getMinimumSize().width,super.getMinimumSize().height);
	}

	private javax.swing.Action getOpenURLAction() {
		if (openURLAction==null) {
			openURLAction= new AbstractAction(ApplicationHelper.EMPTY_STRING,ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_browser)) {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if (actions!=null && actions[0].getUrl()!=null) {
						try {
							Desktop.getDesktop().browse(actions[0].getUrl().toURI());
						} catch (Exception e1) {
							Logger.getLogger(this.getClass()).info("I/O error.", e1); //$NON-NLS-1$
							JOptionPane.showConfirmDialog(ActionPanel.this, Messages.getString("ActionPanel.LinkError.1")+actions[0].getUrl()+Messages.getString("ActionPanel.LinkError.2")+e1.getMessage(), Messages.getString("ActionPanel.LinkError.title"), JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				}
			
			};
			openURLAction.putValue(AbstractAction.SHORT_DESCRIPTION, Messages.getString("ActionPanel.Link.desc")); //$NON-NLS-1$
			openURLAction.setEnabled(false);
		}
		return openURLAction;

	}

	private javax.swing.Action getDeleteAction() {
		if (deleteAction==null) {
			deleteAction= new AbstractAction(Messages.getString("ActionPanel.Delete")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					Action[] aa=actions;
					for (Action a : aa) {
						a.setResolution(Resolution.DELETED);
					}
				}
			
			};
			deleteAction.putValue(AbstractAction.SHORT_DESCRIPTION, Messages.getString("ActionPanel.Delete.desc")); //$NON-NLS-1$
			deleteAction.putValue(AbstractAction.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_delete));
			deleteAction.putValue(AbstractAction.SMALL_ICON, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_delete));
			deleteAction.setEnabled(false);
		}
		return deleteAction;
	}

	private javax.swing.Action getResolveAction() {
		if (resolveAction==null) {
			resolveAction= new AbstractAction(Messages.getString("ActionPanel.Resolve")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					Action[] aa=actions;
					for (Action a : aa) {
						a.setResolution(Resolution.RESOLVED);
					}
				}
			
			};
			resolveAction.putValue(AbstractAction.SHORT_DESCRIPTION, Messages.getString("ActionPanel.Resolve.desc")); //$NON-NLS-1$
			resolveAction.putValue(AbstractAction.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_resolve));
			resolveAction.putValue(AbstractAction.SMALL_ICON, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_resolve));
			
			resolveAction.setEnabled(false);
		}
		return resolveAction;
	}

	private javax.swing.Action getQueuedAction() {
		if (queuedAction==null) {
			queuedAction= new AbstractAction(Messages.getString("ActionPanel.EnqueueDequeue")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					Action[] aa=actions;
					boolean b= !queueActionButton.isSelected();
					for (Action a : aa) {
						a.setQueued(b);
					}
					updateGUI();
				}
			
			};
			queuedAction.putValue(AbstractAction.SHORT_DESCRIPTION, Messages.getString("ActionPanel.Queue.desc")); //$NON-NLS-1$
			queuedAction.putValue(AbstractAction.SMALL_ICON, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_queue_on));
			queuedAction.putValue(AbstractAction.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_queue_on));
			queuedAction.setEnabled(false);
		}
		return queuedAction;
	}

	private javax.swing.Action getQueuedButtonAction() {
		if (queuedButtonAction==null) {
			queuedButtonAction= new AbstractAction(Messages.getString("ActionPanel.Queue")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					Action[] aa=actions;
					boolean b= queueActionButton.isSelected();
					for (Action a : aa) {
						a.setQueued(b);
					}
					updateGUI();
				}
			
			};
			queuedButtonAction.putValue(AbstractAction.SHORT_DESCRIPTION, Messages.getString("ActionPanel.Queue.desc")); //$NON-NLS-1$
			queuedButtonAction.putValue(AbstractAction.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_queue_off));
			queuedButtonAction.setEnabled(false);
		}
		return queuedButtonAction;
	}

	private javax.swing.Action getReopenAction() {
		if (reopenAction==null) {
			reopenAction= new AbstractAction(Messages.getString("ActionPanel.Reopen")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					Action[] aa=actions;
					for (Action a : aa) {
						a.setResolution(Resolution.OPEN);
					}
				}
			
			};
			reopenAction.putValue(AbstractAction.SHORT_DESCRIPTION, Messages.getString("ActionPanel.Reopen.desc")); //$NON-NLS-1$
			reopenAction.putValue(AbstractAction.LARGE_ICON_KEY, ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_undelete));
			reopenAction.putValue(AbstractAction.SMALL_ICON, ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_undelete));

			reopenAction.setEnabled(false);
		}
		return reopenAction;
	}

	/**
	 * @return the action
	 */
	public Action[] getActions() {
		return actions;
	}

	/**
	 * @param action the action to set
	 */
	public void setActions(Action[] action) {
		if (this.actions!=null) {
			for (Action a : this.actions) {
				a.getParent().removeFolderListener(this);
			}
		}
		this.actions = action;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateGUI();
			}
		});
		setEnabled(action!=null);
		if (this.actions!=null) {
			for (Action a : this.actions) {
				a.getParent().addFolderListener(this);
			}
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		//super.setEnabled(enabled);
		descText.setEnabled(enabled);
		urlText.setEnabled(enabled);
		datePicker.setEnabled(enabled);
		//previousDayButton.setEnabled(enabled);
		//nextDayButton.setEnabled(enabled);
		clearDateButton.setEnabled(enabled);
		projectCombo.setEnabled(enabled);
		priorityPanel.setEnabled(enabled);
		boolean hasOpen= Action.hasOpen(actions);
		getReopenAction().setEnabled(enabled && Action.hasNonOpen(actions));
		getResolveAction().setEnabled(enabled && hasOpen);
		getDeleteAction().setEnabled(enabled && Action.hasNonDeleted(actions));
		getQueuedAction().setEnabled(enabled && hasOpen);
		getQueuedButtonAction().setEnabled(enabled && hasOpen);
		getOpenURLAction().setEnabled(enabled && (actions!=null && actions[0].getUrl()!=null));
		/*if (moveDownButton.getAction()!=null) {
			moveDownButton.getAction().setEnabled(enabled && action!=null);
		}
		if (moveUpButton.getAction()!=null) {
			moveUpButton.getAction().setEnabled(enabled && action!=null);
		}*/
	}

	private void updateGUI() {
		boolean hasOpen= Action.hasOpen(actions);
		getReopenAction().setEnabled(Action.hasNonOpen(actions));
		getResolveAction().setEnabled(hasOpen);
		getDeleteAction().setEnabled(Action.hasNonDeleted(actions));
		getQueuedAction().setEnabled(hasOpen);
		getQueuedButtonAction().setEnabled(hasOpen);
		getOpenURLAction().setEnabled((actions!=null && actions[0].getUrl()!=null));

		if (actions==null) {
			idLabel.setText(Messages.getString("ActionPanel.NA")); //$NON-NLS-1$
			createdLabel.setText(Messages.getString("ActionPanel.NA")); //$NON-NLS-1$
			folderLabel.setText(Messages.getString("ActionPanel.NA")); //$NON-NLS-1$
			descText.setText(ApplicationHelper.EMPTY_STRING); //$NON-NLS-1$
			descText.clearUndoHistory();
			urlText.setText(ApplicationHelper.EMPTY_STRING); //$NON-NLS-1$
			projectCombo.setSelectedProject(null);
			datePicker.setDate(null);
			priorityPanel.setPriority(Priority.None);
		} else {
			if (actions.length==1) {
				String s= String.valueOf(actions[0].getId()); //$NON-NLS-1$
				idLabel.setText(s);
				s= ApplicationHelper.toISODateTimeString(actions[0].getCreated()); //$NON-NLS-1$
				createdLabel.setText(s);
				folderLabel.setText(actions[0].getParent().getName()); //$NON-NLS-1$
				if (!setting) {
					setting =true;
					if (queueActionButton.isSelected()!=actions[0].isQueued()) {
						queueActionButton.setSelected(actions[0].isQueued());
					}
					s=actions[0].getDescription()==null ? ApplicationHelper.EMPTY_STRING : actions[0].getDescription(); //$NON-NLS-1$
					descText.setEditable(true);
					descText.setEnabled(true);
					if (!s.equals(descText.getText())) {
						descText.setText(s);
						descText.setCaretPosition(0);
						descText.clearUndoHistory();
					}
					if (actions[0].getUrl()!=null) {
						urlText.setText(actions[0].getUrl().toString());	
					} else {
						urlText.setText(ApplicationHelper.EMPTY_STRING); //$NON-NLS-1$
					}
					if (actions[0].getRemind()!=null) {
						if (!actions[0].getRemind().equals(datePicker.getDate())) {
							datePicker.setDate(actions[0].getRemind());
						}
					} else {
						if (datePicker.getDate()!=null) {
							datePicker.setDate(null);
						}
					}
					if (actions[0].getProject()!=null) {
						projectCombo.setSelectedProject(actions[0].getParent().getParent().getProject(actions[0].getProject()));
					} else {
						projectCombo.setSelectedProject(null);
					}
					priorityPanel.setPriority(actions[0].getPriority());
					setting=false;
				}
			} else {
				String s= Messages.getString("ActionPanel.Multi"); //$NON-NLS-1$
				idLabel.setText(s);
				createdLabel.setText(s);
				folderLabel.setText(s); //$NON-NLS-1$
				descText.setEditable(false);
				descText.setEnabled(false);
				if (!setting) {
					setting =true;
					if (queueActionButton.isSelected()!=actions[0].isQueued()) {
						queueActionButton.setSelected(actions[0].isQueued());
					}
					descText.setText(s);
					urlText.setText(ApplicationHelper.EMPTY_STRING); //$NON-NLS-1$
					datePicker.setDate(null);
					projectCombo.setSelectedProject(null);
					priorityPanel.setPriority(null);
					setting=false;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			
			JFrame f= new JFrame();
			ActionPanel ap= new ActionPanel(false);
			ap.setEnabled(true);
			f.setContentPane(ap);
			f.setSize(300, 300);
			f.setVisible(true);
			
		} catch (Exception e) {
			e.toString();
		}
	}

	public void elementAdded(FolderEvent note) {
		//
	}

	public void elementModified(org.gtdfree.model.ActionEvent note) {
		if (note.getAction()==actions[0]) {
			updateGUI();
		}
	}

	public void elementRemoved(FolderEvent note) {
		//
	}
	
	public void orderChanged(Folder f) {
		//
	}

	/**
	 * @return the gtdModel
	 */
	public GTDFreeEngine getEngine() {
		return engine;
	}

	/**
	 * @param gtdModel the gtdModel to set
	 */
	public void setEngine(GTDFreeEngine e) {
		this.engine = e;
		projectCombo.setGTDModel(engine.getGTDModel());
		descText.setEngine(e);
		engine.getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_ALL_ACTIONS, new PropertyChangeListener() {
		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				reopenButton.setVisible(engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
			}
		});
		reopenButton.setVisible(reopenButtonVisible || engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
	}
	
	public void setDescriptionTextMinimumHeight(int min) {
		descTextScroll.setMinimumSize(new Dimension(descTextScroll.getMinimumSize().width,min));
	}
	public int getDescriptionTextMinimumHeight() {
		return descTextScroll.getMinimumSize().height;
	}
	
	public void addSwingActions(ActionMap actions) {
		moveDownButton.setAction(actions.get(ActionTable.JACTION_MOVE_DOWN));
		moveUpButton.setAction(actions.get(ActionTable.JACTION_MOVE_UP));
		selectNextButton.setAction(actions.get(ActionTable.JACTION_SELECT_NEXT));
		selectPreviousButton.setAction(actions.get(ActionTable.JACTION_SELECT_PREVIOUS));
	}

	public void setReopenButtonVisible(boolean b) {
		reopenButtonVisible=b;
		reopenButton.setVisible(reopenButtonVisible || engine.getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
	}
	
	public boolean isReopenButtonVisible() {
		return reopenButtonVisible;
	}
}
