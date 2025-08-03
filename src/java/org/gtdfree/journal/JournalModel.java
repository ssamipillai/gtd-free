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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.EventListenerList;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.model.Action;
import org.gtdfree.model.GTDModel;

/**
 * @author ikesan
 *
 */
public class JournalModel {
	
	class EventHandler implements JournalModelListener {
		private void markDirtyDay(JournalEntry je) {
			if(je != null) {
				dirtyDays.add(je.getDay());
			}
		}
		
		@Override
		public void journalEntryIntervalRemoved(JournalEntryEvent e) {
			JournalModelListener[] l= listeners.getListeners(JournalModelListener.class);
			
			for (int i = 0; i < l.length; i++) {
				l[i].journalEntryIntervalRemoved(e);
			}

			markDirtyDay(e.getJournalEntry());
		}
	
		@Override
		public void journalEntryIntervalAdded(JournalEntryEvent e) {
			JournalModelListener[] l= listeners.getListeners(JournalModelListener.class);
			
			for (int i = 0; i < l.length; i++) {
				l[i].journalEntryIntervalAdded(e);
			}

			markDirtyDay(e.getJournalEntry());
		}
	
		@Override
		public void journalEntryChanged(JournalEntryEvent e) {
			JournalModelListener[] l= listeners.getListeners(JournalModelListener.class);
			
			for (int i = 0; i < l.length; i++) {
				l[i].journalEntryChanged(e);
			}
			
			if(e.getProperty().equals("day") && e.getOldValue() != null) { //$NON-NLS-1$
				dirtyDays.add((Long)e.getOldValue());
			}
			markDirtyDay(e.getJournalEntry());
		}
	
		@Override
		public void journalEntryAdded(JournalEntryEvent e) {
			JournalModelListener[] l= listeners.getListeners(JournalModelListener.class);
			
			for (int i = 0; i < l.length; i++) {
				l[i].journalEntryAdded(e);
			}

			markDirtyDay(e.getJournalEntry());
		}

		public void journalEntryAdded(JournalEntry je) {
			JournalEntryEvent e= new JournalEntryEvent(je,"entries",je,null,-1);  //$NON-NLS-1$
			journalEntryAdded(e);
		}
	};


	// Used for generating XML document. Copied from GTDModel.
	private final static String EOL="\n"; //$NON-NLS-1$
	private final static String SKIP="  "; //$NON-NLS-1$
	private final static String SKIPSKIP="    "; //$NON-NLS-1$
	
	private final String TAG_GTD_JOURNAL_DATA = "gtd-journal-data"; //$NON-NLS-1$
	private final String ATTR_VERSION = "version"; //$NON-NLS-1$
	private final String ATTR_MODIFIED = "modified"; //$NON-NLS-1$
	
	private final String TAG_ENTRY = "entry"; //$NON-NLS-1$
	//private final String ATTR_ENTRY_ID = "id";
	private final String ATTR_ENTRY_COMMENT = "comment"; //$NON-NLS-1$
	private final String ATTR_ENTRY_CHECKED = "checked"; //$NON-NLS-1$
	
	private final String TAG_INTERVAL = "interval"; //$NON-NLS-1$
	private final String ATTR_INTERVAL_START = "start"; //$NON-NLS-1$
	private final String ATTR_INTERVAL_END = "end"; //$NON-NLS-1$
	
	private final String TAG_ACTION = "action"; //$NON-NLS-1$
	private final String ATTR_ACTION_ID = "id"; //$NON-NLS-1$
	
	private final String VERSION_1_0 = "1.0"; //$NON-NLS-1$
	
	private final static ThreadLocal<DateFormat> JOURNAL_DATE_FORMAT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd")); //$NON-NLS-1$
	
	private File dataFolder;

	private GTDModel gtdModel;
	//private int lastEntryID=0; 
	private Map<Long,List<JournalEntry>> data;
	private EventListenerList listeners= new EventListenerList();
	private EventHandler eventHandler= new EventHandler();
	// A list of all days whose journal entries have changed and require to be saved.
	private Set<Long> dirtyDays = new HashSet<Long>();

	public JournalModel(File dataFolder, GTDModel gtdModel) {
		data= new HashMap<Long, List<JournalEntry>>();
		this.dataFolder = dataFolder;
		this.gtdModel = gtdModel;
	}
	
	public JournalEntry[] getEntries(long day) {
		List<JournalEntry> l= data.get(day);
	
		if(l == null) {
			try {
				l = load(day);
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				l = new LinkedList<JournalEntry>();
			} 
			data.put(day, l);
		}
		
		return l.toArray(new JournalEntry[l.size()]);
	}
	
	public JournalEntry addEntry(long day) {
		
		JournalEntry e= new JournalEntry(/*lastEntryID++*/);
		
		e.setDay(day);
		
		List<JournalEntry> l= data.get(day);
		if (l==null) {
			l= new ArrayList<JournalEntry>();
			data.put(day, l);
		}
		l.add(e);
		
		e.addJournalEntryListener(eventHandler);
		eventHandler.journalEntryAdded(e);
		
		return e;		
	}
	
