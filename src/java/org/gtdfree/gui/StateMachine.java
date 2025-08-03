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

import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public class StateMachine {
	
	public static enum Panel {
		COLLECTING,PROCESSING,ORGANIZING,EXECUTING,JOURNALING;
	}
	
	public static enum States {
		SHOW_ALL_ACTIONS, SHOW_RESOLVED_COLUMN, SHOW_REMIND_COLUMN, CANGE_ACTION_ORDER, DEFAULT_TREE_BRANCH, SHOW_PRIORITY_COLUMN, SHOW_PROJECT_COLUMN, SHOW_FOLDER_COLUMN
	}
	
	private boolean[][] table=
	//                     INBUCKET,ACTION,REFERENCE,BUILDIN,PROJECT,QUEUE,BUILDIN_REMIND,BUILDIN_RESOLVED,BUILDIN_PRIORITY,SOMEDAY,BUILDIN_DELETED
	/*SHOW_ALL_ACTIONS*/    {{false, false,    false,  false,  false,false,         false,            true,           false,  false,           true},
	/*SHOW_RESOLVED_COLUMN*/ {false, false,    false,  false,  false,false,         false,            true,           false,  false,           true},
	/*SHOW_REMIND_COLUMN*/   {false, false,    false,  false,  false,false,          true,           false,           false,  false,          false},
	/*CHANGE_ACTION_ORDER*/  {false,  true,     true,  false,   true, true,         false,           false,           false,   true,          false},
	/*DEFAULT_TREE_BRANCH*/  { true, false,    false,  false,  false, true,          true,            true,            true,  false,           true},
	/*SHOW_PRIORITY_COLUMN*/ {false,  true,    false,  false,   true, true,          true,           false,            true,  false,          false},
	/*SHOW_PROJECT_COLUMN*/  {false,  true,     true,  false,  false, true,          true,            true,            true,   true,           true},
	/*SHOW_FOLDER_COLUMN*/   {false, false,    false,  false,   true, true,          true,            true,            true,  false,           true},
	};
	
	public StateMachine() {
	}
	
	public boolean getPermission(Panel p) {
		return table[0][p.ordinal()];
	}
	
	public boolean getShowAllActions(FolderType f) {
		return getState(States.SHOW_ALL_ACTIONS,f);
	}

	public boolean getState(States s, FolderType f) {
		return table[s.ordinal()][f.ordinal()];
	}

	public boolean getShowResolvedColumn(FolderType type) {
		return getState(States.SHOW_RESOLVED_COLUMN,type);
	}

	public boolean getShowRemindColumn(FolderType type) {
		return getState(States.SHOW_REMIND_COLUMN,type);
	}

	public boolean getChangeActionOrder(FolderType type) {
		return getState(States.CANGE_ACTION_ORDER,type);
	}

	public boolean getDefaultTreeBranch(FolderType type) {
		return getState(States.DEFAULT_TREE_BRANCH,type);
	}

	public boolean getShowPriorityColumn(FolderType type) {
		return getState(States.SHOW_PRIORITY_COLUMN,type);
	}

	public boolean getShowProjectColumn(FolderType type) {
		return getState(States.SHOW_PROJECT_COLUMN,type);
	}

	public boolean getShowFolderColumn(FolderType type) {
		return getState(States.SHOW_FOLDER_COLUMN,type);
	}

}
