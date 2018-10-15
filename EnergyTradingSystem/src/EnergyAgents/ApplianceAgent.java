package EnergyAgents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

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
import java.util.Vector;

//import com.opencsv.*;

import database.DbHelper;

/**
 * ApplicantEnum 
 * @author Phan
 * 
 * @Description The applicant agent compute its energy usage and send it to the HomeAgent periodically
 */
public class ApplianceAgent extends Agent {
	private String applicantName;
	
	// For Message Communication to HomeAgent
	private static final int UPATE_DURATION = 10000;				// 10s -> specify the frequency of message sent to Home Agent. 
																	// Ideally, this should be equal to USAGE_DURATION. However, waiting 30 mins to see message sent is too long
	// For energyUsage Stimulation
	private static int actualLivedSeconds;							// number of seconds agents have lived since created
	private Map <String, Integer> applicantDict;					// hold agent name and its index for searching its usage in data file
	private static final int USAGE_DURATION = 1800000;				// 30 mins -> specify the total usage of agent in a period of time, 30 mins.
	private static final String pathToCSV = "./src/database/Electricity_P_DS.csv";
	
	// For prediction
	private static final int LIVED_DAYS = 30;						// 30 days: number of days agents have lived in the stimulation
	private static final int secondsInADay = 86400;					// number of seconds in a day
	
	// For Home Agent
	private AID homeAgent;
	private static final String HomeAgentService = "Home";
	
	public ApplianceAgent () {
		
		// this is set for skipping the first row in CSV file below
		this.actualLivedSeconds = 1000;				//TODO: check if this should be changed to startTime and endTime
		
		intializeAppliantDictionary();
	}

	protected void setup() {
		Object[] args = getArguments();				// Arguments should be in format: AgentName:EnergyAgents.AppianceAgent("Appliance","ApplianceName"); 
        this.applicantName = args[1].toString();
        
        System.out.println("Appliance Agent" + getLocalName() + " is created!");
        // Create Service Description to be registered
        ServiceDescription sd  = new ServiceDescription();
        sd.setType(args[0].toString());
        sd.setName(getApplicantName());
        
        // calling Agent's method to start the registration process
        register(sd);
        
        searchForHomeAgent(HomeAgentService);
        
        // Send Request to Home Agent for buy energy with prediction amount
		
        TickerBehaviour communicateToHome = new TickerBehaviour(this, UPATE_DURATION) {
    
            protected void onTick() {							// TODO: this can be changed with FNSM
            	sendActualUsage();
            	
            	String predictionUsage = predictUsage();		//TODO: define this later
            	
                // sendRequestBuyingEnergyToHome(""); 			// TODO: update with the predictionUsage later
            }
        };
        
        // Sending message every 5 seconds
        addBehaviour(communicateToHome);
    }

	private AID searchForHomeAgent(String service) {
		AID homeAgent = null;
	    // Search for HomeAgent via its service
	   	DFAgentDescription[] agent = getService(service);
	    if (agent.length > 0) {
	    	homeAgent = agent[0].getName();
	    }
	    else {
	           	System.out.println("Home Service is not found!");
	    }
	    return homeAgent;
	}
	
    // Energy Consumption Stimulation and Send to Home Agent 
    // Agents doesn't produce the energy but read from database and send them to Home Agent
    protected void sendActualUsage() {
    	String energyConsumed = Double.toString(getActualEnergyUsage(USAGE_DURATION));
        	
    	// Send messages to home agents
	    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
	    msg.setContent(energyConsumed);
	    msg.addReceiver(getHomeAgent());
	    
	    // Send Message
	    System.out.println(getLocalName() + ": Sending message " + msg.getContent() + " to ");
	    
	    Iterator receivers = msg.getAllIntendedReceiver();
	    while(receivers.hasNext()) {
	            System.out.println(((AID)receivers.next()).getLocalName());
	    }
	    // send message
	    send(msg);
    }
	
