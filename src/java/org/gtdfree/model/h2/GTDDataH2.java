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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.gtdfree.GlobalProperties;
import org.gtdfree.model.Action;
import org.gtdfree.model.Action.ActionType;
import org.gtdfree.model.Action.Resolution;
import org.gtdfree.model.ConsistencyException;
import org.gtdfree.model.Folder;
import org.gtdfree.model.Folder.FolderType;
import org.gtdfree.model.GTDData;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.Priority;

/**
 * H2 database implementation of GTDData interface.
 * Modern replacement for NeoDatis ODB implementation.
 * 
 * @author GitHub Copilot
 */
public class GTDDataH2 implements GTDData {
    
    private static final Logger logger = Logger.getLogger(GTDDataH2.class.getName());
    
    private Connection connection;
    private File databaseFile;
    private GlobalProperties globalProperties;
    private AtomicInteger nextActionId = new AtomicInteger(1);
    private AtomicInteger nextFolderId = new AtomicInteger(1);
    private GTDModel model;
    
    /**
     * Check if a GTDModel uses H2 database.
     */
    public static boolean isH2Model(GTDModel m) {
        return m.getDataRepository() instanceof GTDDataH2;
    }
    
    public GTDDataH2() throws SQLException {
        // Default constructor for in-memory database (testing)
        this.databaseFile = new File("gtd-free-h2");
    }
    
    public GTDDataH2(File databaseFile, GlobalProperties gp) throws SQLException {
        this.databaseFile = databaseFile;
        this.globalProperties = gp;
    }
    
    public GTDDataH2(File databaseFile) throws SQLException {
        this(databaseFile, null);
    }
    
