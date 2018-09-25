package EnergyAgents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import database.DbHelper;
import jade.core.Agent;

public class ApplianceEnergyProducerAgent extends ApplianceAgent implements EnergyGenerator {
	private double energyDefaultProduction;
	private double energyActualProduction;
	
	protected void setup(String applicantID, double energyDefaultUsage, double energyDefaultProduction) {	// these data is taken from database and passed in from parameters
		setApplicantID(applicantID);
		setEnergyDefaultUsage(energyDefaultUsage);
		this.energyDefaultProduction = energyDefaultProduction;
		
		// turn on the appliance
		setApplicantStatus(true);
		SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");
	    Date date = new Date();
		super.setStartTime(hourFormatter.format(date));
	}
	
	public String estimateElectricity() {
		return "";
	}
	
	// This is invoked by A "Behaviour"
	public String sendActualEnergyProductionToHomeAgent() {
		SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");
	    Date date = new Date();
	    super.setEndTime(hourFormatter.format(date));
	    
	    double energyActualUsage = getActualEnergyProduction(getStartTime(), getEndTime());
	    
	    //TODO: send message to HomeAgent
	    
		return "";
	}

	@Override
	public double getActualEnergyProduction(String startTime, String endTime) {             // per hour
		// startTime and endTime can be am/pm format
		// String startTime = "9:00 AM";
		// String endTime = "10:00 AM";
		SimpleDateFormat hourFormatter = new SimpleDateFormat("h:mm a");					// always convert them to AM/PM to handle
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
		long minutesDiff = (timeDiff / 3600000) * 3600000 + (timeDiff % 3600000) / 60000 ;
		
		return this.energyActualProduction = minutesDiff * getDefaultEnergProduction();
	}

	@Override
	public double getDefaultEnergProduction() {
		return this.energyDefaultProduction;
	}

	@Override
	public String updateActualEnergyProductionToDatabase() {
		// this is deprecated from project requirements
		return null;
	}
}