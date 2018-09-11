package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;



/**
 * Database Handler for SQLite
 * 
 * @author Tola Veng 10/09/2018
 */

public class DbHelper {

	private String dbName;
	private String dbPath;
	
	private boolean isLog = true;
	
	private Connection connection;
	private String error;

	// Initialize properties
	private void init() {
		this.dbName = "EnergySystemDB.sqlite";
		this.dbPath = System.getProperty("user.dir") + File.separator + this.dbName;
		this.connection = null;
		this.error = "";
	}
	
	/*--- Constructor ---*/
	
	public DbHelper() {
		this.init();
	}
	
	public DbHelper(String dbName) {
		this.init();
		this.dbName = dbName;
		this.dbPath = System.getProperty("user.dir") + File.separator + this.dbName;
	}
	
	
	/*--- Private ---*/
	private void log(String log) {
		if (this.isLog) {
			System.out.println("DATABASE: " + log);
		}
	}
	
	
	/**
	 * Connect to Database
	 * @param
	 * @return boolean
	 */
	public boolean connect() {
		if ( this.connection == null ) {
			try {
				Class.forName("org.sqlite.JDBC");
				this.connection = DriverManager.getConnection("jdbc:sqlite:"+ dbPath);
				return true;
			}catch(ClassNotFoundException ex) {
				this.error = ex.getMessage();
				this.log("Sqlite JAR supportted is not found.");
				ex.printStackTrace();
			}catch(SQLException ex) {
				this.error = ex.getMessage();
				this.log("Cannot connection to databae " + dbName);
				ex.printStackTrace();
			}
		}
		return false;
	}
	
	
	/**
	 * Check if connected
	 * 
	 */
	public boolean isConnected() {
		if ( this.connection != null ) {
			try {
				return !this.connection.isClosed();
			} catch (SQLException ex) {
				this.error = ex.getMessage();
				ex.printStackTrace();
			}
		}
		return false;
	}
	
	
	/**
	 * get error message
	 * @return
	 */
	public String getError() {
		return this.error;
	}
	
	
	public String getDbLocation() {
		return this.dbPath;
	}

