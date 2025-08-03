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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.model.Action;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDModel;

/**
 * @author ikesan
 *
 */
public class PlainTextExportAddOn implements ExportAddOn {

	private String description= "Exports GTD-Free data as plain text.";
	private ExportAddOn.ExportOrder[] orders= new ExportAddOn.ExportOrder[]{ExportAddOn.ExportOrder.FoldersActions,ExportAddOn.ExportOrder.ProjectsActions};
	private String name = "Plain Text";
	private FileFilter[] fileFilters = new FileFilter[]{new FileNameExtensionFilter("Plain text file","txt")};
	//private GTDFreeEngine engine;

	/* (non-Javadoc)
	 * @see org.gtdfree.model.ExportAddOn#export(org.gtdfree.model.GTDModel, org.gtdfree.model.ActionFilter, java.io.OutputStream, org.gtdfree.model.ExportAddOn.ExportOrder)
	 */
	@Override
	public void export(GTDModel model, ActionsCollection collection, OutputStream out,
			ExportAddOn.ExportOrder order, FileFilter ff, boolean compact) throws Exception {
		
		PrintWriter pw= new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		
		Iterator<Object> it= collection.iterator(order);
		
		pw.println();
		//          00000000011111111112222222222333333333344444444445555555555666666666677777777778
		//          12345678901234567890123456789012345678901234567890123456789012345678901234567890
		pw.println("                                GTD-FREE DATA");
		pw.println();
		pw.print("@EXPORTED: ");
		pw.println(ApplicationHelper.toISODateTimeString(new Date()));
		pw.println("@FILE ENCODING: UTF-8");
		pw.println();
		
		while (it.hasNext()) {
			Object o = it.next();
			if (o instanceof Folder) {
				Folder f= (Folder)o;
				if (f.isProject()) {
					if (order== ExportAddOn.ExportOrder.ProjectsActions) {
						pw.print("@PROJECT: ");
						pw.println(f.getName());
						pw.print("@ID: ");
						pw.println(f.getId());
						pw.println("@DESCRIPTION: ");
						pw.print("~~");
						pw.print(f.getDescription());
						pw.println("~~");
						pw.println();
					}
				} else if (order == ExportAddOn.ExportOrder.FoldersActions) {
					pw.print("@LIST: ");
					pw.println(f.getName());
					pw.print("@ID: ");
					pw.println(f.getId());
					pw.println("@DESCRIPTION: ");
					pw.print("~~");
					pw.print(f.getDescription());
					pw.println("~~");
					pw.println();
				}
			} else if (o instanceof Action) {
				Action a= (Action)o;
				pw.print("@ACTION: ");
				pw.println(a.getId());
				pw.print("@STATUS: ");
				pw.println(a.getResolution());
				pw.println("@DESCRIPTION: ");
				pw.print("~~");
				pw.print(a.getDescription());
				pw.println("~~");
				pw.println();
				
			} else if (o == ActionsCollection.ACTIONS_WITHOUT_PROJECT) {
				pw.println(o.toString());
				pw.println();
				
			}
			
		}
		
		pw.flush();
		
		
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
