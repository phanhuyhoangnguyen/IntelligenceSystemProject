package EnergyAgents;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import database.DbHelper;

/**
 * Retailer Agent: 
 * Response to any requests from single Home agent
 * 
 * @author Tola
 *
 */

public class RetailerAgent extends Agent {
	// Agent Identification
	private String agentName;
	private String agentType;
	
	// charge per hour, unit price in cent
	private double usageCharge;
	
	// charge is over the meter of the contract
	private double overCharge;
	
	
	// negotiation, price in cent per KWH, time in second
	// negotiation price for every iteration
	private double negoPrice;
	// negotiation mechanism: by time, on demand
	private String negoMechanism;
	// limit amount , no less than 25 cents offer
	private double negoLimit;
	// limit counter offer
	private int negoCounter;
	// negotiation timeout in second
	private long negoTimeout;
	
	// list map of home agent in the contract, by AID and Name
	private Map<AID,Date> homeAgents; 
		
	
	// if the agent can buy power from a home agent (only one) such as solar
	private String buyFrom;
	private double buyAmount;
	private double buyPrice;
	private Date buyDate;
		
	// create an agent default setup
	public RetailerAgent() {
		init();
	}
	
	
	/**
	 * initialize the agent in setup
	 */
	private void init() {
		agentName = "Retailer";
		agentType = "Retailer";
		
		usageCharge = 25.0;
		overCharge = 30.0;
		negoPrice = 28.0;
		negoLimit = 20.0;
		negoMechanism = "general";
		negoCounter = 3;
		negoTimeout = 60; // 1 minute
		
		buyFrom = null;
		buyAmount = 0.0;
		buyPrice = 0.0;
		buyDate = null;
		
		homeAgents = new HashMap<>();
		
	}
	
	
	/**
	 * 
	 */
	
	
	/* --- Jade function --- */
	
	/**
	 * setup jade agent
	 */
	@Override
	protected void setup() {
		agentName += "_" + getAID().getLocalName();
		System.out.println( agentName +" Starting.");
		
		// Register the service
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(agentType);
		sd.setName(agentName);
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// add behaviour to accept the offer
		addBehaviour(new ServicesBehaviour());
		
		guiShow();
	}
	
	/**
	 * clean up
	 */
	@Override
	protected void takeDown() {
		// Deregister
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
				fe.printStackTrace();
		}
		guiClose();
		System.out.println( agentName +" closed.");
	}
	
	
	/* --- Jade Agent behaviour classes --- */
	/**
	 * waiting for any request from home agent
	 * 
	 */
	private class ServicesBehaviour extends CyclicBehaviour {
		public void action() {
			// Receive all messages
			ACLMessage msg = myAgent.receive();
			
			// There is a message
			if (msg != null) {
				
				// get sender AID
				AID sender = msg.getSender();
				String senderName = sender.getLocalName();
				System.out.println( agentName + " receives message from " + sender);
				
			
				// create a reply message
				ACLMessage reply = msg.createReply();
				
				// correspondent for individual response from home agent
				switch(msg.getPerformative()) {
					case ACLMessage.REQUEST:
					case ACLMessage.QUERY_REF:
						System.out.println( agentName + " sends the first offer for " + negoPrice);
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(Double.toString(negoPrice));
						break;
						
					case ACLMessage.PROPOSE:
						System.out.println( agentName + " sends counter offer for " + negoPrice);
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(Double.toString(negoPrice));
						// TODO: counter offer
						break;
						
					case ACLMessage.ACCEPT_PROPOSAL:
					case ACLMessage.AGREE:
						System.out.println( agentName + " thank for your support.");
						// TODO: sign new contract
						reply.setPerformative(ACLMessage.AGREE);
						reply.setContent(Double.toString(usageCharge));
						break;
						
					case ACLMessage.REJECT_PROPOSAL:
					case ACLMessage.REFUSE:
						System.out.println( agentName + " sorry to hear that. ");
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent("sorry to hear that");
						break;
						
					case ACLMessage.INFORM:
						System.out.println( agentName + " received an inform from " + senderName);
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("Sorry, cannot buy any energy for now");
						break;
						
					default:
						reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
						reply.setContent("NOT UNDERSTOOD");
				}
				
				// send back
				myAgent.send(reply);
				
			} else {
				// wait for message
				block();
			}// end if msg
		}// end action
	}// end ServicesBehaviour
	
	
	
	/* --- Helper function --- */
	
	/**
	 * show the user interface after the agent created
	 */
	private void guiShow() {
		
	}
	
	
	/**
	 * close the user interface after the agent terminated
	 */
	private void guiClose() {
		
	}
	
	/**
	 * Database for saving configuration
	 */
	public void createTable() {
		String tableName = "Retailler_tb";
		
		Map<String, String> columns = new LinkedHashMap();
		
		
		columns.put("id", "INTEGER PRIMARY KEY AUTOINCREMENT");
		columns.put("aid", "VARCHAR(255)");		// agent id
		columns.put("usageCharge", "DECIMAL");	// price in cent per khw
		columns.put("overCharge", "DECIMAL");	// price in cent per khw

		columns.put("negoPrice", "DECIMAL");	// negotiation price for every iteration
		columns.put("negoMechanism", "VARCHAR(32)");	// negotiation mechanism: by time or on demand				
		columns.put("negoLimit", "DECIMAL");	// limit amount , no less than 25 cents offer
		
		//columns.put("waitingTime", "INTEGER");	// in hour
		
		// Buy from Home
		columns.put("buyFrom", "VARCHAR(255)");
		columns.put("buyAmount", "DECIMAL");
		columns.put("buyPrice", "DECIMAL");
		
		DbHelper db = new DbHelper();
		if( db.connect() ) {
			db.dropTable(tableName);
			db.createTable(tableName, columns);
		}
	}
	
	
	
	
	
}
