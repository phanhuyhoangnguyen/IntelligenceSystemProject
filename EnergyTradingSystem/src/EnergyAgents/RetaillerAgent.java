package EnergyAgents;

import java.util.LinkedHashMap;
import java.util.Map;

import jade.core.Agent;

import database.DbHelper;

public class RetaillerAgent extends Agent {
	
	public void createTable() {
		String tableName = "Retailler_tb";
		
		Map<String, String> columns = new LinkedHashMap();
		
		
		columns.put("id", "INTEGER PRIMARY KEY AUTOINCREMENT");
		columns.put("usageCharge", "DECIMAL");	// price in cent per khw
		columns.put("overCharge", "DECIMAL");	// price in cent per khw

		columns.put("negoPrice", "DECIMAL");	// negotiation price for every iteration
		columns.put("negoMechanism", "VARCHAR(32)");	// negotiation mechanism: by time, on demand				
		columns.put("negoLimit", "DECIMAL");	// limit amount , no less than 25 cents offer
		
		//columns.put("waitingTime", "INTEGER");	// in hour
		
		// Buy from Home
		columns.put("buyFrom", "INTEGER");
		columns.put("buyAmount", "DECIMAL");
		columns.put("buyPrice", "DECIMAL");
		
		DbHelper db = new DbHelper();
		if( db.connect() ) {
			db.dropTable(tableName);
			db.createTable(tableName, columns);
		}
		
	}
	
	
	
}
