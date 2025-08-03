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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JPanel;
import javax.swing.JTextPane;

import org.gtdfree.journal.JournalEntry;

/**
 * @author ikesan
 *
 */
public class JournalEntryPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;

//	class IntervalPanel extends JPanel {
//		private static final long serialVersionUID = 1L;
//		int index;
//		Interval interval;
//		private TimeField from;
//		private TimeField to;
//		
//		public IntervalPanel(int i, Interval in) {
//			index=i;
//			initialize();
//			setInterval(in);
//		}
//		
//		public void setIndex(int index) {
//			this.index = index;
//		}
//		
//		public void setInterval(Interval interval) {
//			this.interval = interval;
//			from.setTimeOfDay(interval.getStart());
//			to.setTimeOfDay(interval.getEnd());
//		}
//		
//		private void initialize() {
//			setLayout(new GridBagLayout());
//			
//			from = new TimeField();
//			from.addPropertyChangeListener(TimeField.PROPERTY_TIME_OF_DAY, new PropertyChangeListener() {
//			
//				@Override
//				public void propertyChange(PropertyChangeEvent evt) {
//					journalEntry.setInterval(index, new Interval(from.getTimeOfDay(),interval.getEnd()));
//				}
//			});
//			
//			to= new TimeField();
//			to.addPropertyChangeListener(TimeField.PROPERTY_TIME_OF_DAY, new PropertyChangeListener() {
//				
//				@Override
//				public void propertyChange(PropertyChangeEvent evt) {
//					journalEntry.setInterval(index, new Interval(interval.getStart(),to.getTimeOfDay()));
//				}
//			});
//
//			JButton b= new JButton();
//			b.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_add));
//			b.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					journalEntry.addInterval(new Interval(interval.getEnd(), interval.getEnd()));
//				}
//			});
//			
//			add(from, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
//			add(new JLabel("\u00E2"), new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
//			add(to, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
//			add(b, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
//			
//		}
//	}
	
	private JournalEntry journalEntry;
	//private JLabel idLabel;
	private JTextPane commentText;
	private IntervalFieldPanel intervalPanel;
	
	public JournalEntryPanel() {
		initialize();
	}

	public JournalEntryPanel(JournalEntry e) {
		this();
		setJournalEntry(e);
	}
	
	private void initialize() {
		
		setLayout(new GridBagLayout());
		
//		idLabel= new JLabel();
//		idLabel.setText("ID: N/A");
//		idLabel.setFont(idLabel.getFont().deriveFont(Font.BOLD));
//		add(idLabel, new GridBagConstraints(0,0,1,1,0.0,0.0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(1,4,1,4),0,0));
		
		commentText= new JTextPane();
		commentText.setOpaque(true);
		commentText.setBackground(Color.WHITE);
		commentText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				 journalEntry.setComment(commentText.getText());
			}
		});
		//JScrollPane jsp= new JScrollPane(commentText);
		add(commentText, new GridBagConstraints(1,0,1,1,1.0,1.0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,4,1,4),0,0));

		intervalPanel= new IntervalFieldPanel();
		
		add(intervalPanel, new GridBagConstraints(3,0,1,1,0.0,1.0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(1,4,1,4),0,0));
		
		//setMinimumSize(new Dimension(100,24));
		//setPreferredSize(new Dimension(100,24));
	}
	
	public void setJournalEntry(JournalEntry journalEntry) {
		this.journalEntry = journalEntry;
		
		//idLabel.setText("ID: "+journalEntry.getId());
		commentText.setText(journalEntry.getComment());
		
		intervalPanel.setEntry(journalEntry);
	}
	
	public void release() {
		intervalPanel.setEntry(null);
		this.journalEntry = null;
	}
}
