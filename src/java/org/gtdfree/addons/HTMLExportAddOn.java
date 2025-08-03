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

package org.gtdfree.addons;

import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.html.HTMLStreamWriter;
import org.gtdfree.html.Structure;
import org.gtdfree.html.TAG;
import org.gtdfree.model.Action;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.Priority;

/**
 * @author ikesan
 *
 */
public class HTMLExportAddOn implements ExportAddOn {

	private static final String NONE_DOT = "No actions selected or defined.";
	private static final String NONE = "None";
	private static final String REMINDER = "Reminder:"+" ";
	private static final String PRIORITY = "Priority:"+" ";
	private static final String PROJECT = "Project:"+" ";
	private static final String CREATED = "Created:"+" ";
	private static final String ID = "ID:"+" ";
	private static final String CHECK_OPEN= "OPEN \u2610";
	private static final String CHECK_RESOLVED= "RESOLVED \u2611";
	private static final String CHECK_DELETED= "DELETED \u2612";
	private static final String CHECK_STALLED= "STALLED \u2205";
	private static final String FULL_STAR= "\u2605";
	private static final String HOLLOW_STAR= "\u2606";
	
	private static final Structure STAR_NONE= Structure.with(HOLLOW_STAR);
	private static final Structure STAR_LOW= Structure.withStart(TAG.SPAN.withClass("low")).add(FULL_STAR).addEnd();
	private static final Structure STAR_MEDIUM= Structure.withStart(TAG.SPAN.withClass("medium")).add(FULL_STAR).addEnd();
	private static final Structure STAR_HIGH= Structure.withStart(TAG.SPAN.withClass("high")).add(FULL_STAR).addEnd();
	
	private static final Structure PRIORITY_NONE= Structure.withStart(TAG.SPAN.withClass("stars")).add(" ").add(STAR_NONE).add(STAR_NONE).add(STAR_NONE).addEnd();
	private static final Structure PRIORITY_LOW= Structure.withStart(TAG.SPAN.withClass("stars")).add(" ").add(STAR_LOW).add(STAR_NONE).add(STAR_NONE).addEnd();
	private static final Structure PRIORITY_MEDIUM= Structure.withStart(TAG.SPAN.withClass("stars")).add(" ").add(STAR_LOW).add(STAR_MEDIUM).add(STAR_NONE).addEnd();
	private static final Structure PRIORITY_HIGH= Structure.withStart(TAG.SPAN.withClass("stars")).add(" ").add(STAR_LOW).add(STAR_MEDIUM).add(STAR_HIGH).addEnd();
	
	public static void main(String[] args) {
		
		System.out.println(STAR_NONE);
		System.out.println(STAR_LOW);
		System.out.println(STAR_MEDIUM);
		System.out.println(STAR_HIGH);
		System.out.println(PRIORITY_NONE);
		System.out.println(PRIORITY_MEDIUM);
		System.out.println(PRIORITY_HIGH);
	}
	
