package EnergyAgents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import jade.core.Agent;

/**
 * ApplianceEnergyProducerAgent - depricated
 * @author Phan - 101042618
 * @Description The applicant agent to generate energy
 */
public class ApplianceEnergyProducerAgent extends ApplianceAgent implements EnergyGenerator {
	
	protected void setup(String applicantID, double energyDefaultUsage, double energyDefaultProduction) {	// these data is taken from database and passed in from parameters
		setApplianceName(applicantID);
	}
	
	public String estimateElectricity() {
		return "";
	}
	
	// This is invoked by A "Behaviour"
	public String sendActualEnergyProductionToHomeAgent() {    
	    //TODO: send message to HomeAgent
		return "";
	}
}