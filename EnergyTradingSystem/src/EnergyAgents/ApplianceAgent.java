package EnergyAgents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
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
import jade.lang.acl.MessageTemplate;
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
 * @author Phan - 101042618
 * @Description The applicant agent compute its prediction, request Home Agent to buy energy and send its actual usage periodically
 */
public class ApplianceAgent extends Agent {
	private String applianceName;
	private String serviceType;
	
	// For Message Communication to HomeAgent
	// TODO: change this later
	private static final int UPATE_DURATION = 300000000;				// 30s -> specify the frequency of message sent to Home Agent. 
																	// Ideally, this should be equal to USAGE_DURATION. However, waiting 30 mins to see message sent is too long
	// For energyUsage Stimulation
	private int actualLivedSeconds;									// number of seconds agents have lived since created
	private Map <String, Integer> applicantDict;					// hold agent name and its index for searching its usage in data file
	private static final int USAGE_DURATION = 1800000;				// 30 mins (1800s) -> specify the total usage of agent in a period of time, 30 mins.
	private static final int HALF_HOUR = 1800000;

	//private static final String pathToCSV = "./src/database/Electricity_P_DS.csv";
	// ! Testing
	private static final String pathToCSV = "./EnergyTradingSystem/src/database/Electricity_P_DS.csv";
	
	// For prediction
	private static final int LIVED_DAYS = 15;						// 15 days: number of days agents have lived in the stimulation
	private static final int secondsInADay = 86400;					// number of seconds in a day
	
	// For Home Agent
	private AID homeAgent;
	private static final String HomeAgentService = "Home";
	private boolean requestIsMet = true;
	
	// For testing
	private int testCounter = 0;
	