	private String description= "Exports GTD-Free data as HTML doument.";
	private ExportAddOn.ExportOrder[] orders= new ExportAddOn.ExportOrder[]{ExportAddOn.ExportOrder.FoldersProjectsActions,ExportAddOn.ExportOrder.FoldersActions,ExportAddOn.ExportOrder.ProjectsActions,ExportAddOn.ExportOrder.ProjectsFoldersActions,ExportAddOn.ExportOrder.Actions};
	private String name = "HTML";
	private FileFilter[] fileFilters = new FileFilter[]{new FileNameExtensionFilter("HTML documents","html")};
	//private GTDFreeEngine engine;

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#export(org.gtdfree.model.GTDModel, org.gtdfree.model.ActionFilter, java.io.OutputStream, org.gtdfree.model.ExportAddOn.ExportOrder)
	 */
	@Override
	public void export(GTDModel model, ActionsCollection collection, OutputStream out,
			ExportAddOn.ExportOrder order, FileFilter ff, boolean compact) throws Exception {
		
		boolean emptyH2=false;
		boolean emptyH3=false;
		
		HTMLStreamWriter w= HTMLStreamWriter.newInstance(out);
		
		w.startHtmlDocument("GTD-Free Data");
		
		w.startTag(TAG.STYLE);
		w.writenl();
		w.writeStyleRule("BODY","font-family: sans-serif; font-size: small");
		w.writeStyleRule("H1","text-align: center; margin-bottom: +1em");
		w.writeStyleRule("H2","border-bottom: solid");
		w.writeStyleRule("H3","border-bottom: solid 1px");
		w.writeStyleRule("DIV.head TABLE","margin-left: auto; margin-right: auto"); 
		w.writeStyleRule("DIV.head","text-align: center");
		w.writeStyleRule("TABLE","border-collapse: collapse; page-break-inside: avoid");
		w.writeStyleRule("TABLE.summary","margin-left: 1.27em; margin-right: 1.27em");
		w.writeStyleRule("TD","border: 1px solid black; padding: 0.3em");
		w.writeStyleRule("TD.check-open","text-align: right; color: #0000ff");
		w.writeStyleRule("TD.check-del","text-align: right; color: #808080");
		w.writeStyleRule("TD.check-res","text-align: right; color: #008000");
		w.writeStyleRule("TD.check-stall","text-align: right");
		w.writeStyleRule("TD.desc","padding: 0.75em");
		w.writeStyleRule("TD.strong","font-weight: bold");
		w.writeStyleRule("DIV.action","margin-top: 1em");
		w.writeStyleRule("DIV.action * TD","width: 33%");
		w.writeStyleRule("*.end","text-align: center; font-size: smaller");
		w.writeStyleRule("*.low","color: #fff001");
		w.writeStyleRule("*.medium","color: #ffc13b");
		w.writeStyleRule("*.high","color: #ff2b01");
		w.writeStyleRule("*.stars","font-size: larger");
		
		w.endTag();
		
		w.startHtmlBody();
		
		Iterator<Object> it= collection.iterator(order);

		w.startTag(TAG.DIV.withClass("head"));
		w.writeTag(TAG.H1,"GTD-Free Data");
		w.writenl();
		
		/*w.startTag(TAG.TABLE);
		w.writeTableRow("Exported",ApplicationHelper.toISODateTimeString(new Date()));
		w.endTag();*/

		w.writeHr();
		
		w.endTag();

		
		while (it.hasNext()) {
			Object o = it.next();
			
			if (o == ActionsCollection.ACTIONS_WITHOUT_PROJECT) {
				
				if (order==ExportAddOn.ExportOrder.FoldersProjectsActions) {
					w.writeTag(TAG.H3,ActionsCollection.ACTIONS_WITHOUT_PROJECT);
					emptyH2=false;
					emptyH3=true;
				} else {
					w.writeTag(TAG.H2,ActionsCollection.ACTIONS_WITHOUT_PROJECT);
					emptyH2=true;
					emptyH3=false;
				}

				continue;
			}
			
			if (o instanceof Folder) {
				
				Folder f= (Folder)o;

				if (f.isProject()) {
					
					if ( order== ExportAddOn.ExportOrder.ProjectsActions 
							|| order== ExportAddOn.ExportOrder.ProjectsFoldersActions) {

						if (emptyH2 || emptyH3) {
							w.writeTag(TAG.P, NONE_DOT);
						}

						w.writeTag(TAG.H2,f.getName());
						w.startTag(TAG.TABLE.withClass("summary"));
						
						w.writeTableRow(new String[]{"ID",String.valueOf(f.getId())}, new TAG[]{TAG.TD,TAG.TD.withClass("strong")});
						w.writeTableRow(new String[]{"Type","Project"}, new TAG[]{TAG.TD,TAG.TD.withClass("strong")});
						w.writeTableRow(new String[]{"Open",String.valueOf(f.getOpenCount())}, new TAG[]{TAG.TD,TAG.TD.withClass("strong")});
						w.writeTableRow(new String[]{"All",String.valueOf(f.size())}, new TAG[]{TAG.TD,TAG.TD.withClass("strong")});

						w.startTag(TAG.TR);
						w.startTag(TAG.TD);
						w.writeCharacters("Description");
						w.writeEndElement();
						w.startTag(TAG.TD.withClass("desc"));
						w.writeMultiline(f.getDescription());
						w.writeEndElement();
						w.endTag();

						w.endTag();
						
						emptyH2=true;
						emptyH3=false;

					} else {
						
						if (emptyH3) {
							w.writeTag(TAG.P, NONE_DOT);
						}

						w.writeTag(TAG.H3,"Project:"+" "+f.getName());
						
						emptyH2=false;
						emptyH3=true;
					}
					
					continue;
 
				}
				if (order == ExportAddOn.ExportOrder.FoldersActions || order == ExportAddOn.ExportOrder.FoldersProjectsActions) {
			
					if (emptyH2 || emptyH3) {
						w.writeTag(TAG.P, NONE_DOT);
					}

					w.writeTag(TAG.H2,f.getName());
					w.startTag(TAG.TABLE.withClass("summary"));
					w.writeTableRow(new String[]{"ID",String.valueOf(f.getId())}, new TAG[]{TAG.TD,TAG.TD.withClass("strong")});
					
					String type="";
					if (f.isAction()) {
						type="Action list";
					} else if (f.isInBucket()) {
						type="In-Bucket";
					} else if (f.isQueue()) {
						type="Next action queue";
					} else if (f.isReference()) {
						type="Reference list";
					} else if (f.isSomeday()) {
						type="Someday/Maybe list";
					} else if (f.isBuildIn()) {
						type="Default list";
					}

					w.writeTableRow(new String[]{"Type",type}, new TAG[]{TAG.TD,TAG.TD.withClass("strong")});
					w.writeTableRow(new String[]{"Open",String.valueOf(f.getOpenCount())}, new TAG[]{TAG.TD,TAG.TD.withClass("strong")});
					w.writeTableRow(new String[]{"All",String.valueOf(f.size())}, new TAG[]{TAG.TD,TAG.TD.withClass("strong")});

					w.startTag(TAG.TR);
					w.startTag(TAG.TD);
					w.writeCharacters("Description");
					w.writeEndElement();
					w.startTag(TAG.TD.withClass("desc"));
					w.writeMultiline(f.getDescription());
					w.writeEndElement();
					w.endTag();

					w.endTag();

					emptyH2=true;
					emptyH3=false;

				} else {
					
					if (emptyH3) {
						w.writeTag(TAG.P, NONE_DOT);
					}

					w.writeTag(TAG.H3,"Folder:"+" "+f.getName());
					
					emptyH2=false;
					emptyH3=true;

				}

				continue;
				
			}
			
			if (o instanceof Action) {
				emptyH2=false;
				emptyH3=false;
				
				Action a= (Action)o;

				w.startTag(TAG.DIV.withClass("action"));
				w.writenl();
				w.startTag(TAG.TABLE);
				
				String check=null;
				String tagClass=null;
				if (a.isOpen()) {
					check= CHECK_OPEN;
					tagClass="check-open";
				} else if (a.isResolved()) {
					check= CHECK_RESOLVED;
					tagClass="check-res";
				} else if (a.isDeleted()) {
					check= CHECK_DELETED;
					tagClass="check-del";
				} else {
					check= CHECK_STALLED;
					tagClass="check-stall";
				}
				
				w.startTag(TAG.TR);
				w.startTag(TAG.TD);
				w.writeCharacters(ID);
				w.startTag(TAG.STRONG);
				w.writeCharacters(String.valueOf(a.getId()));
				w.writeEndElement();
				w.writeEndElement();
				w.startTag(TAG.TD);
				w.writeCharacters(CREATED);
				w.startTag(TAG.STRONG);
				w.writeCharacters(ApplicationHelper.toISODateTimeString(a.getCreated()));
				w.writeEndElement();
				w.writeEndElement();
				w.startTag(TAG.TD.withClass(tagClass));
				w.startTag(TAG.STRONG);
				w.writeCharacters(check);
				w.writeEndElement();
				w.writeEndElement();
				w.endTag();
				
				String project;
				if (a.getProject()!=null) {
					project= model.getProject(a.getProject()).getName();
				} else {
					project= NONE;
				}
				
				w.startTag(TAG.TR);
				w.startTag(TAG.TD);
				w.writeCharacters(PRIORITY);
				w.startTag(TAG.STRONG);
				w.writeCharacters(a.getPriority()==null ? NONE : a.getPriority().toString());
				w.writeEndElement();
				
				if (a.getPriority()==null || a.getPriority()==Priority.None) {
					w.writeStructure(PRIORITY_NONE);
				} else if (a.getPriority()==Priority.Low) {
					w.writeStructure(PRIORITY_LOW);
				} else if (a.getPriority()==Priority.Medium) {
					w.writeStructure(PRIORITY_MEDIUM);
				} else if (a.getPriority()==Priority.High) {
					w.writeStructure(PRIORITY_HIGH);
				}
				
				w.writeEndElement();
				w.startTag(TAG.TD);
				w.writeCharacters(REMINDER);
				w.startTag(TAG.STRONG);
				w.writeCharacters(a.getRemind()!=null ? ApplicationHelper.toISODateString(a.getRemind()) : NONE);
				w.writeEndElement();
				w.writeEndElement();
				w.startTag(TAG.TD);
				w.writeCharacters(PROJECT);
				w.startTag(TAG.STRONG);
				w.writeCharacters(project);
				w.writeEndElement();
				w.writeEndElement();
				w.endTag();
				
				w.startTag(TAG.TR);
				w.startTag(TAG.TD.withCOLSPAN(3).withClass("desc"));
				w.writeMultiline(a.getDescription());
				w.writeEndElement();
				w.endTag();
				
				if (a.getUrl()!=null) {
					w.startTag(TAG.TR);
					w.startTag(TAG.TD.withCOLSPAN(3));
					
					w.startTag(TAG.A.withHREF(a.getUrl()));
					w.writeCharacters(a.getUrl().toString());
					w.writeEndElement();
					
					w.writeEndElement();
					w.endTag();
				}

				w.endTag();
				w.endTag();
			}
			
		}
		
		if (emptyH2 || emptyH3) {
			w.writeTag(TAG.P, NONE_DOT);
		}
		
		w.writeHr();

		w.startTag(TAG.P.withClass("end"));
		w.writeCharacters("Exported:"+" "+ApplicationHelper.toISODateTimeString(new Date()));
		w.endTag();

		w.endHtmlDocument();
		w.flush();
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#getDefaultExportOrder()
	 */
	@Override
	public ExportAddOn.ExportOrder getDefaultExportOrder() {
		return ExportAddOn.ExportOrder.FoldersProjectsActions;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#getSupportedExportOrders()
	 */
	@Override
	public ExportAddOn.ExportOrder[] getSupportedExportOrders() {
		return orders;
	}
	
	@Override
	public FileFilter[] getFileFilters() {
		return fileFilters;
	}

	@Override
	public JComponent getComponent() {
		return null;
	}
	
	@Override
	public void initialize(GTDFreeEngine engine) {
		//this.engine=engine;
	}
}
