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

package org.gtdfree.journal;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.EventListenerList;

import org.gtdfree.model.Action;

/**
 * @author ikesan
 *
 */
public class JournalEntry {
	
	//private int id;
	private long day;
	private String comment;
	private List<Interval> intervals= new ArrayList<Interval>();
	private List<Action> actions;
	private boolean checked=false;
	private EventListenerList listeners= new EventListenerList();
	
	public JournalEntry(/*int id*/) {
		//this.id=id;
	}
	
	/**
	 * @return the id
	 */
//	public int getId() {
//		return id;
//	}
	
	/**
	 * @return the day
	 */
	public long getDay() {
		return day;
	}
	
	/**
	 * @param day the day to set
	 */
	public void setDay(long day) {
		if (this.day == day) {
			return;
		}
		long old= this.day;
		this.day = day;
		fireJournalEntryChanged("day", day, old); //$NON-NLS-1$
	}
	
	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}
	
	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		if ((comment!=null && comment.equals(this.comment)) || comment==null&&this.comment==null) {
			return;
		}
		String old= this.comment;
		this.comment = comment;
		fireJournalEntryChanged("comment", comment, old); //$NON-NLS-1$
	}
	
	/**
	 * @return the intervals
	 */
	public Interval[] getIntervals() {
		return intervals.toArray(new Interval[intervals.size()]);
	}
	
	/**
	 * @return the intervals
	 */
	public int getIntervalCount() {
		return intervals.size();
	}

	/**
	 * @return the actions
	 */
	public List<Action> getActions() {
		return actions;
	}
	
	/**
	 * @param actions the actions to set
	 */
	public void setActions(List<Action> actions) {
		this.actions = actions;
	}
	
	/**
	 * @return the checked
	 */
	public boolean isChecked() {
		return checked;
	}
	
	/**
	 * @param checked the checked to set
	 */
	public void setChecked(boolean checked) {
		if (checked==this.checked) {
			return;
		}
		this.checked = checked;
		fireJournalEntryChanged("checked", checked); //$NON-NLS-1$
	}
	
	public void addInterval(Interval interval) {
		intervals.add(interval);
		fireJournalEntryIntervalAdded(interval, intervals.size()-1);
	}
	
	public void removeInterval(Interval interval) {
		int i= intervals.indexOf(interval);
		if(i == -1) return;
		intervals.remove(i);
		fireJournalEntryIntervalRemoved(interval, i);
	}

	public void setInterval(int index, Interval interval) {
		Interval i= intervals.get(index);
		if ((interval!=null && interval.equals(i)) || interval==null&&i==null) {
			return;
		}
		intervals.set(index, interval);
		fireJournalEntryChanged(new JournalEntryEvent(this,"intervals",interval,i,index)); //$NON-NLS-1$
	}
	
	public void addJournalEntryListener(JournalEntryListener l) {
		listeners.add(JournalEntryListener.class, l);
	}

	public void removeJournalEntryListener(JournalEntryListener l) {
		listeners.remove(JournalEntryListener.class, l);
	}
	
	private void fireJournalEntryChanged(JournalEntryEvent e) {
		if (e.getNewValue()==e.getOldValue()) {
			return;
		}
		
		JournalEntryListener[] l= listeners.getListeners(JournalEntryListener.class);
		
		for (int i = 0; i < l.length; i++) {
			l[i].journalEntryChanged(e);
		}
	}
	
	private void fireJournalEntryIntervalAdded(Interval it, int ix) {
		JournalEntryEvent e= new JournalEntryEvent(this,"intervals",it,null,ix); //$NON-NLS-1$
		
		JournalEntryListener[] l= listeners.getListeners(JournalEntryListener.class);
		
		for (int i = 0; i < l.length; i++) {
			l[i].journalEntryIntervalAdded(e);
		}
	}

	private void fireJournalEntryIntervalRemoved(Interval it, int ix) {
		JournalEntryEvent e= new JournalEntryEvent(this,"intervals",null,it,ix); //$NON-NLS-1$
		
		JournalEntryListener[] l= listeners.getListeners(JournalEntryListener.class);
		
		for (int i = 0; i < l.length; i++) {
			l[i].journalEntryIntervalRemoved(e);
		}
	}


	private void fireJournalEntryChanged(String prop, Object newValue, Object oldValue) {
		fireJournalEntryChanged(new JournalEntryEvent(this,prop,newValue,oldValue,-1));
	}
	
	private void fireJournalEntryChanged(String prop, boolean b) {
		fireJournalEntryChanged(new JournalEntryEvent(this,prop,b,!b,-1));
	}

	public Interval getInterval(int index) {
		return intervals.get(index);
	}

	public void intervalChanged(Interval interval) {
		fireJournalEntryChanged(new JournalEntryEvent(this,"intervals",interval,null,-1)); //$NON-NLS-1$
	}
}