	/**
	 * Disconnect from Database
	 * @param
	 * @return 
	 */
	public void close() {
		if ( connection != null ) {
			try {
				this.connection.close();
				this.connection = null;
			} catch (SQLException ex) {
				this.error = ex.getMessage();
				ex.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Create Table
	 * @param String TableName, Map<Name,Type> Columns
	 * @return boolean
	 */
	public boolean createTable (String tableName, Map<String, String> columns) {
		String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "`(";
		
		boolean isSeparated = false;
		for (String key: columns.keySet()) {
			if (isSeparated) {
				sql += ", ";
			}
			sql += "`" + key + "` " + columns.get(key).toUpperCase();	
			isSeparated = true;
		}
		sql += ")";
		this.log(sql);
		
		return this.executeUpdate(sql);
		
	}
	
	
	
	/**
	 * Drop Table
	 * @param TableName
	 */
	public boolean dropTable (String tableName) {
		String sql = "DROP TABLE IF EXISTS `"+ tableName + "`";
		this.log(sql);
		return this.executeUpdate(sql);		
	}
	
	/**
	 * Select from Table
	 * @param String TableName, String[] Columns, Map<String,Object> Where, String[] orderby, int[] Limit
	 * @return ResultSet or null
	 */
	public ResultSet select (String tableName, String[] columns, Map<String, Object> wheres, String[] orderby , int[] limit ) {
		ResultSet results = null;
		String sql = "SELECT ";
		for( int i=0; i<columns.length; i++) {
			if ( i>0 ) {
				sql += ", ";
			}
			sql += columns[i];
		}
		sql += " FROM `" +tableName +"`";
		
		if ( wheres != null) {
			sql += " WHERE ";
			boolean isSeparate = false;
			for( String key: wheres.keySet()) {
				if (isSeparate) { sql += ", "; }
				sql += "`" + key + "`=?";
				isSeparate = true;
			}
		}
		
		if (orderby != null && orderby.length>0) {
			sql += " ORDER BY " + orderby[0];
			for ( int i=1; i<orderby.length; i++ ) {
				sql += "," + orderby[i];
			}
		}
		
		if (limit != null && limit.length>0) {
			sql += " LIMIT " + limit[0];
			if (limit.length>1) {
				sql += " OFFSET " + limit[1];
			}
		}
		
		this.log(sql);
		
		try {
			PreparedStatement stmt = this.connection.prepareStatement(sql);
			// bind
			if ( wheres != null) {
				int index = 1;
				for (String key: wheres.keySet()) {
					stmt.setObject(index, wheres.get(key));
					index++;
				}
			}
			// execute
			results = stmt.executeQuery();
			//stmt.close();
		} catch (SQLException ex) {
			this.error = ex.getMessage();
			ex.printStackTrace();
		}
			
		return results;
	}
	
	
	
	/**
	 * Insert to Table
	 * @param String TableName, Map<String,Object> ColumnValue
	 * @return int insertedID
	 */
	public int insert(String tableName, Map<String, Object> data) {
		int insertedId = 0;
		String sql = "INSERT INTO `" +tableName +"` (";
		
		boolean isSeparated = false;
		for (String key: data.keySet()) {
			if (isSeparated) {
				sql += ", ";
			}
			sql += "`" + key + "`";	
			isSeparated = true;
		}
		
		sql += ") VALUES (";
		for (int i=0; i<data.keySet().toArray().length; i++) {
			if (i>0) {
				sql += ", ";
			}
			sql += "?";	
		}
		
		sql += ")";
		this.log(sql);
		
		// prepare statement
		try {
			PreparedStatement stmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			int index = 1;
			for (String key: data.keySet()) {
				stmt.setObject(index, data.get(key));
				index++;
			}
			if (stmt.executeUpdate() == 0) {
				this.error = "Insert failed";
				this.log(this.error);
				return 0;
			}
			ResultSet results = stmt.getGeneratedKeys();
			if ( results.next() ) {
				insertedId = results.getInt(1);
			}
			stmt.close();
		} catch (SQLException ex) {
			this.error = ex.getMessage();
			ex.printStackTrace();
		}
		
		return insertedId;
	} // end insert
	
	
	

	/**
	 * Insert to Table
	 * @param String TableName, String[] Columns, Object[] Values
	 * @return int insertedID
	 */
	public int insertArray(String tableName, String[] columns, Object[] values) {
		int insertedId = 0;
		String sql = "INSERT INTO `" +tableName +"` (";
		
		for( int i=0; i<columns.length; i++ ) {
			if ( i > 0 ) {
				sql += ", ";
			}
			sql += "`" +columns[i] +"`";
		}
		
		sql += ") VALUES (";
		for( int i=0; i<values.length; i++ ) {
			if ( i > 0 ) {
				sql += ", ";
			}
			sql += "?";
		}
		
		sql += ")";
		this.log(sql);
		
		// prepare statement
		try {
			PreparedStatement stmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			for( int i=0; i<values.length; i++ ) {
				stmt.setObject(i+1, values[i]);
			}
			if (stmt.executeUpdate() == 0) {
				this.error = "Unable to insert";
				this.log(this.error);
				return 0;
			}
			ResultSet results = stmt.getGeneratedKeys();
			if ( results.next() ) {
				insertedId = results.getInt(1);
			}
			stmt.close();
		} catch (SQLException ex) {
			this.error = ex.getMessage();
			ex.printStackTrace();
		}
		
		return insertedId;
	} // end insert array
	
	
	
	/**
	 * Update Table
	 * @param String TableName, Map<String,Object> ColumnValue, Map<String,Object> Where
	 * @return int updatedRow
	 */
	public int update(String tableName, Map<String, Object> data, Map<String, Object> wheres) {
		int updatedRow = 0;
		String sql = "UPDATE `" +tableName +"` SET ";
		
		boolean isSeparated = false;
		for (String key: data.keySet()) {
			if (isSeparated) {
				sql += ", ";
			}
			sql += "`" + key + "`=?";	
			isSeparated = true;
		}
		
		sql += " WHERE ";
		isSeparated = false;
		for (String key: wheres.keySet()) {
			if (isSeparated) {
				sql += " AND ";
			}
			sql += "`" + key + "`=?";	
			isSeparated = true;
		}
		
		this.log(sql);
		
		// prepare statement
		try {
			PreparedStatement stmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			int index = 1;
			for (String key: data.keySet()) {
				stmt.setObject(index, data.get(key));
				index++;
			}
			for (String key: wheres.keySet()) {
				stmt.setObject(index, wheres.get(key));
				index++;
			}
			updatedRow = stmt.executeUpdate();
			stmt.close();
		} catch (SQLException ex) {
			this.error = ex.getMessage();
			ex.printStackTrace();
		}
		
		return updatedRow;
	} // end update
	
	
	
	
		

	/**
	 * Delete from Table
	 * @param String TableName, Map<String,Object> Where
	 * @return int deletedRow
	 */
	public int delete(String tableName, Map<String, Object> wheres) {
		int deledRow = 0;
		String sql = "DELETE FROM `" +tableName +"` WHERE ";
		
		boolean isSeparated = false;
		for (String key: wheres.keySet()) {
			if (isSeparated) {
				sql += " AND ";
			}
			sql += "`" + key + "`=?";	
			isSeparated = true;
		}
		this.log(sql);
		
		// prepare statement
		try {
			PreparedStatement stmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			int index = 1;
			for (String key: wheres.keySet()) {
				stmt.setObject(index, wheres.get(key));
				index++;
			}
			deledRow = stmt.executeUpdate();
			stmt.close();
		} catch (SQLException ex) {
			this.error = ex.getMessage();
			ex.printStackTrace();
		}
		
		return deledRow;
		
	} // end delete
	
	
	/**
	 * Execute Insert, Update, Delete
	 * @param String SqlQuery
	 * @return Boolean
	 */
	public boolean executeUpdate(String sql) {
		if ( this.connection == null ) {
			this.error = "No connection";
			this.log(this.error);
			return false;
		}
		
		try {
			Statement stmt = null;
			stmt = this.connection.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
			return true;
		}catch(SQLException ex) {
			this.error = ex.getMessage();
			ex.printStackTrace();
		}
		return false;
	}
	
	
	/**
	 * Execute Select Query
	 * @param String SqlQuery
	 * @return ResultSet
	 */
	public ResultSet executeQuery(String sql) {
		if ( this.connection == null ) {
			this.error = "No connection";
			this.log(this.error);
			return null;
		}
		
		try {
			Statement stmt = null;
			stmt = this.connection.createStatement();
			ResultSet results = stmt.executeQuery(sql);
			stmt.close();
			return results;
		}catch(SQLException ex) {
			this.error = ex.getMessage();
			ex.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Execute all Sql
	 * @param String sqlQuery
	 * @return Boolean
	 */
	public boolean execute(String sql) {
		if ( this.connection == null ) {
			this.error = "No connection";
			this.log(this.error);
			return false;
		}
		
		try {
			Statement stmt = null;
			stmt = this.connection.createStatement();
			stmt.execute(sql);
			stmt.close();
			return true;
		}catch(SQLException ex) {
			this.error = ex.getMessage();
			ex.printStackTrace();
		}
		return false;
	}
	
	
	
	
}// end DbHelper
