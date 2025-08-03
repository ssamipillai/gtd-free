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

import java.awt.Font;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

/**
 * @author ikesan
 *
 */
public class StringCellEditor extends DefaultCellEditor implements TableCellEditor {
	
	private static final long serialVersionUID = 1L;

	public StringCellEditor() {
		super(new JTextField());
	}
	
	public Font getFont() {
		return ((JTextField)editorComponent).getFont();
	}

	public void setFont(Font f) {
		((JTextField)editorComponent).setFont(f);
	}
}
