/*
 *    Copyright (C) 2008-2010 Igor Kriznar
 *    H2 Migration by GitHub Copilot 2025
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

package org.gtdfree.model.h2;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.gtdfree.model.Action;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDData.ActionProxy;

/**
 * H2 database implementation of ActionProxy.
 * 
 * @author GitHub Copilot
 */
public class H2ActionProxy implements ActionProxy {
    
    private static final Logger logger = Logger.getLogger(H2ActionProxy.class);
    
    private Action action;
    private GTDDataH2 dataStore;
    private int folderId;
    private Folder parent;
    
    public H2ActionProxy(Action action, GTDDataH2 dataStore, int folderId) {
        this.action = action;
        this.dataStore = dataStore;
        this.folderId = folderId;
        // Note: Action.setProxy is package-private, we'll handle this differently
    }
    
    public H2ActionProxy(Action action, GTDDataH2 dataStore) {
        this.action = action;
        this.dataStore = dataStore;
        this.folderId = -1; // Default value, will be set later
    }
    
    @Override
    public Action get() {
        return action;
    }
    
    @Override
    public void store() {
        try {
            dataStore.saveAction(action, folderId);
        } catch (SQLException e) {
            logger.error("Failed to store action: " + action.getId(), e);
        }
    }
    
    @Override
    public int getId() {
        return action.getId();
    }
    
    @Override
    public void delete() {
        try {
            dataStore.deleteAction(action.getId());
        } catch (SQLException e) {
            logger.error("Failed to delete action: " + action.getId(), e);
        }
    }
    
    @Override
    public Folder getParent() {
        return parent;
    }
    
    @Override
    public void setParent(Folder f) {
        this.parent = f;
        if (f != null) {
            this.folderId = f.getId();
        }
    }
    
    /**
     * Get the folder ID this action belongs to.
     */
    public int getFolderId() {
        return folderId;
    }
    
    /**
     * Set the folder ID this action belongs to.
     */
    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }
}
