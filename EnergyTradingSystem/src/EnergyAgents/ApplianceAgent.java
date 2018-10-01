package EnergyAgents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;

import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;  
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.*;

import database.DbHelper;

public class ApplianceAgent extends Agent {
	private String applicantID;
	private boolean isOn;							// Appliance Status
	private String startTime;
	private String endTime;
	private static final int UPATE_DURATION = 10000;					// 10s -> this will be changed to be 15 minutes later
	private static final String TIME_FORMAT = "HH:mm:ss";
	private static final int LIVED_DAYS = 30;						// number of days agents have lived in the stimulation -> for data reading
	private static final int secondsInADay = 86400;					// number of seconds in a day
	private static int actualLivesSeconds;							// number of seconds agents have lived since created
	
	// testing - TODO: change this to relative path later
	private static final String CSV_FILE_PATH = "D:\\Mark Backup\\Bachelor\\3rd year\\2nd Semester\\COS30018 - Intelligent Systems\\Assignment\\Electricity_P.csv";
	
	public ApplianceAgent () {	//TODO: change this to read data from constructor 
		// setApplicantID(applicantID);
		// setEnergyDefaultUsage(energyUsage);
		
		// for testing - TODO: delete later
		this.applicantID = "fan";
		
		this.actualLivesSeconds = 0;				//TODO: check if this can be changed to startTime and endTime
		
		// start time
		SimpleDateFormat hourFormatter = new SimpleDateFormat(TIME_FORMAT);
	    Date date = new Date();
		setStartTime(hourFormatter.format(date));
	}
	
	protected void setup() {
		// TODO: Create behaviour that receives messages
        // CyclicBehaviour msgReceivingBehaviour = new msgReceivingBehaviour();
        // Waiting for received messages
        // addBehaviour(msgReceivingBehaviour);
		System.out.println("Appliance Agent is created!");
		
        TickerBehaviour communicateToHome = new TickerBehaviour(this, UPATE_DURATION) {
    
            protected void onTick() {
            	
            	sendActualUsage();
            	
            	// predictUsage();		//TODO: update this later
            }
            
            // Energy Consumption Stimulation and Send to Home Agent 
            // Agents doesn't produce the energy but read from database and send them to Home Agent
            protected void sendActualUsage() {
            	String energyConsumed = Long.toString(getActualEnergyUsage(UPATE_DURATION));
            	
            	// Send messages to home agents
    		    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    		    msg.setContent(energyConsumed);
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
            
            protected void predictUsage() {
            	// Read a certain amount of data from CSV file
				File file = new File(CSV_FILE_PATH);
				if(file.exists()) {
					// do something
				    try {
				    	// Create an object of filereader class 
				        FileReader filereader = new FileReader(CSV_FILE_PATH); 
				  
				        // create csvReader object 
				        // and skip first Line - header line
				        CSVReader csvReader = new CSVReaderBuilder(filereader) 
				                                  .withSkipLines(1) 
				                                  .build();
				        String[] nextRecord;
				        
				        // Each row is each second
				        int noOFRowsToRead = LIVED_DAYS * secondsInADay;
				        
				        for (int i = 0; i < noOFRowsToRead; i++) {
				        	nextRecord = csvReader.readNext();
				            for (String cell : nextRecord) { 
				                System.out.print(cell + "\t"); 
				            }
				            System.out.println(); 
				        }
				    } catch (Exception e) {
				    	e.printStackTrace(); 
				    }
				}
            }
        };
        
        // Sending message every 5 seconds
        addBehaviour(communicateToHome);
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
	
	// Energy Consumption Stimulation - Agent doesn't actual consume energy, it reads from data file and return
	private long getActualEnergyUsage(int timeDuration) {				// per hour
		long totalUsage = 0;
		File file = new File(CSV_FILE_PATH);
		if(file.exists()) {
			// do something
		    try {
		    	// Create an object of filereader class 
		        FileReader filereader = new FileReader(CSV_FILE_PATH); 
		  
		        // create csvReader object 
		        // and skip first Line - header line
		        CSVReader csvReader = new CSVReaderBuilder(filereader) 
		                                  .withSkipLines(1)				//TODO: this is changeable 
		                                  .build();
		        String[] nextRecord;
		        
		        // Each row is each second: second = timeDuration / 1000
		        int noOFRowsToRead = timeDuration / 1000;
		        
		        for (int i = 0; i < noOFRowsToRead; i++) {
		        	nextRecord = csvReader.readNext();
		            /*for (String cell : nextRecord) { 
		                System.out.print(cell + "\t");
		            }*/
		        	totalUsage += Long.parseLong(nextRecord[9]);		//TODO: this is also changeable
		            System.out.println(); 
		        }
		    } catch (Exception e) {
		    	e.printStackTrace(); 
		    }
		}
		
		return totalUsage;
	}
	
	public String estimateElectricity() {
		return "";
	}
}
