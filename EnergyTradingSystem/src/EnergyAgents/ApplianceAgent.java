package EnergyAgents;

import jade.core.Agent;
import java.text.SimpleDateFormat;  
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import database.DbHelper;

public class ApplianceAgent extends Agent {
	private int applicantID;
	private double energyDefaultUsage;				// kWh per hour
	private double energyActualUsage;				// kWh per hour
	private boolean isOn;							// Appliance Status
	private String startTime;
	private String endTime;
	
	protected void setup(int applicantID, double energyUsage) {	// these data is taken from database and passed in manually
		this.applicantID = applicantID;				
		this.energyDefaultUsage = energyUsage;
		
		// turn on the appliance
		isOn = true;
		
		// start time
		SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");
	    Date date = new Date();
	    this.startTime = hourFormatter.format(date);
	}
	
	public double getDefaultEnergyUsage() {
		return this.energyDefaultUsage;
	}
	
	private double getActualEnergyUsage(String startTime, String endTime) {					// per hour
		// startTime and endTime can be am/pm format
		// String startTime = "9:00 AM";
		// String endTime = "10:00 AM";
		SimpleDateFormat hourFormatter = new SimpleDateFormat("h:mm a");					// always convert them to AM/PM to handle
		Date d1 = hourFormatter.parse(startTime);
		Date d2 = hourFormatter.parse(endTime);
		
		long timeDiff = d2.getTime() - d1.getTime();
		
		// calculate time different in minutes
		long minutesDiff = (timeDiff / 3600000) * 3600000 + (timeDiff % 3600000) / 60000 ;
		
		return this.energyActualUsage = minutesDiff * getDefaultEnergyUsage();
	}
	
	private void updateActualUsageToDatabase() {
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

		String tableName = "EnergyConsumption";
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
		
	    Date date = new Date();
	    SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");
	    this.endTime = hourFormatter.format(date);
	    
	    String currentDate = dateFormatter.format(date);
	    double energyActualUsage = getActualEnergyUsage(this.startTime, this.endTime);
	    
		// --- Insert to table
		Map<String,Object> data = new LinkedHashMap<>();
		
		data.put("date", currentDate);
		data.put("startTime", this.startTime);
		data.put("endTime", this.endTime);
		data.put("energyUsage", energyActualUsage);
		data.put("applicantID", this.applicantID);
		
		int insertedID = db.insert(tableName, data);
		System.out.println("Insert id is " + insertedID);
		
		// close
		db.close();
	}
	
	public String estimateElectricity() {
		return "";
	}
	
	// This is invoked by "TickerBehaviour"
	public String sendActualEnergyUsageToHomeAgent() {
		SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");
	    Date date = new Date();
	    this.endTime = hourFormatter.format(date);
	    
	    double energyActualUsage = getActualEnergyUsage(this.startTime, this.endTime);
	    
	    //TODO: send message to HomeAgent
	    
		return "";
	}
}
