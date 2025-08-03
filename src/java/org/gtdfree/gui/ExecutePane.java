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

import java.awt.BorderLayout;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Date;

import javax.swing.ActionMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable.PrintMode;
import javax.swing.border.TitledBorder;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;
import org.gtdfree.gui.ActionTable.CellAction;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.Folder;

/**
 * @author ikesan
 *
 */
public class ExecutePane extends JPanel implements WorkflowPane {
	private static final long serialVersionUID = 1L;

	private ActionPanel actionPanel;
	private ActionTable queueTable;
	private GTDFreeEngine engine;

	private JSplitPane split;

	public ExecutePane() {
		//initialize();
	}

	private void initialize() {
		setLayout(new BorderLayout());
		
		split= new JSplitPane();
		split.setOrientation(JSplitPane.VERTICAL_SPLIT);
		split.setOneTouchExpandable(true);
		
		actionPanel= new ActionPanel(false);
		actionPanel.setBorder(new TitledBorder(Messages.getString("ExecutePane.NextA"))); //$NON-NLS-1$
		
		split.setTopComponent(actionPanel);
		
		queueTable= new ActionTable();
		queueTable.setCellAction(CellAction.RESOLVE);
		queueTable.setMoveEnabled(true);
		queueTable.addPropertyChangeListener(ActionTable.SELECTED_ACTIONS_PROPERTY_NAME, new PropertyChangeListener() {
		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				actionPanel.setActions(queueTable.getSelectedActions());
				/*if (queueTable.getSelectedAction()==null && queueTable.getRowCount()>0) {
					queueTable.getSelectionModel().setSelectionInterval(0, 0);
				}*/
			}
		
		});
		
		JPanel jp= new JPanel();
		jp.setLayout(new BorderLayout());
		jp.setBorder(new TitledBorder(Messages.getString("ExecutePane.NextQ"))); //$NON-NLS-1$
		jp.add(new JScrollPane(queueTable));
		
		split.setBottomComponent(jp);
		
		ActionMap am= new ActionMap();
		
		actionPanel.addSwingActions(queueTable.getActionMap());
		queueTable.addSwingActions(actionPanel.getActionMap());

		add(split);
	}
	
	public void setEngine(GTDFreeEngine engine) {
		this.engine=engine;
		queueTable.setEngine(engine);
		queueTable.setFolder(engine.getGTDModel().getQueue());
		actionPanel.setEngine(engine);
		/*if (queueTable.getSelectedAction()==null && queueTable.getRowCount()>0) {
			queueTable.getSelectionModel().setSelectionInterval(0, 0);
		}*/
	}
	
	public GTDFreeEngine getEngine() {
		return engine;
	}

	public void store(GlobalProperties p) {
		p.putProperty("execute.dividerLocation",split.getDividerLocation()); //$NON-NLS-1$
	}

	public void restore(GlobalProperties p) {
		Integer i= p.getInteger("execute.dividerLocation"); //$NON-NLS-1$
		if (i!=null) {
			split.setDividerLocation(i);
		}
	}
	
	@Override
	public ActionsCollection getActionsInView() {
		return new ActionsCollection(queueTable);
	}
	
	public void printTable() throws PrinterException {
		queueTable.print(PrintMode.FIT_WIDTH, new MessageFormat("GTD-Free Data - Next Action Queue - "+ApplicationHelper.toISODateTimeString(new Date())), new MessageFormat("Page - {0}")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Override
	public void initialize(GTDFreeEngine engine) {
		initialize();
		setEngine(engine);
		restore(engine.getGlobalProperties());
	}
	
	@Override
	public boolean isInitialized() {
		return engine!=null;
	}
	
	@Override
	public Folder getSelectedFolder() {
		return queueTable.getFolder();
	}

}
