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

import org.gtdfree.GTDFreeEngine;
import org.gtdfree.Messages;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.GTDModel;

/**
 * This is interface for add-on which provides export functionality. 
 * 
 * @author ikesan
 *
 */
public interface ExportAddOn {
	
	/**
	 * Enumeration which specifies export order or hierarchy of elements.
	 * 
	 * @author ikesan
	 *
	 */
	public static enum ExportOrder {
		FormatSpecific,Actions,FoldersActions,FoldersProjectsActions,ProjectsActions,ProjectsFoldersActions;
		
		private static final String[] names= {
			Messages.getString("ExportAddOn.0"), //$NON-NLS-1$
			Messages.getString("ExportAddOn.1"), //$NON-NLS-1$
			Messages.getString("ExportAddOn.2"), //$NON-NLS-1$
			Messages.getString("ExportAddOn.3"), //$NON-NLS-1$
			Messages.getString("ExportAddOn.4"), //$NON-NLS-1$
			Messages.getString("ExportAddOn.5") //$NON-NLS-1$
		};
		
		/**
		 * Returns name, which should be used in GUI.
		 * @return
		 */
		public String getDisplayName() {
			return names[ordinal()];
		}
		
	}

	/**
	 * Returns short name of the add-on, which will be used in add-on selection GUI component 
	 * (like choose-box).
	 * 
	 * @return short name of the add-on
	 */
	public String getName();
	
	/**
	 * 
	 * @return
	 */
	public String getDescription();

	/**
	 * Returns export order enum which should be offered by GUI to user as default or 
	 * preferred one for the add-on.
	 * @return export order enum which should be offered by GUI to user as default or preferred one 
	 */
	public ExportAddOn.ExportOrder getDefaultExportOrder();
	
	/**
	 * Returns array of enums supported by tis add-on.
	 * @return array of enums supported by tis add-on
	 */
	public ExportAddOn.ExportOrder[] getSupportedExportOrders();
	
	/**
	 * This method writes offered action to provided OutputStream.
	 * 
	 * @param model GTDModel class, which is parent owner for all folders and actions.
	 * @param collection collection of elements. It is produce iterators trough selection of actions.
	 * @param out OutputStream to which exported actions should be written.
	 * @param order suggested hierarchical order, add-on may choose to use different order or 
	 * collection may returned iterator with different order. Add-on should be able to produce 
	 * reasonable output also when actual order is not well defined.
	 * @param fileFilter if output stream is opened for a file, which was chosen by user with 
	 * particular filter, then this filer is provided to add-on. Otherwise may be null.
	 * @param compact a boolean flag giving to add-on a hint to provide more compact output.
	 * 
	 * @throws Exception if export fails for some reason
	 */
	public void export(GTDModel model, ActionsCollection collection, OutputStream out, ExportAddOn.ExportOrder order, FileFilter fileFilter, boolean compact) throws Exception;

	/**
	 * Returns add-on specific file filters, which helps choose a file to export to.
	 * 
	 * @return file filters, which helps choose a file to export to
	 */
	public FileFilter[] getFileFilters();
	
	/**
	 * Returns GUI component which can be offered to user to customize AddOn. May be null.
	 * @return GUI component which can be offered to user to customize AddOn, can be null.
	 */
	public JComponent getComponent();
	
	public void initialize(GTDFreeEngine engine);
}
