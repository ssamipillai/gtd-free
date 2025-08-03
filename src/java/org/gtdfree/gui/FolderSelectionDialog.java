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
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.gtdfree.Messages;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDModel;

/**
 * @author ikesan
 *
 */
public class FolderSelectionDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		try {
			FolderSelectionDialog d= new FolderSelectionDialog(false);
			System.out.println(d.showFolderSelectionDialog());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private FolderSelectionList folderList;

	public FolderSelectionDialog(boolean projects) {
		initialize(projects);
	}

	private void initialize(boolean projects) {
		
		JPanel p= new JPanel();
		p.setLayout(new GridBagLayout());
		
		folderList= new FolderSelectionList(projects);
		
		JScrollPane sp= new JScrollPane();
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setViewportView(folderList);
		
		p.add(sp, new GridBagConstraints(0,0,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(11,11,11,11),0,0));
		
		JButton b= new JButton(folderList.getSelectMarkedAction());
		p.add(b, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,11,2,2),0,0));
	
		b= new JButton(folderList.getDeselectMarkedAction());
		p.add(b, new GridBagConstraints(1,1,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,2,2,11),0,0));
	
		b= new JButton(folderList.getSelectAllAction());
		p.add(b, new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,11,11,2),0,0));
		
		b= new JButton(folderList.getDeselectAllAction());
		p.add(b, new GridBagConstraints(1,2,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,2,11,11),0,0));

		b= new JButton(Messages.getString("FolderSelectionDialog.Close")); //$NON-NLS-1$
		b.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				FolderSelectionDialog.this.dispose();
			}
		});
		p.add(b, new GridBagConstraints(0,3,2,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(0,11,11,11),0,0));

		setContentPane(p);
		
		setModalityType(DEFAULT_MODALITY_TYPE);
		setTitle(projects ? Messages.getString("FolderSelectionDialog.Proj") : Messages.getString("FolderSelectionDialog.List")); //$NON-NLS-1$ //$NON-NLS-2$
		setSize(400, 480);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		/*addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				synchronized (FolderSelectionDialog.this) {
					FolderSelectionDialog.this.notify();
				}
			}
		});*/

	}
	
	public void setGtdModel(GTDModel m) {
		folderList.setGtdModel(m);
	}
	
	public Folder[] showFolderSelectionDialog() {
		
		folderList.refresh();
		
		setVisible(true);
		
		return folderList.getSelectedFolders();
		
	}
	
}
