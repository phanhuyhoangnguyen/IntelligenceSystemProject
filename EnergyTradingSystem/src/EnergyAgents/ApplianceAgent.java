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
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;  
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.*;

import database.DbHelper;

/**
 * ApplicantEnum 
 * @author Phan
 * 
 * @Description The applicant agent compute its energy usage and send it to the HomeAgent periodically
 */
public class ApplianceAgent extends Agent {
	private String applicantName;
	private boolean isOn;											// Appliance Status
	private String startTime;
	private String endTime;
	private static final int UPATE_DURATION = 10000;				// 10s -> specify the frequency of message sent to Home Agent. 
																	// Ideally, this should be equal to USAGE_DURATION. However, waiting 30 mins to see message sent is too long
	private static final String TIME_FORMAT = "HH:mm:ss";
	private static final int LIVED_DAYS = 30;						// number of days agents have lived in the stimulation -> for data reading
	private static final int secondsInADay = 86400;					// number of seconds in a day
	private static int actualLivedSeconds;							// number of seconds agents have lived since created
	private Map <String, Integer> applicantDict;					// hold agent name and its index for searching its usage in data file
	private static final int USAGE_DURATION = 1800000;				// 30 mins -> specify the total usage of agent in a period of time, 30 mins. 
																		
	
	public ApplianceAgent () {
		
		// this is set for skipping the first row in CSV file below
		this.actualLivedSeconds = 1000;				//TODO: check if this should be changed to startTime and endTime
		
		// start time
		SimpleDateFormat hourFormatter = new SimpleDateFormat(TIME_FORMAT);
	    Date date = new Date();
		setStartTime(hourFormatter.format(date));
		
		intializeAppliantDictionary();
	}

	protected void setup() {
		
		
		Object[] args = getArguments();
        this.applicantName = args[0].toString(); 	// this returns the String "1"
        
		// TODO: Create behaviour that receives messages
        // CyclicBehaviour msgReceivingBehaviour = new msgReceivingBehaviour();
        // Waiting for received messages
        // addBehaviour(msgReceivingBehaviour);
        
		System.out.println("Appliance Agent" + this.applicantName + " is created!");
		
        TickerBehaviour communicateToHome = new TickerBehaviour(this, UPATE_DURATION) {
    
            protected void onTick() {
            	
            	sendActualUsage();
            	
            	// predictUsage();		//TODO: update this later
            }
            
            // Energy Consumption Stimulation and Send to Home Agent 
            // Agents doesn't produce the energy but read from database and send them to Home Agent
            protected void sendActualUsage() {
            	String energyConsumed = Double.toString(getActualEnergyUsage(USAGE_DURATION));
            	
            	// TODO: implement service
            	// TODO: search for homeagent
            	
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
            	String pathToCSV = "./src/database/Electricity_P_DS.csv";;
            	// Read a certain amount of data from CSV file
				File file = new File(pathToCSV);
				if(file.exists()) {
					// do something
				    try {
				    	// Create an object of filereader class 
				        FileReader filereader = new FileReader(pathToCSV); 
				  
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
	
	protected void setApplicantID(String applicantName) {
		this.applicantName = applicantName;
	}
	
	public String getApplicantName() {
		return this.applicantName;
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
	
	private int setActualLivedSeconds(int actualLivedSeconds) {
		return this.actualLivedSeconds = actualLivedSeconds;
	}
	
	private int getActualLivedSeconds() {
		return this.actualLivedSeconds;
	}
	
	// Energy Consumption Stimulation - Agent doesn't actual consume energy, it reads from data file and return
	private Double getActualEnergyUsage(int timeDuration) {
		int dataIndex= applicantDict.get(this.applicantName.toUpperCase());
		Double totalUsage = 0.0;
		String pathToCSV = "./src/database/Electricity_P_DS.csv";
    	File directory = new File(pathToCSV);
 	   
		File file = new File(pathToCSV);
		if(file.exists()) {
		    try {
		    	// Create an object of filereader class 
		        FileReader filereader = new FileReader(pathToCSV); 
		  
		        // create csvReader object to read the csv file and skip already read Line
		        CSVReader csvReader = new CSVReaderBuilder(filereader) 
		                                  .withSkipLines(getActualLivedSeconds()/1000)			// index of rows to be read
		                                  .build();
		        String[] nextRecord;
		        
		        // Each row is for 30 min -> if timeDuration is 30 mins then 1 rows will be read
		        int noOFRowsToRead = timeDuration / 1800000;
		        
		        for (int i = 0; i < noOFRowsToRead; i++) {
		        	// Each line is read as 1 array
		        	nextRecord = csvReader.readNext();
		        	// Calculate total usage
		        	totalUsage += Double.parseDouble(nextRecord[dataIndex]);
		        	System.out.println(nextRecord[dataIndex]);
		        	
		        }
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		}
		
		// update the number second have lived
		setActualLivedSeconds(getActualLivedSeconds() + timeDuration);
		
		return totalUsage;
	}
	
	public String estimateElectricity() {
		return "";
	}
	
	private void intializeAppliantDictionary() {
		applicantDict = new HashMap<String, Integer>();
		applicantDict.put("WHE", 2);
		applicantDict.put("RSE", 3);
		applicantDict.put("GRE", 4);
		applicantDict.put("MHE", 5);
		applicantDict.put("B1E", 6);
		applicantDict.put("BME", 7);
		applicantDict.put("CWE", 8);
		applicantDict.put("DWE", 9);
		applicantDict.put("EQE", 10);
		applicantDict.put("FRE", 11);
		applicantDict.put("HPE", 12);
		applicantDict.put("OFE", 13);
		applicantDict.put("UTE", 14);
		applicantDict.put("WOE", 15);
		applicantDict.put("B2E", 16);
		applicantDict.put("CDE", 17);
		applicantDict.put("DNE", 18);
		applicantDict.put("EBE", 19);
		applicantDict.put("FGE", 20);
		applicantDict.put("HTE", 21);
		applicantDict.put("OUE", 22);
		applicantDict.put("TVE", 23);
		applicantDict.put("UNE", 24);
	}
}
		
