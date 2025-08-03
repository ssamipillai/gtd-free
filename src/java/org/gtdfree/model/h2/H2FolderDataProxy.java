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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.gtdfree.model.Action;
import org.gtdfree.model.Folder.FolderPreset;
import org.gtdfree.model.GTDData.ActionProxy;
import org.gtdfree.model.GTDData.FolderDataProxy;

/**
 * H2 database implementation of FolderDataProxy.
 * 
 * @author GitHub Copilot
 */
public class H2FolderDataProxy implements FolderDataProxy {
    
    private static final Logger logger = Logger.getLogger(H2FolderDataProxy.class);
    
    private int folderId;
    private GTDDataH2 dataStore;
    private List<ActionProxy> actions;
    private boolean suspended = false;
    
    public H2FolderDataProxy(int folderId, GTDDataH2 dataStore) {
        this.folderId = folderId;
        this.dataStore = dataStore;
        this.actions = new ArrayList<>();
        loadActions();
    }
    
    /**
     * Load actions from the database.
     */
    private void loadActions() {
        try {
            List<Action> dbActions = dataStore.loadActionsForFolder(folderId);
            actions.clear();
            for (Action action : dbActions) {
                H2ActionProxy proxy = new H2ActionProxy(action, dataStore, folderId);
                actions.add(proxy);
            }
        } catch (SQLException e) {
            logger.error("Failed to load actions for folder " + folderId, e);
        }
    }
    
    @Override
    public void store() {
        if (suspended) {
            return;
        }
        // In H2, individual actions are stored automatically
        // This method is kept for interface compatibility
    }
    
    @Override
    public void delete() {
        try {
            dataStore.deleteFolder(folderId);
        } catch (SQLException e) {
            logger.error("Failed to delete folder " + folderId, e);
        }
    }
    
    @Override
    public String getDescription() {
        // For now, folders don't have descriptions in our H2 schema
        // This could be extended if needed
        return "";
    }
    
    @Override
    public void setDescription(String desc) {
        // For now, folders don't have descriptions in our H2 schema
        // This could be extended if needed
    }
    
    @Override
    public boolean contains(ActionProxy ap) {
        return actions.contains(ap);
    }
    
    @Override
    public int size() {
        return actions.size();
    }
    
    @Override
    public ActionProxy get(int i) {
        return actions.get(i);
    }
    
    @Override
    public Iterator<ActionProxy> iterator(FolderPreset fp) {
        // For simplicity, return all actions regardless of preset
        // This could be enhanced to filter based on preset
        return actions.iterator();
    }
    
    @Override
    public void sort(Comparator<Action> comparator) {
        actions.sort((proxy1, proxy2) -> comparator.compare(proxy1.get(), proxy2.get()));
    }
    
    @Override
    public void add(int i, ActionProxy ap) {
        actions.add(i, ap);
        if (ap instanceof H2ActionProxy) {
            ((H2ActionProxy) ap).setFolderId(folderId);
        }
        ap.store();
    }
    
    @Override
    public void add(ActionProxy ap) {
        actions.add(ap);
        if (ap instanceof H2ActionProxy) {
            ((H2ActionProxy) ap).setFolderId(folderId);
        }
        ap.store();
    }
    
    @Override
    public boolean remove(int i) {
        if (i >= 0 && i < actions.size()) {
            ActionProxy removed = actions.remove(i);
            removed.delete();
            return true;
        }
        return false;
    }
    
    @Override
    public boolean remove(ActionProxy ap) {
        boolean removed = actions.remove(ap);
        if (removed) {
            ap.delete();
        }
        return removed;
    }
    
    @Override
    public void set(int i, ActionProxy actionProxy) {
        actions.set(i, actionProxy);
        if (actionProxy instanceof H2ActionProxy) {
            ((H2ActionProxy) actionProxy).setFolderId(folderId);
        }
        actionProxy.store();
    }
    
    @Override
    public ActionProxy[] toArray() {
        return actions.toArray(new ActionProxy[0]);
    }
    
    @Override
    public void clear() {
        for (ActionProxy ap : actions) {
            ap.delete();
        }
        actions.clear();
    }
    
    @Override
    public void suspend(boolean b) {
        this.suspended = b;
    }
    
    @Override
    public Date getCreated() {
        // Folders don't currently track creation date in our simple implementation
        return new Date();
    }
    
    @Override
    public Date getResolved() {
        // Folders don't have resolution dates
        return null;
    }
    
    @Override
    public Date getModified() {
        // Folders don't currently track modification date in our simple implementation
        return new Date();
    }
    
    @Override
    public void setCreated(Date d) {
        // Not implemented for folders
    }
    
    @Override
    public void setResolved(Date d) {
        // Not implemented for folders
    }
    
    @Override
    public void setModified(Date d) {
        // Not implemented for folders
    }
    
    @Override
    public void reorder(Action[] order) {
        // Reorder actions according to the provided array
        List<ActionProxy> newOrder = new ArrayList<>();
        for (Action action : order) {
            for (ActionProxy proxy : actions) {
                if (proxy.get().getId() == action.getId()) {
                    newOrder.add(proxy);
                    break;
                }
            }
        }
        this.actions = newOrder;
    }
    
    /**
     * Get the folder ID this proxy represents.
     */
    public int getFolderId() {
        return folderId;
    }
    
    /**
     * Refresh actions from the database.
     */
    public void refresh() {
        loadActions();
    }
}