    @Override
    public Folder newFolder(int id, String name, FolderType type) {
        try {
            // Insert folder into database
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO folders (id, name, type_id, created_date) VALUES (?, ?, ?, ?)")) {
                stmt.setInt(1, id);
                stmt.setString(2, name);
                stmt.setInt(3, type.ordinal());
                stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                stmt.executeUpdate();
            }
            
            // Update the ID counter if necessary
            if (id >= nextFolderId.get()) {
                nextFolderId.set(id + 1);
            }
            
            Folder folder = new Folder(model, id, name, type, new H2FolderDataProxy(id, this));
            logger.fine("Created folder: " + name + " (ID: " + id + ")");
            return folder;
        } catch (SQLException e) {
            logger.severe("Failed to create folder: " + name + " - " + e.getMessage());
            throw new RuntimeException("Failed to create folder", e);
        }
    }
    
    @Override
    public void store() {
        // H2 uses auto-commit by default, so explicit store is not needed
        // This method is kept for interface compatibility
        logger.fine("Store operation completed (H2 auto-commit mode)");
    }
    
    @Override
    public void initialize(File dataLoc, GlobalProperties prop) {
        try {
            this.databaseFile = new File(dataLoc, "gtd-free-h2");
            this.globalProperties = prop;
            initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize H2 database", e);
        }
    }
    
    @Override
    public GTDModel restore() throws IOException {
        try {
            this.model = new GTDModel(this);
            logger.info("Restored H2 GTD model");
            return model;
        } catch (Exception e) {
            throw new IOException("Failed to restore GTD model", e);
        }
    }
    
    @Override
    public ActionProxy newAction(int id, Date created, Date resolved, String description) {
        Action action = new Action(id, created, resolved, description);
        
        // Update the ID counter if necessary
        if (id >= nextActionId.get()) {
            nextActionId.set(id + 1);
        }
        
        return new H2ActionProxy(action, this);
    }
    
    @Override
    public ActionProxy newAction(int id, Action copy, Integer project) {
        Action action = new Action(id, copy.getCreated(), copy.getResolved(), copy.getDescription(), copy.getModified());
        
        // Copy all properties
        action.setStart(copy.getStart());
        action.setRemind(copy.getRemind());
        action.setDue(copy.getDue());
        action.setProject(project != null ? project : copy.getProject());
        action.setQueued(copy.isQueued());
        action.setResolution(copy.getResolution());
        action.setType(copy.getType());
        action.setPriority(copy.getPriority());
        action.setUrl(copy.getUrl());
        
        // Update the ID counter if necessary
        if (id >= nextActionId.get()) {
            nextActionId.set(id + 1);
        }
        
        return new H2ActionProxy(action, this);
    }
    
    @Override
    public ActionProxy getProxy(Action a) {
        // Since proxy methods are package-private, we'll use reflection or create a new approach
        // For now, create a new proxy
        H2ActionProxy proxy = new H2ActionProxy(a, this);
        // Associate it with the action later when we find a way
        return proxy;
    }
    
    @Override
    public void flush() throws IOException {
        // H2 uses auto-commit, but we can call commit explicitly for safety
        try {
            if (connection != null && !connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new IOException("Failed to flush H2 database", e);
        }
    }
    
    @Override
    public boolean close(boolean terminal) throws IOException {
        try {
            if (connection != null && !connection.isClosed()) {
                // Commit any pending transactions
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                
                connection.close();
                logger.info("H2 database closed successfully");
            }
            return true;
        } catch (SQLException e) {
            if (terminal) {
                // Force close on terminal shutdown
                logger.severe("Error closing H2 database (terminal mode): " + e.getMessage());
                return true;
            } else {
                throw new IOException("Failed to close H2 database", e);
            }
        }
    }
    
    @Override
    public boolean isClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }
    
    @Override
    public void suspend(boolean b) {
        try {
            // Enable/disable auto-commit based on suspend state
            connection.setAutoCommit(!b);
            logger.fine("H2 suspend mode: " + b + " (auto-commit: " + !b + ")");
        } catch (SQLException e) {
            logger.severe("Failed to set suspend mode: " + e.getMessage());
        }
    }
    
    @Override
    public void checkConsistency(Logger log, boolean fail, boolean correct) throws ConsistencyException {
        // Basic consistency checks for H2 database
        try {
            // Check for orphaned actions (actions without valid folders)
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM actions a WHERE a.folder_id NOT IN (SELECT id FROM folders)")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int orphanedActions = rs.getInt(1);
                        if (orphanedActions > 0) {
                            String msg = "Found " + orphanedActions + " orphaned actions";
                            log.warning(msg);
                            
                            if (correct) {
                                // Remove orphaned actions
                                try (PreparedStatement deleteStmt = connection.prepareStatement(
                                        "DELETE FROM actions WHERE folder_id NOT IN (SELECT id FROM folders)")) {
                                    int deleted = deleteStmt.executeUpdate();
                                    log.info("Removed " + deleted + " orphaned actions");
                                }
                            }
                            
                            if (fail && !correct) {
                                throw new ConsistencyException(msg);
                            }
                        }
                    }
                }
            }
            
            log.info("H2 database consistency check completed");
            
        } catch (SQLException e) {
            throw new ConsistencyException("Database error during consistency check", e);
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "H2";
    }
    
    // Private helper methods
    
    /**
     * Initialize the H2 database connection and schema.
     */
    private void initializeDatabase() throws SQLException {
        logger.info("Initializing H2 database: " + databaseFile.getAbsolutePath());
        
        this.connection = H2DatabaseSchema.createConnection(databaseFile);
        H2DatabaseSchema.initializeSchema(connection);
        
        // Initialize ID counters
        initializeIdCounters();
        
        logger.info("H2 database initialized successfully");
    }
    
    /**
     * Initialize ID counters from existing data.
     */
    private void initializeIdCounters() throws SQLException {
        // Get max action ID
        try (PreparedStatement stmt = connection.prepareStatement("SELECT MAX(id) FROM actions")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int maxId = rs.getInt(1);
                    nextActionId.set(maxId + 1);
                }
            }
        }
        
        // Get max folder ID  
        try (PreparedStatement stmt = connection.prepareStatement("SELECT MAX(id) FROM folders")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int maxId = rs.getInt(1);
                    nextFolderId.set(maxId + 1);
                }
            }
        }
        
        logger.fine("ID counters initialized - next action ID: " + nextActionId.get() + 
                    ", next folder ID: " + nextFolderId.get());
    }
    
    /**
     * Get the database connection for internal operations.
     */
    Connection getConnection() {
        return connection;
    }
    
    /**
     * Load an action from the database by ID.
     */
    Action loadAction(int actionId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM actions WHERE id = ?")) {
            stmt.setInt(1, actionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return createActionFromResultSet(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Load actions for a specific folder.
     */
    List<Action> loadActionsForFolder(int folderId) throws SQLException {
        List<Action> actions = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM actions WHERE folder_id = ? ORDER BY created_date")) {
            stmt.setInt(1, folderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    actions.add(createActionFromResultSet(rs));
                }
            }
        }
        
        return actions;
    }
    
    /**
     * Create an Action object from a database ResultSet.
     */
    private Action createActionFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Date created = rs.getTimestamp("created_date");
        Date resolved = rs.getTimestamp("resolved_date");
        String description = rs.getString("description");
        Date modified = rs.getTimestamp("modified_date");
        
        Action action = new Action(id, created, resolved, description, modified);
        
        // Set additional properties
        action.setStart(rs.getTimestamp("start_date"));
        action.setRemind(rs.getTimestamp("remind_date"));
        action.setDue(rs.getTimestamp("due_date"));
        action.setProject(rs.getObject("project_id", Integer.class));
        action.setQueued(rs.getBoolean("queued"));
        
        // Set resolution
        int resolutionId = rs.getInt("resolution_id");
        action.setResolution(Resolution.values()[resolutionId]);
        
        // Set type
        Integer typeId = rs.getObject("type_id", Integer.class);
        if (typeId != null) {
            action.setType(ActionType.values()[typeId]);
        }
        
        // Set priority
        int priorityId = rs.getInt("priority_id");
        action.setPriority(Priority.values()[priorityId]);
        
        // Set URL
        String urlString = rs.getString("url");
        if (urlString != null && !urlString.trim().isEmpty()) {
            try {
                action.setUrl(new URI(urlString).toURL());
            } catch (URISyntaxException | MalformedURLException e) {
                logger.warning("Invalid URL in action " + id + ": " + urlString + " - " + e.getMessage());
            }
        }
        
        return action;
    }
    
    /**
     * Save an action to the database.
     */
    void saveAction(Action action, int folderId) throws SQLException {
        String sql = """
            MERGE INTO actions (id, folder_id, description, created_date, modified_date, 
                               resolved_date, start_date, remind_date, due_date, project_id, 
                               queued, resolution_id, type_id, priority_id, url) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, action.getId());
            stmt.setInt(2, folderId);
            stmt.setString(3, action.getDescription());
            stmt.setTimestamp(4, new Timestamp(action.getCreated().getTime()));
            stmt.setTimestamp(5, new Timestamp(action.getModified().getTime()));
            
            // Handle nullable dates
            setTimestampOrNull(stmt, 6, action.getResolved());
            setTimestampOrNull(stmt, 7, action.getStart());
            setTimestampOrNull(stmt, 8, action.getRemind());
            setTimestampOrNull(stmt, 9, action.getDue());
            
            // Handle nullable integer
            setIntegerOrNull(stmt, 10, action.getProject());
            
            stmt.setBoolean(11, action.isQueued());
            stmt.setInt(12, action.getResolution().ordinal());
            
            // Handle nullable type
            if (action.getType() != null) {
                stmt.setInt(13, action.getType().ordinal());
            } else {
                stmt.setNull(13, java.sql.Types.INTEGER);
            }
            
            stmt.setInt(14, action.getPriority().ordinal());
            
            // Handle nullable URL
            if (action.getUrl() != null) {
                stmt.setString(15, action.getUrl().toString());
            } else {
                stmt.setNull(15, java.sql.Types.VARCHAR);
            }
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Delete an action from the database.
     */
    void deleteAction(int actionId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM actions WHERE id = ?")) {
            stmt.setInt(1, actionId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Delete a folder from the database.
     * Package-private method for internal use by H2FolderDataProxy.
     */
    void deleteFolder(int folderId) throws SQLException {
        // Delete all actions in the folder first
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM actions WHERE folder_id = ?")) {
            stmt.setInt(1, folderId);
            stmt.executeUpdate();
        }
        
        // Delete the folder
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM folders WHERE id = ?")) {
            stmt.setInt(1, folderId);
            stmt.executeUpdate();
        }
        
        logger.fine("Deleted folder ID: " + folderId);
    }
    
    /**
     * Helper method to set timestamp or null.
     */
    private void setTimestampOrNull(PreparedStatement stmt, int paramIndex, Date date) throws SQLException {
        if (date != null) {
            stmt.setTimestamp(paramIndex, new Timestamp(date.getTime()));
        } else {
            stmt.setNull(paramIndex, java.sql.Types.TIMESTAMP);
        }
    }
    
    /**
     * Helper method to set integer or null.
     */
    private void setIntegerOrNull(PreparedStatement stmt, int paramIndex, Integer value) throws SQLException {
        if (value != null) {
            stmt.setInt(paramIndex, value);
        } else {
            stmt.setNull(paramIndex, java.sql.Types.INTEGER);
        }
    }
}