	private void sendRequestBuyingEnergyToHome(String predictionUsage) {
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
    	msg.addReceiver(this.homeAgent);
        // Set the interaction protocol
    	msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

    	// Specify the reply deadline (10 seconds)
    	msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        
    	// Set message content
    	msg.setContent("Prediction Usage: " + predictionUsage);

    	// Define the AchieveREInitiator behaviour with the message
    	// TODO: how to do nested behaviour, this is also need to be invoked periodically
    	addBehaviour(new AchieveREInitiator(this, msg) {
    		// Method to handle an agree message from responder
    		protected void handleAgree(ACLMessage agree) {
    			System.out.println(getLocalName() + ": " + agree.getSender().getName() + " has agreed to the request");
    		}
    
    		// Method to handle an inform message from Home Agent after its negotiation with Retailer is success
	        protected void handleInform(ACLMessage inform) {
	        	System.out.println(getLocalName() + ": " + inform.getSender().getName() + " negotiate successful. Appliance's request is fulfiled");
	        }
	
	        // Method to handle a refuse message from responder
	        protected void handleRefuse(ACLMessage refuse) {
	        	System.out.println(getLocalName() + ": " + refuse.getSender().getName() + " negotiate failed. Appliance's request is not met");
	        }
	
	        // Method to handle a failure message (failure in delivering the message)
	        protected void handleFailure(ACLMessage failure) {
	        	if (failure.getSender().equals(myAgent.getAMS())) {
	        		// FAILURE notification from the JADE runtime -> the receiver does not exist
	        		System.out.println(getLocalName() + ": " + getHomeAgent() +" does not exist");
	        	} else {
	                System.out.println(getLocalName() + ": " + failure.getSender().getName() + " failed to perform the requested action");
	        	}
	        }
	            
	        // Method that is invoked when notifications have been received from all responders
	        protected void handleAllResultNotifications(Vector notifications) {
	        	System.out.println(getLocalName() + ": " + " the request is completed!");
	        }
	    });
	}
	
	protected String predictUsage() {
		int dataIndex = applicantDict.get(this.applicantName.toUpperCase());
		final int TIME_INDEX = 1;
		
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
		        int noOFRowsToRead = LIVED_DAYS * secondsInADay;
		        
		        for (int i = 0; i < noOFRowsToRead; i++) {
		        	// Each line is read as 1 array
		        	nextRecord = csvReader.readNext();
		        	
		        	// timeOfTheDay in row
		        	int timeOfTheDay = Integer.parseInt(nextRecord[TIME_INDEX]);
		        	// 9:00 PM on the 04/01/2012 - 1333314000
		        	// 9:00 PM on the 04/02/2012 - 1333400400
		        	// 9:00 PM on the 04/03/2012 - 1333486800
		        	System.out.println("Time of the day: " + timeOfTheDay);
		        	
		        	// usage in row
		        	System.out.println("Usage: " + nextRecord[dataIndex]);
		        	
		        }
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		}
		return "";
    }

	private void register(ServiceDescription sd) {
		DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	    dfd.addServices(sd); // An agent can register one or more services
	    
	    try {
            // Search for the old DFD and deregister it
            DFAgentDescription list[] = DFService.search( this, dfd );
            if ( list.length>0 ) 
            	DFService.deregister(this);
            
            // Add this Service Description to DFAgentDescription
            dfd.addServices(sd);

            // Register Agent�s Service with DF
            DFService.register(this, dfd);
        }

	    catch (FIPAException fe) { fe.printStackTrace(); } 
	}
	
    private DFAgentDescription[] getService(String service) {
		DFAgentDescription dfd = new DFAgentDescription();
		
    	// Create service template for search
        ServiceDescription sd = new ServiceDescription();
        sd.setType(service);

        // Add Service Template to DFAgentDescription 
        dfd.addServices(sd);

        // Search Agent with the target services using DF
        try {
                DFAgentDescription[] result = DFService.search(this, dfd);
                return result;
        }
        catch (Exception fe) {}
        return null;
	}
	
	 // Method to de register the service (on take down)
    protected void takeDown() {
    	/* try { DFService.deregister(this); }
    	catch (Exception e) {} */
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
	
	private int setActualLivedSeconds(int actualLivedSeconds) {
		return this.actualLivedSeconds = actualLivedSeconds;
	}
	
	private int getActualLivedSeconds() {
		return this.actualLivedSeconds;
	}
	
	private AID getHomeAgent() {
		return this.homeAgent;
	}
	
	// Energy Consumption Stimulation - Agent doesn't actual consume energy, it reads from data file and return
	private Double getActualEnergyUsage(int timeDuration) {
		int dataIndex= applicantDict.get(this.applicantName.toUpperCase());
		Double totalUsage = 0.0;
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
		
