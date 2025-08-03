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

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.apache.log4j.Logger;

public abstract class StringTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;

	/**
	 * @param pane
	 */
	StringTransferHandler() {
	}

	@Override
	public boolean importData(TransferSupport support) {
		DataFlavor f= getStringFlavor(support.getDataFlavors());
		Object drop=null;
		String s=null;
		if (f!=null) {
			try {
				drop= support.getTransferable().getTransferData(f);
			} catch (UnsupportedFlavorException e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			} catch (IOException e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			}
		} else {
			try {
				drop= support.getTransferable().getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			} catch (IOException e) {
				Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
			}
		}
		if (drop!=null) {
			s= String.valueOf(drop);
		}
		if (s!=null) {
			return importString(s,support);
		}
		return false;
	}

	protected abstract boolean importString(String s, TransferSupport support);
	protected abstract String exportString();

	@Override
	public boolean canImport(TransferSupport support) {
		DataFlavor f= getStringFlavor(support.getDataFlavors());
		return f!=null;
	}

	private DataFlavor getStringFlavor(DataFlavor[] f) {
		for (int i = 0; i < f.length; i++) {
			if (f[i].equals(DataFlavor.stringFlavor)) {
				return f[i];
			}
		}
		return null;
	}

	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY_OR_MOVE;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		return new Transferable() {
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return DataFlavor.stringFlavor.equals(flavor);
			}
		
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[]{DataFlavor.stringFlavor};
			}
		
			@Override
			public Object getTransferData(DataFlavor flavor)
					throws UnsupportedFlavorException, IOException {
				if (flavor.equals(DataFlavor.stringFlavor)) {
					return exportString();
				}
				return null;
			}
		};
	}
}