	public void addJournalModelListener(JournalModelListener l) {
		listeners.add(JournalModelListener.class, l);
	}
	
	public void removeJournalModelListener(JournalModelListener l) {
		listeners.remove(JournalModelListener.class, l);
	}

	private File getJournalFile(long day) {
		Date date = JournalTools.toDate(day);
		return new File(dataFolder, "journal-" + JOURNAL_DATE_FORMAT.get().format(date) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public List<JournalEntry> load(Long day) throws IOException, XMLStreamException, FactoryConfigurationError {
		LinkedList<JournalEntry> result = new LinkedList<JournalEntry>();
		
		File file = getJournalFile(day);
		if(!file.exists() || !file.canRead()) {
			return result;
		}
		
		XMLStreamReader r;
		
		// buffer size is same as default in 1.6, we explicitly request it so, not to brake if defaut changes.
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bin= new BufferedInputStream(fis, 8192);
		bin.mark(8191);
		
		Reader rr= new InputStreamReader(bin);
		CharBuffer b= CharBuffer.allocate(96);
		rr.read(b);
		b.position(0);
		//System.out.println(b);
		Pattern pattern = Pattern.compile("<\\?.*?encoding\\s*?=.*?\\?>",Pattern.CASE_INSENSITIVE);	             //$NON-NLS-1$
        Matcher matcher = pattern.matcher(b);

        // reset back to start of file
        bin.reset();

        // we check if encoding is defined in xml, by the book encoding on r should be null if not defined in xml,
        // but in reality it can be arbitrary if not defined in xml. So we have to check ourselves.
        if (matcher.find()) {
        	//System.out.println(matcher);
        	// if defined, then XML parser will pick it up and use it
			r = XMLInputFactory.newInstance().createXMLStreamReader(bin);
			System.out.println("XML declared encoding: "+r.getEncoding()+", system default encoding: "+Charset.defaultCharset()); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
        	//System.out.println(matcher);
        	// if not defined, then we assume it is generated by gtd-free version 0.4 or some local editor,
        	// so we assume system default encoding.
			r = XMLInputFactory.newInstance().createXMLStreamReader(new InputStreamReader(bin));
			System.out.println("XML assumed system default encoding: "+Charset.defaultCharset()); //$NON-NLS-1$
        }
		
        int eventType;
        final int INIT = 0;
        final int EXPECT_ENTRY = 1;
        final int EXPECT_INTERVAL_OR_ACTION = 2;
        final int EXPECT_END_INTERVAL = 3;
        final int EXPECT_END_ACTION = 4;
        final int DONE = 5;
        int status = INIT;
        String version;
        JournalEntry je = null;
        while((eventType = r.nextTag()) != XMLStreamConstants.END_DOCUMENT) {
        	switch(eventType) {
        	case XMLStreamConstants.START_ELEMENT:
        		if(status == INIT && TAG_GTD_JOURNAL_DATA.equals(r.getName().getLocalPart())) {
        			status = EXPECT_ENTRY;
        			version = r.getAttributeValue(null, ATTR_VERSION);
        			if(!VERSION_1_0.equals(version)) {
        				throw new IOException("Unrecognized journal file version: " + version); //$NON-NLS-1$
        			}
        			break;
        		}
        		if(status == EXPECT_ENTRY && TAG_ENTRY.equals(r.getName().getLocalPart())) {
        			je = new JournalEntry();
        			je.setDay(day);
        			je.setChecked(Boolean.parseBoolean(r.getAttributeValue(null, ATTR_ENTRY_CHECKED)));
        			je.setComment(r.getAttributeValue(null, ATTR_ENTRY_COMMENT));
        			status = EXPECT_INTERVAL_OR_ACTION;
        			break;
        		}
        		if(status == EXPECT_INTERVAL_OR_ACTION && je != null) {
        			if(TAG_INTERVAL.equals(r.getName().getLocalPart())) {
        				Interval intr = new Interval(
        						je,
        						timeFromString(r.getAttributeValue(null, ATTR_INTERVAL_START)),
        						timeFromString(r.getAttributeValue(null, ATTR_INTERVAL_END))
        				);
        				je.addInterval(intr);
        				status = EXPECT_END_INTERVAL;
        				break;
        			}
        			if(TAG_ACTION.equals(r.getName().getLocalPart())) {
        				List<Action> actions = je.getActions();
        				actions.add(gtdModel.getAction(Integer.parseInt(r.getAttributeValue(null, ATTR_ACTION_ID))));
        				je.setActions(actions);
        				status = EXPECT_END_ACTION;
        				break;
        			}
        		}
        		throw new IllegalStateException();
        	case XMLStreamConstants.END_ELEMENT:
        		if(status == EXPECT_END_INTERVAL && TAG_INTERVAL.equals(r.getName().getLocalPart())) {
        			status = EXPECT_INTERVAL_OR_ACTION;
        			break;
        		}
        		if(status == EXPECT_END_ACTION && TAG_ACTION.equals(r.getName().getLocalPart())) {
        			status = EXPECT_INTERVAL_OR_ACTION;
        			break;
        		}
        		if(status == EXPECT_INTERVAL_OR_ACTION && TAG_ENTRY.equals(r.getName().getLocalPart())) {
        			if(je == null) {
        				throw new IllegalStateException();
        			}
        			result.add(je);
        			je.addJournalEntryListener(eventHandler);
        			eventHandler.journalEntryAdded(je);
        			status = EXPECT_ENTRY;
        			je = null;
        			break;
        		}
        		if(status == EXPECT_ENTRY && TAG_GTD_JOURNAL_DATA.equals(r.getName().getLocalPart())) {
        			status = DONE;
        			r.close();
        			fis.close();
        			return result;
        		}
        		throw new IllegalStateException();
        	default:
        		throw new IllegalStateException();
        	}
        }
		throw new IllegalStateException();
	}

	public void store() throws FileNotFoundException, XMLStreamException, FactoryConfigurationError {
		HashSet<Long> clearedDays = new HashSet<Long>();
		try {
			for(Long dirtyDay : dirtyDays) {
				store(getJournalFile(dirtyDay), data.get(dirtyDay));
				clearedDays.add(dirtyDay);
			}
		} finally {
			for (Long clearedDay: clearedDays) {
				dirtyDays.remove(clearedDay);
			}
		}
	}

	protected void store(File file, List<JournalEntry> list) throws XMLStreamException, FactoryConfigurationError, FileNotFoundException {
		if(list == null) {
			if(file.exists()) {
				file.delete();
			}
			return;
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		XMLStreamWriter w = XMLOutputFactory.newInstance().createXMLStreamWriter(fos, "UTF-8"); //$NON-NLS-1$
		
		w.writeStartDocument("UTF-8","1.0"); //$NON-NLS-1$ //$NON-NLS-2$

		w.writeCharacters(EOL);
		w.writeCharacters(EOL);
		
		w.writeStartElement(TAG_GTD_JOURNAL_DATA);
		w.writeAttribute(ATTR_VERSION, VERSION_1_0);
		w.writeAttribute(ATTR_MODIFIED, ApplicationHelper.formatLongISO(new Date()));
		w.writeCharacters(EOL);

		for(JournalEntry je: list) {
			w.writeCharacters(SKIP);
			w.writeStartElement(TAG_ENTRY);
			//w.writeAttribute(ATTR_ENTRY_ID, Integer.toString(je.getId()));
			if(je.getComment() != null) {
				w.writeAttribute(ATTR_ENTRY_COMMENT, je.getComment());
			}
			w.writeAttribute(ATTR_ENTRY_CHECKED, Boolean.toString(je.isChecked()));
			w.writeCharacters(EOL);

			for(Interval i: je.getIntervals()) {
				w.writeCharacters(SKIPSKIP);
				w.writeEmptyElement(TAG_INTERVAL);
				w.writeAttribute(ATTR_INTERVAL_START, timeToString(i.getStart()));
				w.writeAttribute(ATTR_INTERVAL_END, timeToString(i.getEnd()));
				//w.writeEndElement();
				w.writeCharacters(EOL);
			}

			if(je.getActions() != null) {
				for(Action a: je.getActions()) {
					w.writeCharacters(SKIPSKIP);
					w.writeEmptyElement(TAG_ACTION);
					w.writeAttribute(ATTR_ACTION_ID, Integer.toString(a.getId()));
					//w.writeEndElement();
					w.writeCharacters(EOL);
				}
			}

			w.writeCharacters(SKIP);
			w.writeEndElement();
			w.writeCharacters(EOL);
		}

		w.writeEndElement();
		w.writeEndDocument();
		
		w.flush();
		w.close();
	}	
	
	public static String timeToString(int time) {
		int sec = time % 60;
		time -= sec; time /= 60;
		
		int min = time % 60;
		time -= min; time /= 60;
		
		int hour = time;
		
		return String.format("%02d:%02d:%02d", hour, min, sec); //$NON-NLS-1$
	}
	
	public static int timeFromString(String str) {
		String[] hourMinSec = str.split(":"); //$NON-NLS-1$
		if(hourMinSec.length == 0 || hourMinSec.length > 3) {
			return -1;
		}
		try {
			int hour = 0;
			int min = 0;
			int sec = 0;
			if(hourMinSec.length == 3) {
				sec = Integer.parseInt(hourMinSec[2].trim());
			}
			if(hourMinSec.length >= 2) {
				min = Integer.parseInt(hourMinSec[1].trim());
			}
			if(hourMinSec.length >= 1) {
				hour = Integer.parseInt(hourMinSec[0].trim());
			}
			if(hour < 0 || min < 0 || min > 60 || sec < 0 || sec > 60) {
				return -1;
			}
			return (hour*60 + min)*60 + sec;
		} catch(NumberFormatException nfe) {
			return -1;
		}
	}
}
