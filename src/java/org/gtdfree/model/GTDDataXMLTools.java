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

package org.gtdfree.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.model.Action.ActionType;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public final class GTDDataXMLTools {

	/**
	 * Modern replacement for StringEscapeUtils.unescapeJava()
	 * Handles basic Java string unescaping for the GTD-Free application.
	 */
	private static String unescapeJava(String input) {
		if (input == null) return null;
		
		StringBuilder result = new StringBuilder();
		boolean escaping = false;
		
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if (escaping) {
				switch (ch) {
					case 'n': result.append('\n'); break;
					case 't': result.append('\t'); break;
					case 'r': result.append('\r'); break;
					case 'b': result.append('\b'); break;
					case 'f': result.append('\f'); break;
					case '\'': result.append('\''); break;
					case '"': result.append('"'); break;
					case '\\': result.append('\\'); break;
					case 'u':
						// Handle unicode escape \\uXXXX
						if (i + 4 < input.length()) {
							try {
								String hex = input.substring(i + 1, i + 5);
								char unicode = (char) Integer.parseInt(hex, 16);
								result.append(unicode);
								i += 4; // Skip the 4 hex digits
							} catch (NumberFormatException e) {
								result.append(ch); // If invalid, just append the char
							}
						} else {
							result.append(ch);
						}
						break;
					default:
						result.append(ch);
				}
				escaping = false;
			} else if (ch == '\\') {
				escaping = true;
			} else {
				result.append(ch);
			}
		}
		
		return result.toString();
	}
	
	/**
	 * Modern replacement for new URL(String) constructor.
	 * Uses URI.toURL() which is the recommended approach.
	 */
	private static java.net.URL createURL(String spec) throws java.net.MalformedURLException {
		try {
			return new java.net.URI(spec).toURL();
		} catch (java.net.URISyntaxException e) {
			throw new java.net.MalformedURLException("Invalid URL: " + spec);
		}
	}

	public static class DataHeader {
		public DataHeader(File file, String ver, String mod) {
			this.file=file;
			version=ver;
			if (mod!=null) {
				try {
					modified= ApplicationHelper.parseLongISO(mod);
				} catch (ParseException e) {
					Logger.getLogger(this.getClass()).error("Parse error.", e); //$NON-NLS-1$
				}
			}
		}
		public DataHeader(File f) throws FileNotFoundException, XMLStreamException, javax.xml.stream.FactoryConfigurationError {
			file=f;
			
			InputStream in=null; 
			XMLStreamReader r=null;
			try {
				
				in=new BufferedInputStream(new FileInputStream(f));
				r = XMLInputFactory.newInstance().createXMLStreamReader(in);
				r.nextTag();
	
				if ("gtd-data".equals(r.getLocalName())) { //$NON-NLS-1$
					version=r.getAttributeValue(null, "version"); //$NON-NLS-1$
					try {
						modified=ApplicationHelper.parseLongISO(r.getAttributeValue(null, "modified")); //$NON-NLS-1$
					} catch (ParseException e) {
						Logger.getLogger(this.getClass()).error("Parse error.", e); //$NON-NLS-1$
					}
					if (modified==null) {
						modified= new Date(f.lastModified());
					}
				}
				
			} finally {
				if (r!=null) {
					try {
						r.close();
					} catch (Exception e) {
						Logger.getLogger(this.getClass()).debug("I/O error.", e); //$NON-NLS-1$
					}
					if (in!=null) {
						try {
							in.close();
						} catch (Exception e) {
							Logger.getLogger(this.getClass()).debug("I/O error.", e); //$NON-NLS-1$
						}
					}
				}
			}
	
		}
		private File file;
		String version;
		private Date modified;
		/**
		 * @return the version
		 */
		public String getVersion() {
			return version;
		}
		/**
		 * @return the modified
		 */
		public Date getModified() {
			return modified;
		}
		public File getFile() {
			return file;
		}
		@Override
		public String toString() {
			return file.toString()+" "+ApplicationHelper.toISODateTimeString(modified)+" "+version; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private final static String EOL="\n"; //$NON-NLS-1$
	private final static String SKIP="  "; //$NON-NLS-1$
	private final static String SKIPSKIP="    "; //$NON-NLS-1$

	private static void _load_1_0(GTDModel model, XMLStreamReader r) throws XMLStreamException {
		if (checkTagStart(r,"folders")) { //$NON-NLS-1$
			
			r.nextTag();
	
			while (checkTagStart(r, "folder")) { //$NON-NLS-1$
				String type= r.getAttributeValue(null, "type").trim(); //$NON-NLS-1$
				Folder ff=null;
				if ("NOTE".equals(type)) { //$NON-NLS-1$
					ff=model.getInBucketFolder();
				} else {
					ff= model.createFolder(r.getAttributeValue(null, "name"),FolderType.valueOf(type)); //$NON-NLS-1$
				}
				r.nextTag();
				
				while(checkTagStart(r, "action")) { //$NON-NLS-1$
					int i= Integer.parseInt(r.getAttributeValue(null, "id")); //$NON-NLS-1$
					Date cr= new Date(Long.parseLong(r.getAttributeValue(null, "created"))); //$NON-NLS-1$
					Date re= r.getAttributeValue(null, "resolved")==null ? null : new Date(Long.parseLong(r.getAttributeValue(null, "resolved"))); //$NON-NLS-1$ //$NON-NLS-2$
					String d =r.getAttributeValue(null, "description"); //$NON-NLS-1$
					if (d!=null) {
						d=d.replace("\\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					Action a= new Action(i,cr,re,d);
					a.setResolution(Action.Resolution.toResolution(r.getAttributeValue(null, "resolution"))); //$NON-NLS-1$
					
					String s= r.getAttributeValue(null, "start"); //$NON-NLS-1$
					if (s!=null) a.setStart(new Date(Long.parseLong(s)));
					
					s= r.getAttributeValue(null, "remind"); //$NON-NLS-1$
					if (s!=null) a.setRemind(new Date(Long.parseLong(s)));
	
					s= r.getAttributeValue(null, "due"); //$NON-NLS-1$
					if (s!=null) a.setDue(new Date(Long.parseLong(s)));
	
					s= r.getAttributeValue(null, "type"); //$NON-NLS-1$
					if (s!=null) a.setType(ActionType.valueOf(s));
					
					s= r.getAttributeValue(null, "url"); //$NON-NLS-1$
					if (s!=null) {
						try {
							a.setUrl(createURL(s));
						} catch (Exception e) {
							Logger.getLogger(GTDDataXMLTools.class).debug("Internal error.", e); //$NON-NLS-1$
						}
					}
	
	
					ff.add(a);
					if (a.getId()>model.getLastActionID()) {
						model.setLastActionID(a.getId());
					}
					findTagEnd(r,"action"); //$NON-NLS-1$
					r.nextTag();
				}	
					
				findTagEnd(r,"folder"); //$NON-NLS-1$
				r.nextTag();
			}
			
		}
	}

	private static void _load_2_0(GTDModel model, XMLStreamReader r) throws XMLStreamException {
		
		HashMap<Integer, Action> withProject= new HashMap<Integer, Action>();
		HashMap<Integer, Action> queued= new HashMap<Integer, Action>();
	
		if (checkTagStart(r,"folders")) { //$NON-NLS-1$
	
			r.nextTag();
			while (checkTagStart(r, "folder")) { //$NON-NLS-1$
				Folder ff;
				String id= r.getAttributeValue(null,"id"); //$NON-NLS-1$
				if (id!=null) {
					ff= model.createFolder(Integer.parseInt(id),r.getAttributeValue(null, "name"),FolderType.valueOf(r.getAttributeValue(null, "type"))); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					String s=r.getAttributeValue(null, "type").replace("NOTE", "INBUCKET"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ff= model.createFolder(r.getAttributeValue(null, "name"),FolderType.valueOf(s)); //$NON-NLS-1$
				}
				String s= r.getAttributeValue(null, "closed"); //$NON-NLS-1$
				if (s!=null) ff.setClosed(Boolean.parseBoolean(s));
				s =r.getAttributeValue(null, "description"); //$NON-NLS-1$
				if (s!=null) {
					s=s.replace("\\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (!ff.isInBucket()) {
					ff.setDescription(s);
				}
	
				r.nextTag();
				
				while(checkTagStart(r, "action")) { //$NON-NLS-1$
					int i= Integer.parseInt(r.getAttributeValue(null, "id")); //$NON-NLS-1$
					Date cr= new Date(Long.parseLong(r.getAttributeValue(null, "created"))); //$NON-NLS-1$
					Date re= r.getAttributeValue(null, "resolved")==null ? null : new Date(Long.parseLong(r.getAttributeValue(null, "resolved"))); //$NON-NLS-1$ //$NON-NLS-2$
					String d =r.getAttributeValue(null, "description"); //$NON-NLS-1$
					if (d!=null) {
						d=d.replace("\\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					Action a= new Action(i,cr,re,d);
					
					s= r.getAttributeValue(null, "type"); //$NON-NLS-1$
					if (s!=null) a.setType(ActionType.valueOf(s));
					
					s= r.getAttributeValue(null, "url"); //$NON-NLS-1$
					if (s!=null) {
						try {
							a.setUrl(createURL(s));
						} catch (Exception e) {
							Logger.getLogger(GTDDataXMLTools.class).debug("Internal error.", e); //$NON-NLS-1$
						}
					}
	
					s= r.getAttributeValue(null, "start"); //$NON-NLS-1$
					if (s!=null) a.setStart(new Date(Long.parseLong(s)));
					
					s= r.getAttributeValue(null, "remind"); //$NON-NLS-1$
					if (s!=null) a.setRemind(new Date(Long.parseLong(s)));
	
					s= r.getAttributeValue(null, "due"); //$NON-NLS-1$
					if (s!=null) a.setDue(new Date(Long.parseLong(s)));
	
					s= r.getAttributeValue(null, "queued"); //$NON-NLS-1$
					if (s!=null) a.setQueued(Boolean.parseBoolean(s));
	
					s= r.getAttributeValue(null, "project"); //$NON-NLS-1$
					if (s!=null) a.setProject(Integer.parseInt(s));
					
					s= r.getAttributeValue(null, "priority"); //$NON-NLS-1$
					if (s!=null) a.setPriority(Priority.valueOf(s));
	
					a.setResolution(Action.Resolution.toResolution(r.getAttributeValue(null, "resolution"))); //$NON-NLS-1$
	
					ff.add(a);
	
					if (a.getProject()!=null) {
						withProject.put(a.getId(), a);
					}
	
					if (a.isQueued()) {
						queued.put(a.getId(), a);
					}
					
					if (a.getId()>model.getLastActionID()) {
						model.setLastActionID(a.getId());
					}
					findTagEnd(r,"action"); //$NON-NLS-1$
					r.nextTag();
				}	
					
				findTagEnd(r,"folder"); //$NON-NLS-1$
				r.nextTag();
			}
			findTagEnd(r,"folders"); //$NON-NLS-1$
			//r.nextTag();
			
		}
		
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		// read projects
		r.nextTag();
	
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		
		if (checkTagStart(r,"projects")) { //$NON-NLS-1$
	
			r.nextTag();
			while (checkTagStart(r, "project")) { //$NON-NLS-1$
				Project pp;
				String id= r.getAttributeValue(null,"id"); //$NON-NLS-1$
				if (id!=null) {
					pp= (Project)model.createFolder(Integer.parseInt(id),r.getAttributeValue(null, "name"),FolderType.PROJECT); //$NON-NLS-1$
				} else {
					pp= (Project)model.createFolder(r.getAttributeValue(null, "name"),FolderType.PROJECT); //$NON-NLS-1$
				}
				pp.setClosed(Boolean.parseBoolean(r.getAttributeValue(null, "closed"))); //$NON-NLS-1$
				pp.setGoal(r.getAttributeValue(null, "goal")); //$NON-NLS-1$
				
				String s= r.getAttributeValue(null, "actions"); //$NON-NLS-1$
				
				if (s!=null && s.trim().length()>0) {
					String[] ss= s.trim().split(","); //$NON-NLS-1$
					for (int i = 0; i < ss.length; i++) {
						if (ss[i].trim().length()>0) {
							int ii= Integer.parseInt(ss[i].trim());
							Action a= withProject.remove(ii);
							if (a!=null) {
								pp.add(a);
							}
						}
					}
				}
				r.nextTag();
				findTagEnd(r,"project"); //$NON-NLS-1$
				r.nextTag();
			}
			findTagEnd(r,"projects"); //$NON-NLS-1$
		}
		
		for (Action a: withProject.values()) {
			if (a.getProject()!=null) {
				Project p= model.getProject(a.getProject());
				
				if (p!=null) {
					p.add(a);
				} else {
					System.err.println("Project "+p+" in action "+a+" does not exsist."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					a.setProject(null);
				}
			}
		}
		
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
	
		// read projects
		r.nextTag();
	
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		
		if (checkTagStart(r,"queue")) { //$NON-NLS-1$
			Folder f = model.getQueue();
	
			String s= r.getAttributeValue(null, "actions"); //$NON-NLS-1$
			
			if (s!=null && s.trim().length()>0) {
				String[] ss= s.trim().split(","); //$NON-NLS-1$
				for (int i = 0; i < ss.length; i++) {
					if (ss[i].trim().length()>0) {
						int ii= Integer.parseInt(ss[i].trim());
						Action a= queued.remove(ii);
						if (a!=null) {
							f.add(a);
						}
					}
				}
			}
			r.nextTag();
			findTagEnd(r,"queue"); //$NON-NLS-1$
			r.nextTag();
		}
	
		for (Action a: queued.values()) {
			if (a.isQueued()) {
				System.err.println("Action "+a+" is queued but not in queue list."); //$NON-NLS-1$ //$NON-NLS-2$
				model.getQueue().add(a);
			}
		}
	
	}

	private static void _load_2_1(GTDModel model, XMLStreamReader r) throws XMLStreamException  {
		
		HashMap<Integer, Action> withProject= new HashMap<Integer, Action>();
		HashMap<Integer, Action> queued= new HashMap<Integer, Action>();
	
		if (checkTagStart(r,"lists")) { //$NON-NLS-1$
	
			r.nextTag();
			while (checkTagStart(r, "list")) { //$NON-NLS-1$
				Folder ff;
				String id= r.getAttributeValue(null,"id"); //$NON-NLS-1$
				if (id!=null) {
					ff= model.createFolder(Integer.parseInt(id),r.getAttributeValue(null, "name"),FolderType.valueOf(r.getAttributeValue(null, "type"))); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					String s=r.getAttributeValue(null, "type").replace("NOTE", "INBUCKET"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ff= model.createFolder(r.getAttributeValue(null, "name"),FolderType.valueOf(s)); //$NON-NLS-1$
				}
				String s= r.getAttributeValue(null, "closed"); //$NON-NLS-1$
				if (s!=null) ff.setClosed(Boolean.parseBoolean(s));
				s =r.getAttributeValue(null, "description"); //$NON-NLS-1$
				if (s!=null) {
					s=s.replace("\\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (!ff.isInBucket()) {
					ff.setDescription(s);
				}
	
				r.nextTag();
				
				while(checkTagStart(r, "action")) { //$NON-NLS-1$
					int i= Integer.parseInt(r.getAttributeValue(null, "id")); //$NON-NLS-1$
					Date cr= new Date(Long.parseLong(r.getAttributeValue(null, "created"))); //$NON-NLS-1$
					Date re= r.getAttributeValue(null, "resolved")==null ? null : new Date(Long.parseLong(r.getAttributeValue(null, "resolved"))); //$NON-NLS-1$ //$NON-NLS-2$
					String d =r.getAttributeValue(null, "description"); //$NON-NLS-1$
					if (d!=null) {
						d=d.replace("\\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					Action a= new Action(i,cr,re,d);
					
					s= r.getAttributeValue(null, "type"); //$NON-NLS-1$
					if (s!=null) a.setType(ActionType.valueOf(s));
					
					s= r.getAttributeValue(null, "url"); //$NON-NLS-1$
					if (s!=null) {
						try {
							a.setUrl(createURL(s));
						} catch (Exception e) {
							Logger.getLogger(GTDDataXMLTools.class).debug("Internal error.", e); //$NON-NLS-1$
						}
					}
	
					s= r.getAttributeValue(null, "start"); //$NON-NLS-1$
					if (s!=null) a.setStart(new Date(Long.parseLong(s)));
					
					s= r.getAttributeValue(null, "remind"); //$NON-NLS-1$
					if (s!=null) a.setRemind(new Date(Long.parseLong(s)));
	
					s= r.getAttributeValue(null, "due"); //$NON-NLS-1$
					if (s!=null) a.setDue(new Date(Long.parseLong(s)));
	
					s= r.getAttributeValue(null, "queued"); //$NON-NLS-1$
					if (s!=null) a.setQueued(Boolean.parseBoolean(s));
	
					s= r.getAttributeValue(null, "project"); //$NON-NLS-1$
					if (s!=null) a.setProject(Integer.parseInt(s));
					
					s= r.getAttributeValue(null, "priority"); //$NON-NLS-1$
					if (s!=null) a.setPriority(Priority.valueOf(s));
	
					a.setResolution(Action.Resolution.toResolution(r.getAttributeValue(null, "resolution"))); //$NON-NLS-1$
	
					ff.add(a);
	
					if (a.getProject()!=null) {
						withProject.put(a.getId(), a);
					}
	
					if (a.isQueued()) {
						queued.put(a.getId(), a);
					}
					
					if (a.getId()>model.getLastActionID()) {
						model.setLastActionID(a.getId());
					}
					findTagEnd(r,"action"); //$NON-NLS-1$
					r.nextTag();
				}	
					
				findTagEnd(r,"list"); //$NON-NLS-1$
				r.nextTag();
			}
			
			findTagEnd(r,"lists"); //$NON-NLS-1$
			//r.nextTag();
		}
		
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		// read projects
		r.nextTag();
	
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		
		if (checkTagStart(r,"projects")) { //$NON-NLS-1$
	
			r.nextTag();
			while (checkTagStart(r, "project")) { //$NON-NLS-1$
				Project pp;
				String id= r.getAttributeValue(null,"id"); //$NON-NLS-1$
				if (id!=null) {
					pp= (Project)model.createFolder(Integer.parseInt(id),r.getAttributeValue(null, "name"),FolderType.PROJECT); //$NON-NLS-1$
				} else {
					pp= (Project)model.createFolder(r.getAttributeValue(null, "name"),FolderType.PROJECT); //$NON-NLS-1$
				}
				pp.setClosed(Boolean.parseBoolean(r.getAttributeValue(null, "closed"))); //$NON-NLS-1$
				pp.setGoal(r.getAttributeValue(null, "goal")); //$NON-NLS-1$
				
				String s= r.getAttributeValue(null, "actions"); //$NON-NLS-1$
				
				if (s!=null && s.trim().length()>0) {
					String[] ss= s.trim().split(","); //$NON-NLS-1$
					for (int i = 0; i < ss.length; i++) {
						if (ss[i].trim().length()>0) {
							int ii= Integer.parseInt(ss[i].trim());
							Action a= withProject.remove(ii);
							if (a!=null) {
								pp.add(a);
							}
						}
					}
				}
				r.nextTag();
				findTagEnd(r,"project"); //$NON-NLS-1$
				r.nextTag();
			}
			findTagEnd(r,"projects"); //$NON-NLS-1$
		}
		
		for (Action a: withProject.values()) {
			if (a.getProject()!=null) {
				Project p= model.getProject(a.getProject());
				
				if (p!=null) {
					p.add(a);
				} else {
					System.err.println("Project "+p+" in action "+a+" does not exsist."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					a.setProject(null);
				}
			}
		}
		
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
	
		// read projects
		r.nextTag();
	
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		
		if (checkTagStart(r,"queue")) { //$NON-NLS-1$
			Folder f = model.getQueue();
	
			String s= r.getAttributeValue(null, "actions"); //$NON-NLS-1$
			
			if (s!=null && s.trim().length()>0) {
				String[] ss= s.trim().split(","); //$NON-NLS-1$
				for (int i = 0; i < ss.length; i++) {
					if (ss[i].trim().length()>0) {
						int ii= Integer.parseInt(ss[i].trim());
						Action a= queued.remove(ii);
						if (a!=null) {
							f.add(a);
						}
					}
				}
			}
			r.nextTag();
			findTagEnd(r,"queue"); //$NON-NLS-1$
			r.nextTag();
		}
	
		for (Action a: queued.values()) {
			if (a.isQueued()) {
				System.err.println("Action "+a+" is queued but not in queue list."); //$NON-NLS-1$ //$NON-NLS-2$
				model.getQueue().add(a);
			}
		}
	
	}

	private static void _load_2_2(GTDModel model, XMLStreamReader r) throws XMLStreamException  {
		
		HashMap<Integer, Action> withProject= new HashMap<Integer, Action>();
		HashMap<Integer, Action> queued= new HashMap<Integer, Action>();
	
		if (checkTagStart(r,"lists")) { //$NON-NLS-1$
			
			r.nextTag();
			while (checkTagStart(r, "list")) { //$NON-NLS-1$
				Folder ff;
				String id= r.getAttributeValue(null,"id"); //$NON-NLS-1$
				if (id!=null) {
					ff= model.createFolder(Integer.parseInt(id),r.getAttributeValue(null, "name"),FolderType.valueOf(r.getAttributeValue(null, "type"))); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					String s=r.getAttributeValue(null, "type").replace("NOTE", "INBUCKET"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ff= model.createFolder(r.getAttributeValue(null, "name"),FolderType.valueOf(s)); //$NON-NLS-1$
				}
				String s= r.getAttributeValue(null, "closed"); //$NON-NLS-1$
				if (s!=null) ff.setClosed(Boolean.parseBoolean(s));
				
				s = unescapeJava(r.getAttributeValue(null, "description")); //$NON-NLS-1$
				
				if (!ff.isInBucket()) {
					ff.setDescription(s);
				}
				
				Date cr=null, mo=null, re=null;
				s= r.getAttributeValue(null, "created"); //$NON-NLS-1$
				if (s!=null) {
					cr= new Date(Long.parseLong(s));
				}
				s= r.getAttributeValue(null, "modified"); //$NON-NLS-1$
				if (s!=null) {
					mo= new Date(Long.parseLong(s));
				}
				s= r.getAttributeValue(null, "resolved"); //$NON-NLS-1$
				if (s!=null) {
					re= new Date(Long.parseLong(s));
				}
				ff.setDates(cr, mo, re);

				r.nextTag();
				
				while(checkTagStart(r, "action")) { //$NON-NLS-1$
					int i= Integer.parseInt(r.getAttributeValue(null, "id")); //$NON-NLS-1$
					cr= new Date(Long.parseLong(r.getAttributeValue(null, "created"))); //$NON-NLS-1$
					re= r.getAttributeValue(null, "resolved")==null ? null : new Date(Long.parseLong(r.getAttributeValue(null, "resolved"))); //$NON-NLS-1$ //$NON-NLS-2$
					mo= r.getAttributeValue(null, "modified")==null ? null : new Date(Long.parseLong(r.getAttributeValue(null, "modified"))); //$NON-NLS-1$ //$NON-NLS-2$
					
					String d = unescapeJava(r.getAttributeValue(null, "description")); //$NON-NLS-1$
					
					Action a= new Action(i,cr,re,d,mo);
					
					s= r.getAttributeValue(null, "type"); //$NON-NLS-1$
					if (s!=null) a.setType(ActionType.valueOf(s));
					
					s= r.getAttributeValue(null, "url"); //$NON-NLS-1$
					if (s!=null) {
						try {
							a.setUrl(createURL(s));
						} catch (Exception e) {
							Logger.getLogger(GTDDataXMLTools.class).debug("Internal error.", e); //$NON-NLS-1$
						}
					}
	
					s= r.getAttributeValue(null, "start"); //$NON-NLS-1$
					if (s!=null) a.setStart(new Date(Long.parseLong(s)));
					
					s= r.getAttributeValue(null, "remind"); //$NON-NLS-1$
					if (s!=null) a.setRemind(new Date(Long.parseLong(s)));
	
					s= r.getAttributeValue(null, "due"); //$NON-NLS-1$
					if (s!=null) a.setDue(new Date(Long.parseLong(s)));
	
					s= r.getAttributeValue(null, "queued"); //$NON-NLS-1$
					if (s!=null) a.setQueued(Boolean.parseBoolean(s));
	
					s= r.getAttributeValue(null, "project"); //$NON-NLS-1$
					if (s!=null) a.setProject(Integer.parseInt(s));
					
					s= r.getAttributeValue(null, "priority"); //$NON-NLS-1$
					if (s!=null) a.setPriority(Priority.valueOf(s));
	
					a.setResolution(Action.Resolution.toResolution(r.getAttributeValue(null, "resolution"))); //$NON-NLS-1$
	
					ff.add(a);
					
					if (a.getProject()!=null) {
						withProject.put(a.getId(), a);
					}
	
					if (a.isQueued()) {
						queued.put(a.getId(), a);
					}
					
					if (a.getId()>model.getLastActionID()) {
						model.setLastActionID(a.getId());
					}
					
					findTagEnd(r,"action"); //$NON-NLS-1$
					r.nextTag();
				}	
					
				findTagEnd(r,"list"); //$NON-NLS-1$
				r.nextTag();
			}
			findTagEnd(r,"lists"); //$NON-NLS-1$
			//r.nextTag();
		}
		
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		// read projects
		r.nextTag();
	
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		
		if (checkTagStart(r,"projects")) { //$NON-NLS-1$
	
			r.nextTag();
			while (checkTagStart(r, "project")) { //$NON-NLS-1$
				Project pp;
				String id= r.getAttributeValue(null,"id"); //$NON-NLS-1$
				if (id!=null) {
					pp= (Project)model.createFolder(Integer.parseInt(id),r.getAttributeValue(null, "name"),FolderType.PROJECT); //$NON-NLS-1$
				} else {
					pp= (Project)model.createFolder(r.getAttributeValue(null, "name"),FolderType.PROJECT); //$NON-NLS-1$
				}
				pp.setClosed(Boolean.parseBoolean(r.getAttributeValue(null, "closed"))); //$NON-NLS-1$
				pp.setGoal(r.getAttributeValue(null, "goal")); //$NON-NLS-1$
				
				String s = unescapeJava(r.getAttributeValue(null, "description")); //$NON-NLS-1$
				if (s!=null) {
					pp.setDescription(s);
				}
				
				Date cr=null, mo=null, re=null;
				s= r.getAttributeValue(null, "created"); //$NON-NLS-1$
				if (s!=null) {
					cr= new Date(Long.parseLong(s));
				}
				s= r.getAttributeValue(null, "modified"); //$NON-NLS-1$
				if (s!=null) {
					mo= new Date(Long.parseLong(s));
				}
				s= r.getAttributeValue(null, "resolved"); //$NON-NLS-1$
				if (s!=null) {
					re= new Date(Long.parseLong(s));
				}
				pp.setDates(cr, mo, re);

				s= r.getAttributeValue(null, "actions"); //$NON-NLS-1$
				
				if (s!=null && s.trim().length()>0) {
					String[] ss= s.trim().split(","); //$NON-NLS-1$
					for (int i = 0; i < ss.length; i++) {
						if (ss[i].trim().length()>0) {
							int ii= Integer.parseInt(ss[i].trim());
							Action a= withProject.remove(ii);
							if (a!=null) {
								pp.add(a);
							}
						}
					}
				}
				r.nextTag();
				findTagEnd(r,"project"); //$NON-NLS-1$
				r.nextTag();
			}
			findTagEnd(r,"projects"); //$NON-NLS-1$
		}
		
		for (Action a: withProject.values()) {
			if (a.getProject()!=null) {
				Project p= model.getProject(a.getProject());
				
				if (p!=null) {
					p.add(a);
				} else {
					System.err.println("Project "+p+" in action "+a+" does not exsist."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					a.setProject(null);
				}
			}
		}
		
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
	
		// read projects
		r.nextTag();
	
		if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
			return;
		}
		
		if (checkTagStart(r,"queue")) { //$NON-NLS-1$
			Folder f = model.getQueue();
	
			String s= r.getAttributeValue(null, "actions"); //$NON-NLS-1$
			
			if (s!=null && s.trim().length()>0) {
				String[] ss= s.trim().split(","); //$NON-NLS-1$
				for (int i = 0; i < ss.length; i++) {
					if (ss[i].trim().length()>0) {
						int ii= Integer.parseInt(ss[i].trim());
						Action a= queued.remove(ii);
						if (a!=null) {
							f.add(a);
						}
					}
				}
			}
			r.nextTag();
			findTagEnd(r,"queue"); //$NON-NLS-1$
			r.nextTag();
		}
	
		for (Action a: queued.values()) {
			if (a.isQueued()) {
				System.err.println("Action "+a+" is queued but not in queue list."); //$NON-NLS-1$ //$NON-NLS-2$
				model.getQueue().add(a);
			}
		}
	
	}

	static private boolean checkTagStart(XMLStreamReader r, String tag) throws XMLStreamException {
		return tag.equals(r.getLocalName()) && r.getEventType()==XMLStreamReader.START_ELEMENT;
	}

	private static void findTagEnd(XMLStreamReader r, String tag) throws XMLStreamException {
		while (!r.getLocalName().equals(tag) || XMLStreamReader.END_ELEMENT!=r.getEventType()) {
			if (r.getEventType()==XMLStreamReader.END_DOCUMENT) {
				return;
			}
			r.nextTag();
		}
	}

	static public void importFile(GTDModel model, InputStream in) throws XMLStreamException, FactoryConfigurationError, IOException {
		GTDModel m= new GTDModel(null);
		load(m,in);
		model.importData(m);
	}

	static public void importFile(GTDModel model, File file) throws XMLStreamException, FactoryConfigurationError, IOException {
		InputStream r= new FileInputStream(file);
		try {
			importFile(model, r);
		} finally {
			try {
				r.close();
			} catch (IOException e) {
				Logger.getLogger(GTDDataXMLTools.class).debug("I/O error.", e); //$NON-NLS-1$
			}
		}
	}

	static public void load(GTDModel model, File f) throws XMLStreamException, IOException {
		InputStream r= new FileInputStream(f);
		try {
			load(model,r);
		} finally {
			try {
				r.close();
			} catch (IOException e) {
				Logger.getLogger(GTDDataXMLTools.class).debug("I/O error.", e); //$NON-NLS-1$
			}
		}
	}

	static public DataHeader load(GTDModel model, InputStream in) throws XMLStreamException, IOException {
				
		model.setSuspendedForMultipleChanges(true);
		model.getDataRepository().suspend(true);
		
		XMLStreamReader r;
		try {
			
			// buffer size is same as default in 1.6, we explicitly request it so, not to brake if defaut changes.
			BufferedInputStream bin= new BufferedInputStream(in,8192);
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
				Logger.getLogger(GTDDataXMLTools.class).info("XML declared encoding: "+r.getEncoding()+", system default encoding: "+Charset.defaultCharset()); //$NON-NLS-1$ //$NON-NLS-2$
	        } else {
	        	//System.out.println(matcher);
	        	// if not defined, then we assume it is generated by gtd-free version 0.4 or some local editor,
	        	// so we assume system default encoding.
				r = XMLInputFactory.newInstance().createXMLStreamReader(new InputStreamReader(bin));
				Logger.getLogger(GTDDataXMLTools.class).info("XML assumed system default encoding: "+Charset.defaultCharset()); //$NON-NLS-1$
	        }
			
			r.nextTag();
			if ("gtd-data".equals(r.getLocalName())) { //$NON-NLS-1$
				DataHeader dh= new DataHeader(null,r.getAttributeValue(null, "version"),r.getAttributeValue(null, "modified")); //$NON-NLS-1$ //$NON-NLS-2$
				if (dh.version!=null) {
					if (dh.version.equals("2.0")) { //$NON-NLS-1$
						r.nextTag();
						_load_2_0(model, r);
						return dh;
					}
				}
				String s= r.getAttributeValue(null, "lastActionID"); //$NON-NLS-1$
				if (s!=null) {
					try {
						model.setLastActionID(Integer.parseInt(s));
					} catch (Exception e) {
						Logger.getLogger(GTDDataXMLTools.class).debug("Internal error.", e); //$NON-NLS-1$
					}
				}
				if (dh.version!=null) {
					if (dh.version.equals("2.1")) { //$NON-NLS-1$
						r.nextTag();
						_load_2_1(model, r);
						return dh;
						
					} 
					if (dh.version.startsWith("2.2")) { //$NON-NLS-1$
						r.nextTag();
						_load_2_2(model, r);
						return dh;
					}
				}
				throw new IOException("XML gtd-free data with version number "+dh.version+" can not be imported. Data version is newer then supported versions. Update your GTD-Free application to latest version."); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			_load_1_0(model, r);
			
			return null;
	
		} catch (XMLStreamException e) {
			if (e.getNestedException()!=null) {
				Logger.getLogger(GTDDataXMLTools.class).debug("Parse error.", e.getNestedException()); //$NON-NLS-1$
			} else {
				Logger.getLogger(GTDDataXMLTools.class).debug("Parse error.", e); //$NON-NLS-1$
			}
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			model.setSuspendedForMultipleChanges(false);
			model.getDataRepository().suspend(false);
		}
		
	}

	static public void store(GTDModel model, File f, ActionFilter filter) throws IOException, XMLStreamException, FactoryConfigurationError {
		BufferedOutputStream bw= new BufferedOutputStream(new FileOutputStream(f));
		store(model,bw,filter);
		bw.close();
	}

	static public void store(GTDModel model, File f) throws IOException, XMLStreamException, FactoryConfigurationError {
		store(model, f, new DummyFilter(true));
	}

	static public void store(GTDModel model, OutputStream out, ActionFilter filter) throws IOException, XMLStreamException, FactoryConfigurationError {
		
		if (filter==null) {
			filter= new DummyFilter(true);
		}
		XMLStreamWriter w= XMLOutputFactory.newInstance().createXMLStreamWriter(out,"UTF-8"); //$NON-NLS-1$
	
		w.writeStartDocument("UTF-8","1.0"); //$NON-NLS-1$ //$NON-NLS-2$
	
		w.writeCharacters(EOL);
		w.writeCharacters(EOL);
		
		w.writeStartElement("gtd-data"); //$NON-NLS-1$
		w.writeAttribute("version", "2.2"); //$NON-NLS-1$ //$NON-NLS-2$
		w.writeAttribute("modified", ApplicationHelper.formatLongISO(new Date())); //$NON-NLS-1$
		w.writeAttribute("lastActionID",Integer.toString(model.getLastActionID())); //$NON-NLS-1$
		w.writeCharacters(EOL);
		w.writeCharacters(EOL);
	
		// Write folders
		
		Folder[] fn= model.toFoldersArray();
		w.writeStartElement("lists"); //$NON-NLS-1$
		w.writeCharacters(EOL);
		
		for (int i = 0; i < fn.length; i++) {
			Folder ff= fn[i];
			if (ff.isMeta() || !filter.isAcceptable(ff, null)) {
				continue;
			}
			w.writeCharacters(SKIP);
			w.writeStartElement("list"); //$NON-NLS-1$
			w.writeAttribute("id", String.valueOf(ff.getId())); //$NON-NLS-1$
			w.writeAttribute("name", ff.getName()); //$NON-NLS-1$
			w.writeAttribute("type", ff.getType().toString()); //$NON-NLS-1$
			w.writeAttribute("closed", Boolean.toString(ff.isClosed())); //$NON-NLS-1$
			if (ff.getCreated()!=null) w.writeAttribute("created", Long.toString(ff.getCreated().getTime())); //$NON-NLS-1$
			if (ff.getModified()!=null) w.writeAttribute("modified", Long.toString(ff.getModified().getTime())); //$NON-NLS-1$
			if (ff.getResolved()!=null) w.writeAttribute("resolved", Long.toString(ff.getResolved().getTime())); //$NON-NLS-1$
			if (!ff.isInBucket() && ff.getDescription()!=null) {
				w.writeAttribute("description", ApplicationHelper.escapeControls(ff.getDescription())); //$NON-NLS-1$
			}
			w.writeCharacters(EOL);
			
			for (Action a : ff) {
				
				if (!filter.isAcceptable(ff,a)) {
					continue;
				}
				
				w.writeCharacters(SKIPSKIP);
				w.writeStartElement("action"); //$NON-NLS-1$
				w.writeAttribute("id", Integer.toString(a.getId())); //$NON-NLS-1$
				w.writeAttribute("created", Long.toString(a.getCreated().getTime())); //$NON-NLS-1$
				w.writeAttribute("resolution", a.getResolution().toString()); //$NON-NLS-1$
				if (a.getResolved()!=null) w.writeAttribute("resolved", Long.toString(a.getResolved().getTime())); //$NON-NLS-1$
				if (a.getModified()!=null) w.writeAttribute("modified", Long.toString(a.getModified().getTime())); //$NON-NLS-1$
				if (a.getDescription()!=null) w.writeAttribute("description", ApplicationHelper.escapeControls(a.getDescription())); //$NON-NLS-1$
				if (a.getStart()!=null) w.writeAttribute("start", Long.toString(a.getStart().getTime())); //$NON-NLS-1$
				if (a.getRemind()!=null) w.writeAttribute("remind", Long.toString(a.getRemind().getTime())); //$NON-NLS-1$
				if (a.getDue()!=null) w.writeAttribute("due", Long.toString(a.getDue().getTime())); //$NON-NLS-1$
				if (a.getType()!=null) w.writeAttribute("type", a.getType().toString()); //$NON-NLS-1$
				if (a.getUrl()!=null) w.writeAttribute("url", a.getUrl().toString()); //$NON-NLS-1$
				if (a.isQueued()) w.writeAttribute("queued", Boolean.toString(a.isQueued())); //$NON-NLS-1$
				if (a.getProject()!=null) w.writeAttribute("project", a.getProject().toString()); //$NON-NLS-1$
				if (a.getPriority()!=null) w.writeAttribute("priority", a.getPriority().toString()); //$NON-NLS-1$
				w.writeEndElement();
				w.writeCharacters(EOL);
			}
			w.writeCharacters(SKIP);
			w.writeEndElement();
			w.writeCharacters(EOL);
		}
		w.writeEndElement();
		w.writeCharacters(EOL);
	
		// Write projects
		Project[] pn= model.toProjectsArray();
		w.writeStartElement("projects"); //$NON-NLS-1$
		w.writeCharacters(EOL);
		
		for (int i = 0; i < pn.length; i++) {
			Project ff= pn[i];
			
			if (!filter.isAcceptable(ff, null)) {
				continue;
			}
			
			w.writeCharacters(SKIP);
			w.writeStartElement("project"); //$NON-NLS-1$
			w.writeAttribute("id", String.valueOf(ff.getId())); //$NON-NLS-1$
			w.writeAttribute("name", ff.getName()); //$NON-NLS-1$
			w.writeAttribute("closed", String.valueOf(ff.isClosed())); //$NON-NLS-1$
			if (ff.getCreated()!=null) w.writeAttribute("created", Long.toString(ff.getCreated().getTime())); //$NON-NLS-1$
			if (ff.getModified()!=null) w.writeAttribute("modified", Long.toString(ff.getModified().getTime())); //$NON-NLS-1$
			if (ff.getResolved()!=null) w.writeAttribute("resolved", Long.toString(ff.getResolved().getTime())); //$NON-NLS-1$
	
			if (ff.getDescription()!=null) {
				w.writeAttribute("description", ApplicationHelper.escapeControls(ff.getDescription())); //$NON-NLS-1$
			}
			
			StringBuilder sb= new StringBuilder();
			
			for (Action a : ff) {
				if (!filter.isAcceptable(ff, a)) {
					continue;
				}
				if (sb.length()>0) {
					sb.append(","); //$NON-NLS-1$
				}
				sb.append(a.getId());
			}
			w.writeAttribute("actions", sb.toString()); //$NON-NLS-1$
			w.writeEndElement();
			w.writeCharacters(EOL);
		}
		w.writeEndElement();
		w.writeCharacters(EOL);
		
		// Write queue
		Folder f= model.getQueue();
		
		if (filter.isAcceptable(f, null)) {
			w.writeStartElement("queue"); //$NON-NLS-1$
			w.writeAttribute("id", String.valueOf(f.getId())); //$NON-NLS-1$
			w.writeAttribute("name", f.getName()); //$NON-NLS-1$
			
			StringBuilder sb= new StringBuilder();
			Iterator<Action> i= f.iterator();
			if (i.hasNext()) {
				sb.append(i.next().getId());
			}
			while (i.hasNext()) {
				sb.append(","); //$NON-NLS-1$
				sb.append(i.next().getId());
			}
			w.writeAttribute("actions", sb.toString()); //$NON-NLS-1$
			w.writeEndElement();
			w.writeCharacters(EOL);
		}
		
		// containers
		w.writeEndElement();
		w.writeEndDocument();
		
		w.flush();
		w.close();
		
		//changed=false;
		
	}

}
