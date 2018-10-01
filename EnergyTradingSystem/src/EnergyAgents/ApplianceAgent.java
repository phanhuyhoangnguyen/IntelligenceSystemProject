package EnergyAgents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;  
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import database.DbHelper;

public class ApplianceAgent extends Agent {
	private String applicantID;
	private double energyDefaultUsage;				// kWh per hour
	private double energyActualUsage;				// kWh per hour
	private boolean isOn;							// Appliance Status
	private String startTime;
	private String endTime;
	private static final int UPATE_DURATION = 5000;					// 5s -> this will be changed to be 15 minutes later
	private static final String TIME_FORMAT = "HH:mm:ss";
	
	public ApplianceAgent () {	//TODO: change this to read data from constructor 
		// setApplicantID(applicantID);
		// setEnergyDefaultUsage(energyUsage);
		
		// for testing - TODO: delete later
		this.energyDefaultUsage = 0.075;
		this.applicantID = "fan"; 
		
		// turn on the appliance
		setApplicantStatus(true);
		
		// start time
		SimpleDateFormat hourFormatter = new SimpleDateFormat(TIME_FORMAT);
	    Date date = new Date();
		setStartTime(hourFormatter.format(date));
	}
	
	protected void setup() {	
		// Create behaviour that receives messages
        // CyclicBehaviour msgReceivingBehaviour = new msgReceivingBehaviour();
        // Waiting for received messages
        // addBehaviour(msgReceivingBehaviour);
        
        TickerBehaviour sendActualEnergyUsage = new TickerBehaviour(this, UPATE_DURATION) {
    
            protected void onTick() {
    			SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");
    		    Date date = new Date();
    		    String endTime = hourFormatter.format(date);
    		    setEndTime(endTime);
    		    double energyActualUsage = getActualEnergyUsage(getStartTime(), getEndTime());
    		    
    		    // Reset the start time
    		    setStartTime(endTime);
    		    
    		    // Send messages to home agents
    		    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    		    msg.setContent(Double.toString(energyActualUsage));
    		    msg.addReceiver(new AID("Home", AID.ISLOCALNAME) );
    		    
    		    // Send Message
    		    System.out.println(getLocalName() + ": Sending message " + msg.getContent() + " to ");
    		    
    		    Iterator receivers = msg.getAllIntendedReceiver();
    		    while(receivers.hasNext()) {
    		            System.out.println(((AID)receivers.next()).getLocalName());
    		    }
    		    // send message
    		    send(msg);  
            }
        };
        // Sending message every 5 seconds
        addBehaviour(sendActualEnergyUsage);
    }
	
	private class msgReceivingBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			System.out.println(getLocalName() + ": Waiting for message");

            // Retrieve message from message queue if there is
            ACLMessage msg= receive();
            if (msg!=null) {
            	// Print out message content
            	System.out.println(getLocalName()+ ": Received response " + msg.getContent() + " from " + msg.getSender().getLocalName());
           }
        
            // Block the behaviour from terminating and keep listening to the message
            block();
		}
		
	}
	
	protected void setApplicantID(String applicantID) {
		this.applicantID = applicantID;
	}
	
	public String getApplicantID() {
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
		
		SimpleDateFormat hourFormatter = new SimpleDateFormat(TIME_FORMAT);

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
		
		// this is for testing - TODO: delete later
		long secondsDiff = timeDiff / 1000;
		
		System.out.println("Start Time: " + startTime + " endTime: " + endTime + " secondsDiff: " + secondsDiff);
		
		return this.energyActualUsage = secondsDiff * getDefaultEnergyUsage();	//TODO: change this to minutesDiff later
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
}
