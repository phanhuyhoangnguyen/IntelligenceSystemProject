package EnergyAgents;

import jade.core.Agent;

import java.text.ParseException;
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
	
	protected void setup(int applicantID, double energyUsage) {	// these data is taken from database and passed in from parameters
		setApplicantID(applicantID);
		setEnergyDefaultUsage(energyUsage);
		
		// turn on the appliance
		setApplicantStatus(true);
		// start time
		SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");
	    Date date = new Date();
		setStartTime(hourFormatter.format(date));
	}
	
	protected void setApplicantID(int applicantID) {
		this.applicantID = applicantID;
	}
	
	public int getApplicantID() {
		return this.applicantID;
	}
	
	protected void setEnergyDefaultUsage(double energyDefaultUsage) {
		this.energyDefaultUsage = energyDefaultUsage;
	}
	
	public double getEnergyDefaultUsage() {
		return this.energyDefaultUsage;
	}
	
	protected void setApplicantStatus(boolean isOn) {
		this.isOn = isOn;
	}
	
	public boolean getApplicantStatus() {
		return this.isOn;
	}
	
	protected void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	
	public String getStartTime() {
		return this.startTime;
	}

	protected void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	
	public String getEndTime() {
		return this.endTime;
	}
	
	public double getDefaultEnergyUsage() {
		return this.energyDefaultUsage;
	}
	
	private double getActualEnergyUsage(String startTime, String endTime) {				// per hour
		// startTime and endTime can be am/pm format
		// String startTime = "9:00 AM";
		// String endTime = "10:00 AM";
		
		SimpleDateFormat hourFormatter = new SimpleDateFormat("h:mm a");			// always convert them to AM/PM to handle
        Date d1 = new Date();
		try {
			d1 = hourFormatter.parse(startTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
        Date d2 = new Date();
		try {
			d2 = hourFormatter.parse(endTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
        long timeDiff = d2.getTime() - d1.getTime();
        
		// calculate time different in minutes
        long minutesDiff = timeDiff / (60 * 1000) % 60;

		
		return this.energyActualUsage = minutesDiff * getDefaultEnergyUsage();
	}
	
	private String updateActualUsageToDatabase() {			// deprecated
		String result = "";
		// Database object
		DbHelper db = new DbHelper();
		
		// Connect to database
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
	    
		// Insert to table
		Map<String,Object> data = new LinkedHashMap<>();
		
		data.put("date", currentDate);
		data.put("startTime", this.startTime);
		data.put("endTime", this.endTime);
		data.put("energyUsage", energyActualUsage);
		data.put("applicantID", this.applicantID);
		
		int insertedID = db.insert(tableName, data);
		if (insertedID != 0) {
			System.out.println("Insert id is " + insertedID);
			result = "Insert Success!";
		}
		
		// close
		db.close();
		
		return result;
	}
	
	public String estimateElectricity() {
		return "";
	}
	
	// This is invoked by A "Behaviour"
	public String sendActualEnergyUsageToHomeAgent() {
		SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");
	    Date date = new Date();
	    this.endTime = hourFormatter.format(date);
	    
	    double energyActualUsage = getActualEnergyUsage(this.startTime, this.endTime);
	    
	    //TODO: send message to HomeAgent
	    
		return "";
	}
}
