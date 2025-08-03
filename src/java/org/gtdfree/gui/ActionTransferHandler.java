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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.apache.log4j.Logger;
import org.gtdfree.model.Action;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDModel;


public abstract class ActionTransferHandler extends TransferHandler {

	private static final long serialVersionUID = 1L;

	class ActionTransfer implements Transferable {
		ActionInfo[] id;
		public ActionTransfer(Action[] i, Folder source, int[] indeces) {
			id=new ActionInfo[i.length];
			for (int j = 0; j < i.length; j++) {
				id[j]=new ActionInfo(i[j],indeces[j]);
			}
			id[0].sourceID=source.getId();
		}
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return ACTION_FLAVOR==flavor;
		}
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[]{ACTION_FLAVOR};
		}
		public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			if (flavor==ACTION_FLAVOR) {
				return id;
			}
			return null;
		}
		@Override
		public String toString() {
			return "transferable="+Arrays.toString(id); //$NON-NLS-1$
		}
	}
	
	static class ActionInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		public int actionID;
		public int folderID;
		public int sourceID;
		public int actionIndex;
		
		public ActionInfo(int fID, int aID, int aIndex) {
			actionID=aID;
			folderID=fID;
			actionIndex=aIndex;
		}
		
		public ActionInfo(Action a, int aIndex) {
			actionID=a.getId();
			folderID=a.getParent().getId();
			actionIndex=aIndex;
		}
	}
	
	public static final DataFlavor ACTION_FLAVOR= new DataFlavor(ActionInfo.class,"Action"); //$NON-NLS-1$

	
	private GTDModel model;
	
	/**
	 * @param pane
	 */
	ActionTransferHandler() {
	}
	
	public void setModel(GTDModel model) {
		this.model = model;
	}

	@Override
	public boolean importData(TransferSupport support) {
		Object drop=null;
		ActionInfo[] i=null;
		try {
			drop= support.getTransferable().getTransferData(ACTION_FLAVOR);
		} catch (UnsupportedFlavorException e) {
			Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
		} catch (IOException e) {
			Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
		}
		if (drop!=null) {
			i= (ActionInfo[])drop;
		}
		if (i!=null) {
			//System.out.println("IMPORT "+component.getClass().getName()+" "+i+" "+support.getTransferable());
			return importActions(i,support);
		}
		//System.out.println("IMPORT FALSE "+component.getClass().getName()+" "+i+" "+support.getTransferable());
		return false;
	}

	private boolean importActions(ActionInfo[] id, TransferSupport support) {
		Action[] a= new Action[id.length];
		int[] idx= new int[a.length];
		
		for (int i = 0; i < a.length; i++) {
			a[i]= model.getFolder(id[i].folderID).getActionByID(id[i].actionID);
			idx[i]= id[i].actionIndex;
		}
		return importActions(a, model.getFolder(id[0].sourceID), idx, support);
	}

	protected abstract boolean importActions(Action[] id, Folder source, int[] indexes, TransferSupport support);
	protected abstract Action[] exportActions();
	protected abstract int[] exportIndexes();
	protected abstract Folder exportSourceFolder();

	@Override
	public boolean canImport(TransferSupport support) {
		DataFlavor[] f= support.getDataFlavors();
		for (int i = 0; i < f.length; i++) {
			if (f[i]==ACTION_FLAVOR) {
				//System.out.println("CAN IMPORT TRUE "+component.getClass().getName()+" "+support.getComponent().getClass().getName()+" "+support.getTransferable());
				return true;
			}
		}
		//System.out.println("CAN IMPORT FALSE "+component.getClass().getName()+" "+support.getComponent().getClass().getName()+" "+support.getTransferable());
		return false;
	}

	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.MOVE;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		//System.out.println("CREATE TRANSFER "+c.getClass().getName()+" "+exportAction());
		return new ActionTransfer(exportActions(),exportSourceFolder(),exportIndexes());
	}
}
