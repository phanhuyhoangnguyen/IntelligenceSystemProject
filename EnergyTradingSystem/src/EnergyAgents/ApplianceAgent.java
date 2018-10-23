package EnergyAgents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat; 
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import GUI.GUIListener;
import GUI.RetailerGUIDetails;
import database.DbHelper;

/**
 * ApplianceAgent
 * @author Phan
 * 
 * @Description The applicant agent compute its energy usage and send it to the HomeAgent periodically
 */
public class ApplianceAgent extends Agent {
	private String applianceName;
	private String serviceType;
	
	// For Message Communication to HomeAgent
	// TODO: change this later
	private static final int UPATE_DURATION = 5000;					// 10s -> specify the frequency of message sent to Home Agent. 
																	// Ideally, this should be equal to USAGE_DURATION. However, waiting 30 mins to see message sent is too long
	// For energyUsage Stimulation
	private static int actualLivedSeconds;							// number of seconds agents have lived since created
	private Map <String, Integer> applicantDict;					// hold agent name and its index for searching its usage in data file
	private static final int USAGE_DURATION = 1800000;				// 30 mins (1800s) -> specify the total usage of agent in a period of time, 30 mins.
	private static final String pathToCSV = "./src/database/Electricity_P_DS.csv";
	private static final int HALF_HOUR = 1800000;
	
	// For prediction
	private static final int LIVED_DAYS = 15;						// 15 days: number of days agents have lived in the stimulation
	private static final int secondsInADay = 86400;					// number of seconds in a day
	
	// For Home Agent
	private AID homeAgent;
	private static final String HomeAgentService = "Home";
	private boolean isFinishedNegotiated = true;
	
	public ApplianceAgent () {
		
		// this is set for stimulating the Appliance Agent has lived for at least 30 mins (1 row in CSV)
		this.actualLivedSeconds = HALF_HOUR;
		intializeAppliantDictionary();
	}

	protected void setup() {
		Object[] args = getArguments();				// Arguments should be in format: AgentName:EnergyAgents.AppianceAgent("Appliance","ApplianceName"); 
		if (args != null && args.length > 0) {
			this.applianceName = args[1].toString();
	        this.serviceType = args[0].toString();
	        System.out.println("Appliance Agent: " + getLocalName() + " is created!");
	        
			// Create Service Description to be registered from the arguments
	        ServiceDescription sd  = new ServiceDescription();
	        sd.setType(getServiceType());
	        sd.setName(getApplianceName());
	        register(sd);
	        
	        addBehaviour(new WaitForStart());
	        
		} else {
			System.out.println(getLocalName() + ": " + "You have not specified any arguments.");
		}
    }
    
    // This behaviour search for home agent
    private class searchHomeAgent extends OneShotBehaviour {
    	public void action() {
    		// Search for home agent
    		setHomeAgent(searchForHomeAgent(HomeAgentService));
    	}
    }
    
    // This behaviour search for home agent
    private class reportingEnergyUsagePrediction extends OneShotBehaviour {
    	public void action() {
			String predictionUsage;
			
			double predictedValue = predictUsage(USAGE_DURATION);
			
			// round up the double value to 2 decimal places
			DecimalFormat df = new DecimalFormat("#.##");
			predictionUsage = df.format(predictedValue);
			
	        System.out.println("prediction of " + getLocalName() + "(" + getApplianceName() + "): " + predictionUsage);
	        
        	// Send request to HomeAgent
            sendRequestBuyingEnergyToHome(predictionUsage); 
    	}
    }
    
    // This behaviour send actual energy usage to home
    private class reportingActualEnergyUsage extends OneShotBehaviour {
    	public void action() {
        	// this is only trigger when the stage 1 is completed
        	sendActualUsage();
    	}
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
    	msg.addReceiver(getHomeAgent());
        // Set the interaction protocol
    	msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

    	// Specify the reply deadline (10 seconds)
    	msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        
		// Set message content
    	msg.setContent(predictionUsage);

