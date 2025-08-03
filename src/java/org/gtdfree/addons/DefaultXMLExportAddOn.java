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

import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.FolderProjectActionFilter;
import org.gtdfree.model.GTDDataXMLTools;
import org.gtdfree.model.GTDModel;

/**
 * @author ikesan
 *
 */
public class DefaultXMLExportAddOn implements ExportAddOn {

	private final String description = Messages.getString("DefaultXMLExportAddOn.Desc"); //$NON-NLS-1$
	private final String name = "Default XML"; //$NON-NLS-1$
	private ExportAddOn.ExportOrder[] supported = new ExportAddOn.ExportOrder[] {
			ExportAddOn.ExportOrder.FormatSpecific };
	private FileFilter[] fileFilters = new FileFilter[] {
			new FileNameExtensionFilter(Messages.getString("DefaultXMLExportAddOn.FileType"), "xml") }; //$NON-NLS-1$ //$NON-NLS-2$
	// private GTDFreeEngine engine;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gtdfree.model.ExportAddOn#export(org.gtdfree.model.GTDModel,
	 * org.gtdfree.model.ActionFilter, java.io.OutputStream)
	 */
	@Override
	public void export(GTDModel model, ActionsCollection collection, OutputStream out, ExportAddOn.ExportOrder o,
			FileFilter ff, boolean compact) throws Exception {

		FolderProjectActionFilter f = new FolderProjectActionFilter(collection.getFoldersKeys(),
				collection.getProjectKeys(), collection.getFilter(), collection.isIncludeWithoutProject());

		GTDDataXMLTools.store(model, out, f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gtdfree.model.ExportAddOn#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gtdfree.model.ExportAddOn#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public final ExportAddOn.ExportOrder getDefaultExportOrder() {
		return ExportAddOn.ExportOrder.FormatSpecific;
	}

	@Override
	public final ExportAddOn.ExportOrder[] getSupportedExportOrders() {
		return supported;
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
		// this.engine=engine;
	}

}