	public ApplianceAgent () {
		// Appliance Agent has lived for at least 30 mins (1 row in CSV)
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
	
	/**
	 * 	Register Appliance's service to the DF
	 */
	private void register(ServiceDescription sd) {
		DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	    dfd.addServices(sd); // An agent can register one or more services
	    
	    try {
            // search for the old DFD and deregister it
            DFAgentDescription list[] = DFService.search( this, dfd );
            if ( list.length>0 ) 
            	DFService.deregister(this);
            
            // add this Service Description to DFAgentDescription
            dfd.addServices(sd);

            // register Agent's Service with DF
            DFService.register(this, dfd);
        }

	    catch (FIPAException fe) { fe.printStackTrace(); } 
	}
	
	/**
	 *  Find and return Agent from its service
	 */
    private DFAgentDescription[] getService(String service) {
		DFAgentDescription dfd = new DFAgentDescription();
		
    	// create service template for search
        ServiceDescription sd = new ServiceDescription();
        sd.setType(service);

        // add Service Template to DFAgentDescription 
        dfd.addServices(sd);

        // search Agent with the target services using DF
        try {
                DFAgentDescription[] result = DFService.search(this, dfd);
                return result;
        }
        catch (Exception fe) {}
        return null;
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
					startNegotiation();
				}
			} else {
				block();
			}
		}
	}
	
	/**
	 * Start negotiation
	 */
	private void startNegotiation() {
		SequentialBehaviour sb = new SequentialBehaviour();
        
        SearchHomeAgent searchHomeAgent = new SearchHomeAgent();
        
        // Communicate to Home Agent for requesting buy energy with prediction amount and send the actual usage
        // 1st tick is set to 1 second
        TickerBehaviour communicateToHome = new TickerBehaviour(this, 1000) {
    
            protected void onTick() {
            	if (requestIsMet) {
		        	
		        	String predictionUsage;
					double predictedValue = getUsagePrediction(USAGE_DURATION);
					
					// round up the double value to 2 decimal places
					DecimalFormat df = new DecimalFormat("#.##");
					predictionUsage = df.format(predictedValue);
					
			        System.out.println("prediction of " + getLocalName() + "(" + getApplianceName() + "): " + predictionUsage);
			        
		        	// create message to send to HomeAgent
		            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		        	msg.addReceiver(getHomeAgent());
		        	
		            // set the interaction protocol
		        	msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

		        	// specify the reply deadline (10 seconds)
		        	msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
		            
		    		// set message content
		        	msg.setContent(predictionUsage);
		        	requestIsMet = false;
		        	
			        // add AchieveREInitiator behaviour with the message to send Prediction and Request to buy
		        	addBehaviour(new SendEnergyUsagePrediction(getApplianceAgent(), msg));
		        	
		        	// only listen to Home Agent with Inform message
			    	MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
			    			MessageTemplate.MatchSender(getHomeAgent()));
			        
					// add behaviour that receives messages contained result of negotation
			    	addBehaviour(new ResultReceiver(getApplianceAgent(), messageTemplate));
			    	
			        // after the 1st tick, the update duration is set to 30s
			        this.reset(UPATE_DURATION);
            	}
            }
		};
		
		/*
		//TODO : @DAVE This code below is for testing (run only 1)
		// Communicate to Home Agent for requesting buy energy with prediction amount and send the actual usage
		DelayBehaviour communicateToHome = new DelayBehaviour(this, 3000) {
			protected void handleElapsedTimeout() {
				if (true) {
					SequentialBehaviour communicationSequence = new SequentialBehaviour();
		        	
		        	String predictionUsage;
					double predictedValue = getUsagePrediction(USAGE_DURATION);
					
					// round up the double value to 2 decimal places
					DecimalFormat df = new DecimalFormat("#.##");
					predictionUsage = df.format(predictedValue);
					
			        System.out.println("prediction of " + getLocalName() + "(" + getApplianceName() + "): " + predictionUsage);
			        
		        	// create message to send to HomeAgent
		            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		        	msg.addReceiver(getHomeAgent());
		        	
		            // set the interaction protocol
		        	msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

		        	// specify the reply deadline (10 seconds)
		        	msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
		            
		    		// set message content
		        	msg.setContent(predictionUsage);
		        	isFinishedNegotiated = false;
		        	
			        // add AchieveREInitiator behaviour with the message to send Prediction and Request to buy
		        	communicationSequence.addSubBehaviour(new SendEnergyUsagePrediction(getApplianceAgent(), msg));
		        	
		        	// add behaviour to report actual usage
		        	communicationSequence.addSubBehaviour(new ReportingActualEnergyUsage());
		        	
			        addBehaviour(communicationSequence);
			        
			    	// only listen to Home Agent with Inform message
			    	MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
			    			MessageTemplate.MatchSender(getHomeAgent()));
			        
					// add behaviour that receives messages
			    	addBehaviour(new ResultReceiver(getApplianceAgent(), messageTemplate));
			        
			        // after the 1st tick, the update duration is set to 30s
			        this.reset(UPATE_DURATION);
				}
			}
		};*/
        
        // trigger service to find home agent
        sb.addSubBehaviour(searchHomeAgent);	 
        
        // sending message every 5 seconds
        sb.addSubBehaviour(communicateToHome);
        
        // add sequential behaviour to the Agent
        addBehaviour(sb);
	}
     
	/**
	 * This behaviour search for home agent
	 */
    private class SearchHomeAgent extends OneShotBehaviour {
    	public void action() {
    		// search for home agent
    		setHomeAgent(searchForHomeAgent(HomeAgentService));
    	}
    }
    
	/**
	 *  This behaviour send usage prediction for home agent
	 */
    private class SendEnergyUsagePrediction extends AchieveREInitiator {
    	public SendEnergyUsagePrediction(Agent a, ACLMessage msg) {
			super(a, msg);
		}
		// Method to handle an agree message from responder
		protected void handleAgree(ACLMessage agree) {
			System.out.println(getLocalName() + ": " + agree.getSender().getLocalName() + " has agreed to the request");
			
		    // print to GUI
	        printGUI(getLocalName() +  ": " + agree.getSender().getLocalName() + " has agreed to the request");
		}

		// Method to handle an inform message from Home Agent after its negotiation with Retailer is success
        protected void handleInform(ACLMessage inform) {
        	System.out.println(getLocalName() + ": receive the inform from " + inform.getSender().getLocalName());
        	
		    // print to GUI
	        printGUI(getLocalName() + ": receive the inform from " + inform.getSender().getLocalName());
        }
        
        // Method to handle a refuse message from responder
        protected void handleRefuse(ACLMessage refuse) {
        	System.out.println(getLocalName() + ": " + refuse.getSender().getLocalName() + " refused to the request.");
        	
		    // print to GUI
	        printGUI(getLocalName() + ": " + refuse.getSender().getLocalName() + " refused to the request.");
        }

        // Method to handle a failure message (failure in delivering the message)
        protected void handleFailure(ACLMessage failure) {
        	if (failure.getSender().equals(myAgent.getAMS())) {
        		// FAILURE notification from the JADE runtime -> the receiver does not exist
        		System.out.println(getLocalName() + ": " + getHomeAgent() +" does not exist");
        	} else {
                System.out.println(getLocalName() + ": " + failure.getSender().getLocalName() + " failed to perform the requested action");
                
    		    // print to GUI
    	        printGUI(getLocalName() + ": " + failure.getSender().getLocalName() + " failed to perform the requested action");
        	}
        }
            
        // Method that is invoked when notifications have been received from all responders
        protected void handleAllResultNotifications(Vector notifications) {
        	System.out.println(getLocalName() + ": the request is completed!");
        	requestIsMet = true;
        	// print to GUI
	        printGUI(getLocalName() + ": the request is completed!");
    	
        }
    }

	/**
	 *  This behaviour send actual energy usage to home
	 */
    private class ReportingActualEnergyUsage extends OneShotBehaviour {
    	public void action() {
        	// this is only trigger when the stage 1 is completed
        	sendActualUsage();
    	}
    }
    
	/**
	 *  This method to search for home agent
	 */
	private AID searchForHomeAgent(String service) {
		AID homeAgent = null;
	    // search for HomeAgent via its service
	   	DFAgentDescription[] agent = getService(service);
	    if (agent.length > 0) {
	    	homeAgent = agent[0].getName();
	    }
	    else {
	        System.out.println(getLocalName() + ": Home Service is not found!");
	        
		    // print to GUI
	        printGUI(getLocalName() + ": Home Service is not found!");
	    }
	    return homeAgent;
	}
	
	/**
	 *  Energy Consumption Stimulation and Send to Home Agent
	 *  Agents doesn't produce the energy but read from database and send them to Home Agent
	 */
    protected void sendActualUsage() {
    	String energyConsumed = Double.toString(getActualEnergyUsage(USAGE_DURATION));
        	
    	// send messages to home agents
	    ACLMessage msg = new ACLMessage(ACLMessage.INFORM_REF);
	    msg.setContent(energyConsumed);
	    msg.addReceiver(getHomeAgent());
	    
	    // send Message
	    System.out.println(getLocalName() + ": Sending Actual Usage to Home: " + msg.getContent());

	    // print to GUI
        printGUI(getLocalName() + ": Sending Actual Usage to Home: <b>" + msg.getContent() + "</b>");
	    
        // send message with actual energy usage
	    send(msg);
    }
	
	/**
	 *  Energy Consumption Prediction - Naive Prediction with the data from CSV file
	 */
	protected double getUsagePrediction(int duration) {
		// get the data for the Appliance Agent from CSV file based on the index column
		int dataIndex = applicantDict.get(this.applianceName.toUpperCase());
		
		double predictUsage = 0;
		
		File file = new File(pathToCSV);
		if(file.exists()) {
		    try {
		    	// create an object of filereader class 
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
		        	// each line is read as 1 array
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
		
		// update the number second have lived: move row 1 -> row 2 for CSV reading below
		setActualLivedSeconds(getActualLivedSeconds() + timeDuration);
 	   
		File file = new File(pathToCSV);
		if(file.exists()) {
		    try {
		    	// create an object of FileReader class 
		        FileReader filereader = new FileReader(pathToCSV); 
		  
		        // create csvReader object to read the csv file and skip already read Line
		        // actual usage is read from row 2
		        CSVReader csvReader = new CSVReaderBuilder(filereader) 
		                                  .withSkipLines(getActualLivedSeconds()/HALF_HOUR)			// number of row to be skipped
		                                  .build();
		        
		        String[] nextRecord;
		        
		        // each row is for 30 min -> if timeDuration is 30 mins (1800s) then 1 rows will be read
		        int noOFRowsToRead = timeDuration / HALF_HOUR;
		        
		        for (int i = 0; i < noOFRowsToRead; i++) {
		        	// each line is read as 1 array
		        	nextRecord = csvReader.readNext();
		        	// calculate total usage
		        	totalUsage += Double.parseDouble(nextRecord[dataIndex]);
		        	
		        }
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		}

		return totalUsage;
	}
	
	/**
	 *  ResultReceiver Behavior - Receiving Home Agent Message Inform Negotiation result of it and Retailer Agent
	 */
	private class ResultReceiver extends CyclicBehaviour {

		private MessageTemplate msgTemplate;
		private boolean isReceived = false;
		
		public ResultReceiver(Agent a, MessageTemplate msgTemplate) {
			super (a);
			this.msgTemplate = msgTemplate;
		}
		
		@Override
		public void action() {
			//System.out.println(getLocalName() + ": Waiting for Result Message....");

			// retrieve message from message queue if there is
	        ACLMessage msg= receive(this.msgTemplate);
	        if (msg!=null) {
		        // print out message content to console
		        System.out.println(getLocalName() + ": received result " + msg.getContent() + " from " + msg.getSender().getLocalName());

		        // print out message content to GUI
		        printGUI(getLocalName() + ": received result <b>" + msg.getContent() + "</b> from " + msg.getSender().getLocalName());
		        isReceived = true;
		        
	        	// add behaviour to report actual usage
	        	addBehaviour(new ReportingActualEnergyUsage());
			}
	    
	        // block the behaviour from terminating and keep listening to the message
	        block();
	    }
		/*
		public boolean done() {
			return isReceived;
		}*/
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
		msg.setContent("<font color='green'>" + text + "</font>");
		send(msg);
	}
	
	private void printGUIClean() {
		ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
		msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME ));
		msg.setContent("");
		send(msg);
	}
	
	/**
	 *  Method to de register the service (on take down)
	 */
    protected void takeDown() {
    	try { DFService.deregister(this); }
    	catch (Exception e) {}
    }
    
	/**
	 *  Getter(s) and Setter(s) of Agent
	 */
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
}