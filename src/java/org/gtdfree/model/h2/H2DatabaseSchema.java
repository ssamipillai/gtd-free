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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * H2 Database schema manager for GTD-Free.
 * Handles creation and migration of the H2 database schema.
 * 
 * @author GitHub Copilot
 */
public class H2DatabaseSchema {
    
    private static final Logger logger = Logger.getLogger(H2DatabaseSchema.class);
    
    public static final String SCHEMA_VERSION = "1.0";
    
    // Table creation SQL
    private static final String CREATE_FOLDERS_TABLE = """
        CREATE TABLE IF NOT EXISTS folders (
            id INTEGER PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            type_id INTEGER NOT NULL,
            open_count INTEGER DEFAULT 0,
            closed BOOLEAN DEFAULT FALSE,
            description TEXT,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """;
    
    private static final String CREATE_ACTIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS actions (
            id INTEGER PRIMARY KEY,
            folder_id INTEGER,
            description TEXT,
            created_date TIMESTAMP NOT NULL,
            modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            resolved_date TIMESTAMP,
            start_date TIMESTAMP,
            remind_date TIMESTAMP,
            due_date TIMESTAMP,
            project_id INTEGER,
            queued BOOLEAN DEFAULT FALSE,
            resolution_id INTEGER DEFAULT 0,
            type_id INTEGER,
            priority_id INTEGER DEFAULT 0,
            url VARCHAR(2048),
            FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE
        )
        """;
    
    private static final String CREATE_SCHEMA_INFO_TABLE = """
        CREATE TABLE IF NOT EXISTS schema_info (
            version VARCHAR(50) PRIMARY KEY,
            applied_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            description TEXT
        )
        """;
    
    // Indexes for performance
    private static final String CREATE_INDEXES = """
        CREATE INDEX IF NOT EXISTS idx_actions_folder_id ON actions(folder_id);
        CREATE INDEX IF NOT EXISTS idx_actions_resolution ON actions(resolution_id);
        CREATE INDEX IF NOT EXISTS idx_actions_created ON actions(created_date);
        CREATE INDEX IF NOT EXISTS idx_actions_due ON actions(due_date);
        CREATE INDEX IF NOT EXISTS idx_actions_remind ON actions(remind_date);
        CREATE INDEX IF NOT EXISTS idx_folders_type ON folders(type_id);
        """;
    
    /**
     * Initialize or upgrade the database schema.
     * 
     * @param connection H2 database connection
     * @throws SQLException if schema creation fails
     */
    public static void initializeSchema(Connection connection) throws SQLException {
        logger.info("Initializing H2 database schema...");
        
        try (Statement stmt = connection.createStatement()) {
            // Create schema info table first
            stmt.execute(CREATE_SCHEMA_INFO_TABLE);
            
            // Check if schema already exists
            String currentVersion = getCurrentSchemaVersion(connection);
            if (currentVersion != null) {
                logger.info("Database schema version " + currentVersion + " already exists");
                return;
            }
            
            // Create main tables
            stmt.execute(CREATE_FOLDERS_TABLE);
            stmt.execute(CREATE_ACTIONS_TABLE);
            
            // Create indexes
            String[] indexes = CREATE_INDEXES.split(";");
            for (String index : indexes) {
                if (!index.trim().isEmpty()) {
                    stmt.execute(index.trim());
                }
            }
            
            // Record schema version
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO schema_info (version, description) VALUES (?, ?)")) {
                pstmt.setString(1, SCHEMA_VERSION);
                pstmt.setString(2, "Initial H2 schema for GTD-Free");
                pstmt.executeUpdate();
            }
            
            logger.info("H2 database schema initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize H2 database schema", e);
            throw e;
        }
    }
    
    /**
     * Get the current schema version from the database.
     * 
     * @param connection Database connection
     * @return Current schema version or null if not found
     */
    private static String getCurrentSchemaVersion(Connection connection) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT version FROM schema_info ORDER BY applied_date DESC LIMIT 1")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("version");
                }
            }
        } catch (SQLException e) {
            // Table might not exist yet
            logger.debug("Schema info table not found, assuming new database");
        }
        return null;
    }
    
    /**
     * Create an H2 database connection.
     * 
     * @param databaseFile The database file path
     * @return H2 connection
     * @throws SQLException if connection fails
     */
    public static Connection createConnection(File databaseFile) throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 database driver not found", e);
        }
        
        String url = "jdbc:h2:" + databaseFile.getAbsolutePath() + ";AUTO_SERVER=TRUE";
        logger.debug("Connecting to H2 database: " + url);
        
        Connection connection = DriverManager.getConnection(url, "sa", "");
        connection.setAutoCommit(true);
        
        return connection;
    }
    
    /**
     * Test database connectivity.
     * 
     * @param connection Database connection to test
     * @return true if connection is valid
     */
    public static boolean testConnection(Connection connection) {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            logger.warn("Database connection test failed", e);
            return false;
        }
    }
}