    	isFinishedNegotiated = false;
    	// Define the AchieveREInitiator behaviour with the message
    	addBehaviour(new AchieveREInitiator(this, msg) {
    		// Method to handle an agree message from responder
    		protected void handleAgree(ACLMessage agree) {
    			System.out.println(getLocalName() + ": " + agree.getSender().getLocalName() + " has agreed to the request");
    		}
    
    		// Method to handle an inform message from Home Agent after its negotiation with Retailer is success
	        protected void handleInform(ACLMessage inform) {
	        	System.out.println(getLocalName() + ": " + inform.getSender().getLocalName() + " has negotiated successful with Retailer Agent. Appliance's request is fulfiled");
	        	System.out.println(getLocalName() + ": " + inform.getSender().getName() + "'s offer is " + inform.getContent());
	        }
	        
	        // Method to handle a refuse message from responder
	        protected void handleRefuse(ACLMessage refuse) {
	        	System.out.println(getLocalName() + ": " + refuse.getSender().getLocalName() + " refused to buy energy. Appliance's request is not met");
	        	// TODO: uncomment this to check if Appliance is able to send request again
	        	// addBehaviour(new reportingEnergyUsagePrediction());
	        }
	
	        // Method to handle a failure message (failure in delivering the message)
	        protected void handleFailure(ACLMessage failure) {
	        	if (failure.getSender().equals(myAgent.getAMS())) {
	        		// FAILURE notification from the JADE runtime -> the receiver does not exist
	        		System.out.println(getLocalName() + ": " + getHomeAgent() +" does not exist");
	        	} else {
	                System.out.println(getLocalName() + ": " + failure.getSender().getLocalName() + " failed to perform the requested action");
	        	}
	        }
	            
	        // Method that is invoked when notifications have been received from all responders
	        protected void handleAllResultNotifications(Vector notifications) {
	        	System.out.println(getLocalName() + ": " + " the request is completed!");
	        	isFinishedNegotiated = true;
	        }
	    });
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

            // Register Agent's Service with DF
            DFService.register(this, dfd);
        }

