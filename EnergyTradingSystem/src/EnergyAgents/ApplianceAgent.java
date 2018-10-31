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
	// TODO: change this later ///Dave change to 15s
	private static final int UPATE_DURATION = 15000;				// 30s -> specify the frequency of message sent to Home Agent. 
																	// Ideally, this should be equal to USAGE_DURATION. However, waiting 30 mins to see message sent is too long
	// For energyUsage Stimulation
	private int actualLivedSeconds;									// number of seconds agents have lived since created
	private Map <String, Integer> applicantDict;					// hold agent name and its index for searching its usage in data file
	private static final int USAGE_DURATION = 1800000;				// 30 mins (1800s) -> specify the total usage of agent in a period of time, 30 mins.
	private static final int HALF_HOUR = 1800000;

	//private static final String pathToCSV = "./src/database/Electricity_P_DS.csv";
	// ! Testing for VS Code
	private static final String pathToCSV = "./EnergyTradingSystem/src/database/Electricity_P_DS.csv";
	
	// For prediction
	private static final int LIVED_DAYS = 15;						// 15 days: number of days agents have lived in the stimulation
	private static final int secondsInADay = 86400;					// number of seconds in a day
	
	// For dealing with Home Agent
	private AID homeAgent;
	private static final String HomeAgentService = "Home";
	private boolean communicateIsFinished = true;
	private TickerBehaviour communicateWithHome;
	private SequentialBehaviour sequenceCommunication;
	
	// For manipulating Ticker Behaviour From GUI
	private boolean isPausing;										// pause the operations executed in the ticker behaviour
	
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
	        
	        addBehaviour(new handleCommandFromGUI());
	        
		} else {
			System.out.println(getLocalName() + ": " + "You have not specified any arguments.");
		}
    }
	
	/**
	 * 	Register Appliance's service to the DF
	 *  @param sd: ServiceDescription of the service of this Appliance to be registered with DF
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
	 * Implement Cyclic behaviour waiting for command sent from GUI for Agent Behaviour Manipulation
	 */
	private class handleCommandFromGUI extends CyclicBehaviour{
		@Override
		public void action() {
			MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			
			ACLMessage msg = myAgent.receive(messageTemplate);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM) {
					if (msg.getContent().compareToIgnoreCase("start") == 0) {
						printGUIClean();
						startCommunication();
						isPausing = false;
					} else if (msg.getContent().compareToIgnoreCase("pause") == 0) {
						pauseCommunication();
					} else if (msg.getContent().compareToIgnoreCase("resume") == 0) {
						resumeCommunication();
					}
				}
			} else {
				block();
			}
		}
	}
	
	/**
	 * Start negotiation
	 */
	private void startCommunication() {
		sequenceCommunication = new SequentialBehaviour();
        
        SearchHomeAgent searchHomeAgent = new SearchHomeAgent();
        
        // Communicate to Home Agent for requesting buy energy with prediction amount and send the actual usage
        // 1st tick is set to 1 second
        communicateWithHome = new CommunicateWithHome(this, 1000);
        
        // trigger service to find home agent
        sequenceCommunication.addSubBehaviour(searchHomeAgent);	 
        
        // sending message every 5 seconds
        sequenceCommunication.addSubBehaviour(communicateWithHome);
        
        // add sequential behaviour to the Agent
        addBehaviour(sequenceCommunication);
	}
	
	private void pauseCommunication() {
		isPausing = true;
        printGUI(getLocalName() +  ": state is changed to pause");
	}
	
	private void resumeCommunication() {
		isPausing = false;
		printGUI(getLocalName() +  ": state is changed to resume");
	}
	
	private class CommunicateWithHome extends TickerBehaviour {
		public CommunicateWithHome(Agent agent, long period) {
			super(agent, period);
		}

		@Override
		protected void onTick() {
			// onTick should only be triggered if there is no request is in process and state of Appliance agent is not pausing
        	if (communicateIsFinished && !isPausing) {
	        	// change status for the new request
        		communicateIsFinished = false;
	        	
	        	double predictedValue = getUsagePrediction(USAGE_DURATION);
				
				// round up the double value to 2 decimal places
				DecimalFormat df = new DecimalFormat("#.##");
				String predictionUsage = df.format(predictedValue);
				
		        System.out.println(getLocalName() +" : Prediction value for the next period is: " + predictionUsage + " Watts");
		        
	        	// create message to send to HomeAgent
	            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
	        	msg.addReceiver(getHomeAgent());
	        	
	            // set the interaction protocol
	        	msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

	        	// specify the reply deadline (20 seconds)
	        	msg.setReplyByDate(new Date(System.currentTimeMillis() + 20000));
	            
	    		// set message content
	        	msg.setContent(predictionUsage);
	        	
		        // add AchieveREInitiator behaviour with the message to send Prediction and Request to buy
	        	addBehaviour(new SendEnergyUsagePrediction(getApplianceAgent(), msg));
	        	
		        // after the 1st tick, the update duration is set to 30s
		        reset(UPATE_DURATION);
        	}
		}
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
	 *  This method to search for home agent
	 *  @param service: service's name of the Target Agent to be search for
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
	 *  Find and return Agent from its service
	 *  @param service: service's name of the Target Agent to be search for
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
	 *  This behaviour send usage prediction for home agent
	 */
    private class SendEnergyUsagePrediction extends AchieveREInitiator {
    	
    	public SendEnergyUsagePrediction(Agent agent, ACLMessage msg) {
			super(agent, msg);
		}
    	
		// Method to handle an agree message from responder
    	@Override
		protected void handleAgree(ACLMessage agree) {
			System.out.println(getLocalName() + ": " + agree.getSender().getLocalName() + " has agreed to the request");
	        
	        // home agree to the request, negotiation process between Home Appliance and Retailers start. Appliance will wait for the result of negotiation
        	// only listen to Home Agent with Inform message
	    	MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
	    	
			// add behaviour that receives messages contained result of negotation
	    	addBehaviour(new ResultReceiver(getApplianceAgent(), messageTemplate));
		}

		// Method to handle an inform message from Home Agent after its negotiation with Retailer is success
    	@Override
        protected void handleInform(ACLMessage inform) {
        	System.out.println(getLocalName() + ": received the inform from " + inform.getSender().getLocalName());
        	
		    // print to GUI
	        printGUI(getLocalName() + ": received the inform from " + inform.getSender().getLocalName());
        }
        
        // Method to handle a refuse message from responder
    	@Override
        protected void handleRefuse(ACLMessage refuse) {        	

	        // home refuse to the request - communication is set to finished
    		communicateIsFinished = true;
    		
    		// this will occur when Home is running out of the budget - Appliance will stop sending request after the current request is done
        	// add behaviour to report actual usage
        	addBehaviour(new ReportingActualEnergyUsage());
        	
        	// remove behaviour to stop sending the request
    		sequenceCommunication.removeSubBehaviour(communicateWithHome);

        	System.out.println(getLocalName() + ": " + refuse.getSender().getLocalName() + " refused to the request. Stop sending the request for next period.");

		    // print to GUI
	        printGUI(getLocalName() + ": " + refuse.getSender().getLocalName() + " refused to the request.");
        }

        // Method to handle a failure message (failure in delivering the message)
    	@Override
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
    	@Override
        protected void handleAllResultNotifications(Vector notifications) {
        	System.out.println(getLocalName() + ": the request is completed!");
        }
    }
	
	/**
	 *  Energy Consumption Prediction - Naive Prediction with the data from CSV file
	 *  @param timeDuration: the period of time to make the prediction of energy usage for this Appliance
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
	 *  ResultReceiver Behavior - Receiving Home Agent Message Inform Negotiation result of it and Retailer Agent
	 */
	private class ResultReceiver extends Behaviour {

		private MessageTemplate msgTemplate;
		private boolean isReceived = false;
		
		public ResultReceiver(Agent a, MessageTemplate msgTemplate) {
			super (a);
			this.msgTemplate = msgTemplate;
		}
		
		@Override
		public void action() {
			// System.out.println(getLocalName() + ": Waiting for Result Message....");

			// retrieve message from message queue if there is
	        ACLMessage msg= receive(this.msgTemplate);
	        if (msg!=null) {
		        String messageReturned = msg.getContent();
		        
		        // reformat message to print out the GUI
		        if (messageReturned.compareToIgnoreCase("failure") != 0) {
		        	messageReturned =  "$" + msg.getContent();
		        }
		        // print out message content to console
		        System.out.println(getLocalName() + ": received result " + messageReturned + " from " + msg.getSender().getLocalName());
		        
		        // print out message content to GUI
	        	printGUI(getLocalName() + ": received result <b>" + messageReturned + "</b> from " + msg.getSender().getLocalName());
	        	
	        	// stop waiting for the result message
		        isReceived = true;
		        
	        	// add behaviour to report actual usage
	        	addBehaviour(new ReportingActualEnergyUsage());
			}else {
				// block the behaviour from terminating and keep listening to the message
				block();
			}
	    }
		
		public boolean done() {
			return isReceived;
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
	    
        // set communication status is completed
        communicateIsFinished = true;
    }
    
    /**
	 *  Energy Consumption Stimulation - Agent reads from data from CSV file and return
	 *  @param timeDuration: the period of time with related energy consumption of this Appliance
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
	 * @param text: content to be printed out the GUI
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