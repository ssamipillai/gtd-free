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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.journal.Interval;
import org.gtdfree.journal.JournalEntry;
import org.gtdfree.journal.JournalEntryEvent;
import org.gtdfree.journal.JournalEntryListener;
import org.gtdfree.journal.JournalModel;
import org.gtdfree.journal.JournalTools;

/**
 * @author ikesan
 *
 */
public class IntervalField extends JPanel implements JournalEntryListener {
	
	private static final long serialVersionUID = 1L;
	private JournalEntry entry;
	private Interval interval;
	//private int index;
	private JTextField field;
	//private static final DateFormat format= new SimpleDateFormat("HH:mm");
	private boolean setting;
	private IntervalFieldPanel parent;
	private boolean removeEnabled=true;
	private JButton removeButton;
	private Dimension minFieldSize;
	
	private static Interval activeInterval;
	
	private static Thread intervalUpdater;
	
	static {
		intervalUpdater = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(true) {
						synchronized(intervalUpdater) {
							if(activeInterval == null) { continue; }
							activeInterval.setEnd(JournalTools.secondsOfDay());
						}
						Thread.sleep(1000);
					}
				} catch (InterruptedException ie) {				
				}
			}
		});
		intervalUpdater.setDaemon(true);
		intervalUpdater.start();
	}
	
	public IntervalField() {
	}
	
	public IntervalField(IntervalFieldPanel parent, JournalEntry entry, Interval interval) {
		initialize(parent, entry, interval);
	}

	private void initialize(IntervalFieldPanel parent, JournalEntry entry, Interval interval) {
		this.parent = parent;
		this.entry = entry;
		this.interval = interval;
		
		setLayout(new GridBagLayout());
		
		field= new JTextField();
		field.setInputVerifier(new InputVerifier() {
		
			@Override
			public boolean verify(JComponent input) {
				String s= ((JTextField)input).getText();
				return toInterval(IntervalField.this.interval, s);
			}
		});
		// Is this really the right event?
		field.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setting=true;
				if(toInterval(IntervalField.this.interval, field.getText())) {
					updateInterval();
				}
				setting=false;
			}
		});
		field.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				setting=true;
				toInterval(IntervalField.this.interval, field.getText());
				updateInterval();
				setting=false;
			}
		});
		
		add(field, new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
		
		entry.addJournalEntryListener(this);
		
		JButton b= new JButton();
		b.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_add));
		b.setMargin(new Insets(0,0,0,0));
		b.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				//int i = interval.getEnd();
				IntervalField newField = IntervalField.this.parent.addInterval(
						new Interval(IntervalField.this.entry, 
								JournalTools.secondsOfDay(), JournalTools.secondsOfDay())
				);
				synchronized(intervalUpdater) {
					activeInterval = newField.interval;
				}
			}
		});
		add(b, new GridBagConstraints(1,0,1,1,0.0,0.0,GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(0,11,0,0),0,0));
		
		removeButton= new JButton();
		removeButton.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_remove));
		removeButton.setMargin(new Insets(0,0,0,0));
		removeButton.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				IntervalField.this.entry.removeInterval(IntervalField.this.interval);
			}
		});
		add(removeButton, new GridBagConstraints(2,0,1,1,0.0,0.0,GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(0,0,0,0),0,0));

		updateInterval();
	}
	
	@Override
	public void journalEntryIntervalRemoved(JournalEntryEvent e) {
		if (e.getOldValue() == this.interval) {
			parent.remove(this);
			release();
		}
	}

	@Override
	public void journalEntryIntervalAdded(JournalEntryEvent e) {
	}

	@Override
	public void journalEntryChanged(JournalEntryEvent e) {
		if(e.getNewValue() == this.interval) {
			updateInterval();
		}
	}

	public void release() {
		
		entry.removeJournalEntryListener(this);
		entry=null;
		interval=null;
	}

	private String toString(Interval i) {
		return JournalModel.timeToString(i.getStart()) + " - " + JournalModel.timeToString(i.getEnd()); //$NON-NLS-1$
	}
	
	private boolean toInterval(Interval i, String s) {
		String[] startEnd = s.split("-"); //$NON-NLS-1$
		if(startEnd.length != 2) {
			return false;
		}
		int start = JournalModel.timeFromString(startEnd[0]);
		int end = JournalModel.timeFromString(startEnd[1]);
		if(start == -1 || end == -1) {
			return false;
		}
		
		i.setStart(start);
		i.setEnd(end);
		
		return true;
	}
		
	public void updateInterval() {
		if(!setting) {
			field.setText(toString(interval));
		}
		
		if (minFieldSize==null) {
			minFieldSize= new Dimension(field.getPreferredSize().width,removeButton.getPreferredSize().height);
			field.setMinimumSize(minFieldSize);
			field.setPreferredSize(minFieldSize);
			validate();
		}
	}
	
	public boolean isRemoveEnabled() {
		return removeEnabled;
	}
	
	public void setRemoveEnabled(boolean removeEnabled) {
		this.removeEnabled = removeEnabled;
		removeButton.setEnabled(removeEnabled);
	}

}
