package EnergyAgents;

import jade.core.Agent;
import java.text.SimpleDateFormat;  
import java.util.Date;

public class ApplianceAgent extends Agent {
	private double energyUsage;				// kWh per hour
	private boolean isOn;					// Appliance Status
	private String startTime;
	private String endTime;
	
	protected void setup(double energyUsage) {
		this.energyUsage = energyUsage;
		
		// turn on the appliance
		isOn = true;
		
		// start time
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  
	    Date date = new Date();  
	    this.startTime = formatter.format(date);  
	}
	
	public double getEnergyUsage() {
		return energyUsage;
	}
	
	private double calculateTotalEnergyUsage(String startTime, String endTime) {
		return 0.0;
	}
	
	private void updateUsageToDatabase() {
		
	}
	
	public String estimateElectricity() {
		return "";
	}
}