	    catch (FIPAException fe) { fe.printStackTrace(); } 
	}
	
	/**
	 *  Find and return Agent from its service
	 */
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
	
	/**
	 *  Method to de register the service (on take down)
	 */
    protected void takeDown() {
    	try { DFService.deregister(this); }
    	catch (Exception e) {}
    }
	
	protected ApplianceAgent getApplianceAgent() {
		return this;
	}
	
	protected void setApplianceName(String applianceName) {
		this.applianceName = applianceName;
	}
	
	public String getApplianceName() {
		return this.applianceName;
	}
	
	protected void setServiceType (String serviceType) {
		this.serviceType = serviceType;
	}
	
	public String getServiceType() {
		return this.serviceType;
	}
	
	private void setActualLivedSeconds(int actualLivedSeconds) {
		this.actualLivedSeconds = actualLivedSeconds;
	}
	
	private int getActualLivedSeconds() {
		return this.actualLivedSeconds;
	}
	
	private void setHomeAgent(AID homeAgent) {
		this.homeAgent = homeAgent;
	}
	
	private AID getHomeAgent() {
		return this.homeAgent;
	}
	
	/**
	 *  Energy Consumption Prediction - Naive Prediction with the data from CSV file
	 */
	protected double predictUsage(int duration) {
		// get the data for the Appliance Agent from CSV file based on the index column
		int dataIndex = applicantDict.get(this.applianceName.toUpperCase());
		
		double predictUsage = 0;
		
		File file = new File(pathToCSV);
		if(file.exists()) {
		    try {
		    	// Create an object of filereader class 
		        FileReader filereader = new FileReader(pathToCSV); 
		  
		        // create csvReader object to read the csv file and skip already read Line
		        // predicted values is start from row 1 -> this will be used to compared with actual usage in row 2
		        CSVReader csvReader = new CSVReaderBuilder(filereader)
		                                  .withSkipLines(getActualLivedSeconds()/HALF_HOUR)	// number of row to be skipped
		                                  .build();
		        String[] nextRecord;
		        
		        // the duration is set to be equal to the energy usage consumption duration - 30 mins (1800s) -> 1 row
		        int noOfRowsToRead = duration / HALF_HOUR;
		        
		        for (int i = 0; i < noOfRowsToRead; i++) {
		        	// Each line is read as 1 array
		        	nextRecord = csvReader.readNext();
		        	
		        	 // predicted value is the last observation from the values in CSV file
		        	predictUsage = Double.parseDouble(nextRecord[dataIndex]);
		        }
		        
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		}
		
		return predictUsage;
    }
	
	/**
	 *  Energy Consumption Stimulation - Agent reads from data from CSV file and return
	 */
	private Double getActualEnergyUsage(int timeDuration) {
		int dataIndex= applicantDict.get(this.applianceName.toUpperCase());
		Double totalUsage = 0.0;
		
		// TODO: discuss with team
        System.out.println(getApplianceName() + " getActualLivedSeconds before updating: "+ getActualLivedSeconds() + " timeDuration: " + timeDuration);
		// update the number second have lived: move row 1 -> row 2 for CSV reading below
		setActualLivedSeconds(getActualLivedSeconds() + timeDuration);
        System.out.println(getApplianceName() + " getActualLivedSeconds after updating: "+ getActualLivedSeconds());
 	   
		File file = new File(pathToCSV);
		if(file.exists()) {
		    try {
		    	// Create an object of FileReader class 
		        FileReader filereader = new FileReader(pathToCSV); 
		  
		        // create csvReader object to read the csv file and skip already read Line
		        // actual usage is read from row 2
		        CSVReader csvReader = new CSVReaderBuilder(filereader) 
		                                  .withSkipLines(getActualLivedSeconds()/HALF_HOUR)			// number of row to be skipped
		                                  .build();
		        
		        String[] nextRecord;
		        
		        // Each row is for 30 min -> if timeDuration is 30 mins (1800s) then 1 rows will be read
		        int noOFRowsToRead = timeDuration / HALF_HOUR;
		        
		        for (int i = 0; i < noOFRowsToRead; i++) {
		        	// Each line is read as 1 array
		        	nextRecord = csvReader.readNext();
		        	// Calculate total usage
		        	totalUsage += Double.parseDouble(nextRecord[dataIndex]);
		        	
		        }
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		}

		return totalUsage;
	}
	
	/**
	 * Initialize the map which Appliance's Name and its address in CSV file
	 */
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
	
	/**
	 * Print to GUI agent
	 * @param text
	 */
	private void printGUI(String text) {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME ));
		msg.setContent("<font color='red'>" + text + "</font>");
		send(msg);
	}
	private void printGUIClean() {
		ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
		msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME ));
		msg.setContent("");
		send(msg);
	}
	
	/**
	 * Implement Cyclic behaviour
	 * waiting for start inform
	 */
	private class WaitForStart extends CyclicBehaviour{
		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().compareToIgnoreCase("start") == 0 ) {
					printGUIClean();
					//System.out.println("<font color='gray'> Start negotiation</font>");
					startNegotiation();
				}
			} else {
				block();
			}
		}
	} // end wait for start
	
	/**
	 * Start negotiation
	 */
	private void startNegotiation() {
		SequentialBehaviour sb = new SequentialBehaviour();
        
        searchHomeAgent searchHomeAgent = new searchHomeAgent();
        
        // Communicate to Home Agent for requesting buy energy with prediction amount and send the actual usage
        TickerBehaviour communicateToHome = new TickerBehaviour(this, UPATE_DURATION) {
    
            protected void onTick() {
            	if (isFinishedNegotiated) {
		        	SequentialBehaviour communicationSequence = new SequentialBehaviour();
			        // Register state Predicting and Request to buy
		        	communicationSequence.addSubBehaviour(new reportingEnergyUsagePrediction());
			        // Register state Reporting Actual Usage
		        	communicationSequence.addSubBehaviour(new reportingActualEnergyUsage());
			        
			        addBehaviour(communicationSequence);
            	}
            }
        };
        
        // Trigger service to find home agent
        sb.addSubBehaviour(searchHomeAgent);	 
        
        // Sending message every 5 seconds
        sb.addSubBehaviour(communicateToHome);
        
        // add sequential behaviour to the Agent
        addBehaviour(sb);
	} // end start negotiation
}