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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;

import com.lowagie.text.Font;

/**
 * @author ikesan
 *
 */
public class DatabaseSelectionDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		DatabaseSelectionDialog d= new DatabaseSelectionDialog(null);
		d.setVisible(true);
		System.out.println("Success "+d.isSuccess()); //$NON-NLS-1$
		System.out.println("DB "+d.getDatabase()); //$NON-NLS-1$
		System.out.println("Upgrade "+d.isUpgrade()); //$NON-NLS-1$
	}
	
	protected boolean upgrade;
	protected boolean cont=false;
	private JRadioButton rbXML;
	private JRadioButton rbODB;
	private JCheckBox cb;

	public DatabaseSelectionDialog(Window owner) {
		super(owner);
		initialize();
	}
	
	private void initialize() {
		setTitle(Messages.getString("DatabaseSelectionDialog.Title")); //$NON-NLS-1$
		setModal(true);
		
		JPanel p= new JPanel();
		p.setLayout(new GridBagLayout());
		
		ButtonGroup bg= new ButtonGroup();
		
		int row=0;
		JLabel l= new JLabel(Messages.getString("DatabaseSelectionDialog.DatabaseLoc")+" '"+ApplicationHelper.getDataFolder()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		p.add(l, new GridBagConstraints(0,row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(11,11,7,11),0,0));

		l= new JLabel(Messages.getString("DatabaseSelectionDialog.Question")); //$NON-NLS-1$
		p.add(l, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,11,4,11),0,0));

		rbXML= new JRadioButton();
		rbXML.setText(Messages.getString("DatabaseSelectionDialog.XML")); //$NON-NLS-1$
		bg.add(rbXML);
		p.add(rbXML, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,11,4,11),0,0));
		
		l= new JLabel(Messages.getString("DatabaseSelectionDialog.XMl.desc")); //$NON-NLS-1$
		l.setFont(l.getFont().deriveFont(Font.ITALIC));
		p.add(l, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,22,4,11),0,0));
		
		cb= new JCheckBox();

		rbODB= new JRadioButton();
		rbODB.setText(Messages.getString("DatabaseSelectionDialog.ODB")); //$NON-NLS-1$
		rbODB.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				cb.setEnabled(rbODB.isSelected());
			}
		});
		rbODB.setSelected(true);
		bg.add(rbODB);
		p.add(rbODB, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,11,4,11),0,0));

		l= new JLabel(Messages.getString("DatabaseSelectionDialog.ODB.desc")); //$NON-NLS-1$
		l.setFont(l.getFont().deriveFont(Font.ITALIC));
		p.add(l, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,22,4,11),0,0));

		cb.setText(Messages.getString("DatabaseSelectionDialog.Import")); //$NON-NLS-1$
		cb.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				upgrade=cb.isSelected();
			}
		});
		p.add(cb, new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,22,4,11),0,0));

		JButton b= new JButton();
		b.setText(Messages.getString("DatabaseSelectionDialog.Continue")); //$NON-NLS-1$
		b.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				cont=true;
				DatabaseSelectionDialog.this.dispose();
			}
		});
		p.add(b, new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(11,11,11,4),0,0));

		b= new JButton();
		b.setText(Messages.getString("DatabaseSelectionDialog.Abort")); //$NON-NLS-1$
		b.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				DatabaseSelectionDialog.this.dispose();
			}
		});
		p.add(b, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(11,4,11,11),0,0));
	
		setContentPane(p);
		pack();
		
		setLocationRelativeTo(null);
	}
	
	public boolean isUpgrade() {
		return upgrade;
	}
	
	public boolean isSuccess() {
		return cont;
	}
	
	public String getDatabase() {
		if (!cont) {
			return null;
		}
		
		if (rbODB.isSelected()) {
			return GlobalProperties.DATABASE_VALUE_ODB;
		}
		
		return GlobalProperties.DATABASE_VALUE_XML;
	}
	
	
}
