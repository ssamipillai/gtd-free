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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.model.Folder;


/**
 * @author ikesan
 *
 */
public class QuickCollectPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private GTDFreeEngine engine;
	InputTextArea ideaText;
	private AbstractAction clearAction;
	private AbstractAction doneNoteAction;
	private JRadioButton destinationInB;
	private JRadioButton destinationSel;
	
	public QuickCollectPanel() {
		initialize();
	}

	private void initialize() {

		setLayout(new GridBagLayout());
		
		int col=0;
		add(new JLabel(Messages.getString("QuickCollectPanel.Quick")),new GridBagConstraints(col++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(2,4,2,0),0,0)); //$NON-NLS-1$
		
		JScrollPane jsp= new JScrollPane();
		ideaText= new InputTextArea();
		ideaText.setLineWrap(true);
		ideaText.setWrapStyleWord(true);
		ideaText.setRows(1);
		ideaText.setMargin(new Insets(2,4,2,4));
		ideaText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter"); //$NON-NLS-1$
		ideaText.getActionMap().put("Enter", new AbstractAction() { //$NON-NLS-1$
			private static final long serialVersionUID = 1348070910974195411L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (getDoneNoteAction().isEnabled()) {
					getDoneNoteAction().actionPerformed(e);
				}
			}
		});
		ideaText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "NewLine"); //$NON-NLS-1$
		ideaText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "NewLine"); //$NON-NLS-1$
		ideaText.getActionMap().put("NewLine", new AbstractAction() { //$NON-NLS-1$
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				ideaText.insert("\n", ideaText.getCaretPosition()); //$NON-NLS-1$
			}
		});
		jsp.setViewportView(ideaText);
		jsp.setMinimumSize(ideaText.getPreferredScrollableViewportSize());
		jsp.setPreferredSize(ideaText.getPreferredScrollableViewportSize());
		add(jsp,new GridBagConstraints(col++,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(2,4,2,0),0,0));
		
		ButtonGroup bg= new ButtonGroup();
		
		destinationInB= new JRadioButton();
		destinationInB.setText(Messages.getString("QuickCollectPanel.InB.short")); //$NON-NLS-1$
		destinationInB.setToolTipText(Messages.getString("QuickCollectPanel.InB.desc")); //$NON-NLS-1$
		destinationInB.setSelected(true);
		bg.add(destinationInB);
		add(destinationInB,new GridBagConstraints(col++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(4,4,4,0),0,0));
		
		destinationSel= new JRadioButton();
		destinationSel.setText(Messages.getString("QuickCollectPanel.Sel.short")); //$NON-NLS-1$
		destinationSel.setToolTipText(Messages.getString("QuickCollectPanel.Sel.desc")); //$NON-NLS-1$
		bg.add(destinationSel);
		add(destinationSel,new GridBagConstraints(col++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(4,0,4,4),0,0));

		JButton b= new JButton();
		b.setMargin(new Insets(0,0,0,0));
		b.setAction(getDoneNoteAction());
		add(b,new GridBagConstraints(col++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(2,4,2,0),0,0));

		b= new JButton();
		b.setMargin(new Insets(0,0,0,0));
		b.setAction(getClearAction());
		add(b,new GridBagConstraints(col++,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(2,4,2,4),0,0));
	}
	
	private javax.swing.Action getClearAction() {
		if (clearAction == null) {
			clearAction = new AbstractAction(Messages.getString("InBasketPane.Clear"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_clear)) { //$NON-NLS-1$

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					ideaText.setText(ApplicationHelper.EMPTY_STRING);
					ideaText.requestFocus();
				}
			};
			clearAction.putValue(AbstractAction.SHORT_DESCRIPTION, Messages.getString("InBasketPane.Clear.desc")); //$NON-NLS-1$
			clearAction.setEnabled(true);
			
		}

		return clearAction;
	}

	private javax.swing.Action getDoneNoteAction() {
		if (doneNoteAction == null) {
			doneNoteAction = new AbstractAction(Messages.getString("InBasketPane.Add"),ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_add)) { //$NON-NLS-1$
			
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (ideaText.getText()==null || ideaText.getText().length()==0) {
						return;
					}
					
					if (destinationInB.isSelected()) {
						
						getEngine().getGTDModel().createAction(engine.getGTDModel().getInBucketFolder(), ideaText.getText());
						ideaText.setText(ApplicationHelper.EMPTY_STRING);
					
					} else {
						
						WorkflowPane wp= getEngine().getActiveWorkflowPane();
						if (wp!=null) {
							Folder f= wp.getSelectedFolder();
							
							if (f!=null && (f.isUserFolder() || f.isInBucket())) {

								getEngine().getGTDModel().createAction(f, ideaText.getText());
								ideaText.setText(ApplicationHelper.EMPTY_STRING);
								
							}
						}
						
					}
					
				}
			};
			doneNoteAction.putValue(AbstractAction.SHORT_DESCRIPTION, Messages.getString("InBasketPane.Add.desc")); //$NON-NLS-1$
		}

		return doneNoteAction;
	}

	public void setEngine(GTDFreeEngine engine) {
		this.engine = engine;
	}
	
	public GTDFreeEngine getEngine() {
		return engine;
	}

}
