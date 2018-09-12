package EnergyAgents;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


import database.DbHelper;

public class Main {

	public static void main(String[] args) {
		System.out.println("Main Class running.");
		
		// create Retailler Table for test
		RetaillerAgent retailler = new RetaillerAgent();
		retailler.createTable();
		
		// Test Database
		//testDatabase();
		
		System.out.println("Main Class finished.");
	}
	
	
	
	
	private static void testDatabase() {

		// --- Database object
		DbHelper db = new DbHelper();
		
		// --- Connect to database
		if ( db.connect()) {
			System.out.println("Connected successfully in " + db.getDbLocation());
		} else {
			System.out.println("Fail! cannot connect to database.");
			System.out.println(db.getError());
			System.exit(1);
		}
		
		// --- Create table
		String tableName = "tb_students";
		
		Map<String, String> columns = new LinkedHashMap<String, String>();
		columns.put("student_id","Integer PRIMARY KEY AUTOINCREMENT");
		columns.put("student_name", "varchar(255)");
		columns.put("score","Integer");
		
		db.dropTable( tableName );
		db.createTable( tableName, columns);
		
		// print if error
		if (db.getError() != "") {
			System.out.println(db.getError());
		}
		
		// --- Insert to table
		Map<String,Object> data = new LinkedHashMap<>();
		
		// insert 1
		data.put("student_name", "Tom");
		data.put("score", 65);
		int insertedID = db.insert(tableName, data);
		System.out.println("Insert id is " + insertedID);
		// insert 2
		data.put("student_name", "Jonh");
		data.put("score", 80);
		insertedID = db.insert(tableName, data);
		System.out.println("Insert id is " + insertedID);
		// insert 3
		data.put("student_name", "Sam");
		data.put("score", 65);
		insertedID = db.insert(tableName, data);
		System.out.println("Insert id is " + insertedID);
		// insert 4
		data.put("student_name", "Mary");
		data.put("score", 50);
		insertedID = db.insert(tableName, data);
		System.out.println("Insert id is " + insertedID);
						
		// --- Select
		String[] columnName = {"student_id", "student_name", "score"};
		
		String[] orderby = {"student_id", "score DESC"};
		
		// Query select all result
		ResultSet rows = db.select(tableName, columnName, null, orderby, null);
		// Print out
		try {
			System.out.println("Select result:");
			while (rows.next()) {
				System.out.println(rows.getString(columnName[0]) + " | " + rows.getString(columnName[1]) + " | " + rows.getString(columnName[2]) );
			}
			rows.close();
		} catch (SQLException e) {
			System.out.println("No result found.");
		}
		
		// select only result where student is 2
		Map<String, Object> wheres = new HashMap<>();
		wheres.put("student_id",2);
		
		int[] limit = {5, 0};
		
		ResultSet allrows = db.select(tableName, columnName, wheres , orderby, limit);
		// Print out
		try {
			System.out.println("Select result with student id");
			while (allrows.next()) {
				System.out.println(allrows.getString(columnName[0]) + " | " + allrows.getString(columnName[1]) + " | " + allrows.getString(columnName[2]) );
			}
			rows.close();
		} catch (SQLException e) {
			System.out.println("No result found.");
		}
		
				
				
		// --- Update
		System.out.println("Update");
		Map<String,Object> updateData = new HashMap<>();
		Map<String, Object> where = new HashMap<>();
		
		updateData.put("student_name", "Janny");
		updateData.put("score", 70);
		where.put("student_id", 2);
		
		db.update(tableName, updateData, where);
		
		
		// --- Delete
		System.out.println("Delete");
		where.put("student_id", 4);
		db.delete(tableName, where);
		
		
		
		// close
		db.close();
		
	} // end test database
	
	

}
