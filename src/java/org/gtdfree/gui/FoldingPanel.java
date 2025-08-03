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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.Messages;

/**
 * @author ikesan
 *
 */
public class FoldingPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	class FoldingButton extends JToggleButton {
		private static final long serialVersionUID = 1L;

		private JComponent fold;

		public FoldingButton(String name, JComponent f) {
			super();
			setText(name);
			setName(name);
			this.fold=f;
			
			setMargin(new Insets(0,0,0,0));
			setSelectedIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_unfolded));
			setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_folded));
			setPreferredSize(new Dimension(getPreferredSize().width,21));
			setToolTipText(Messages.getString("FoldingPanel.0")+name+Messages.getString("FoldingPanel.1")); //$NON-NLS-1$ //$NON-NLS-2$
			
			addItemListener(new ItemListener() {
			
				@Override
				public void itemStateChanged(ItemEvent e) {
					fold.setVisible(isSelected());
					if (FoldingPanel.this.getParent()!=null) {
						FoldingPanel.this.getParent().validate();
					}
				}
			});
		}
	}
	
	private JPanel foldingBar;
	private List<FoldingButton> folds = new ArrayList<FoldingButton>();
	private JLabel titleLabel;

	public FoldingPanel() {
		initialize();
	}
	
	private void initialize() {

		setLayout(new GridBagLayout());
		
		titleLabel= new JLabel(""); //$NON-NLS-1$
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		add(titleLabel, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));

		foldingBar= new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(super.getMinimumSize().width,super.getPreferredSize().height);
			}
		};
		foldingBar.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		add(foldingBar, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.NORTH,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));
		
	}

	public void addFold(String name, JComponent fold, boolean unfolded, boolean stretching, boolean butonVisible) {
		
		FoldingButton fb= new FoldingButton(name,fold);
		foldingBar.add(fb);
		
		GridBagConstraints g= new GridBagConstraints(0,getComponentCount(),1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(2,0,0,0),0,0);
		g.weighty= stretching ? 1.0 : 0.0;
		
		add(fold,g);
		
		folds.add(fb);
		
		fb.setSelected(unfolded);
		fold.setVisible(unfolded);
		fb.setVisible(butonVisible);

		folds.get(folds.size()-1).setVisible(butonVisible);
	}
	
	public void addFold(String name, JComponent fold, boolean unfolded, boolean stretching) {
		addFold(name, fold, unfolded, stretching, true);
	}
	
	public void setFoldingState(String name, boolean state) {
		for (FoldingButton fb : folds) {
			if (fb.getName().equals(name)) {
				fb.setSelected(state);
				return;
			}
		}
	}
	
	public boolean[] getFoldingStates() {
		boolean[] b= new boolean[folds.size()];
		
		for (int i = 0; i < b.length; i++) {
			b[i]= folds.get(i).isSelected();
		}
		
		return b;
	}
	
	public void setTitle(String title) {
		titleLabel.setText(title);
	}
	
	public String getTitle() {
		return titleLabel.getText();
	}

	public void setFoldingStates(boolean[] b) {
		if (b==null || b.length==0) {
			return;
		}
		for (int i = 0; i < b.length && i < folds.size(); i++) {
			folds.get(i).setSelected(b[i]);
		}
	}
}
