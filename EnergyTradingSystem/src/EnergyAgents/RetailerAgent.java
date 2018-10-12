package EnergyAgents;

import java.util.Date;
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
import GUI.*;
/**
 * Retailer Agent: 
 * Response to any requests from only one Home agent
 * 
 * @author Tola
 *
 */

public class RetailerAgent extends Agent implements GUIListener{
	// Agent Identification
	private String agentId;
	private String agentName;
	private String agentType;
	
	// charge per hour, unit price in cent
	private double usageCharge;
	
	// charge is over the meter of the contract
	private double overCharge;
	
	
	// negotiation, price in cent per KWH, time in second
	// negotiation price is calculated in every iteration offer
	private double negoPrice;
	// limit amount , eg. no less than 25 cents offer
	private double negoLimitPrice;
	// limit counter offer
	private int negoCounterOffer;
	private int negoCounter;
	// negotiation timeout in second
	private long negoTimeStart;
	private long negoTimeWait;
	
	// negotiation mechanism: by time, on demand
	public static enum Mechanism {
		GENERAL ,BY_TIME ,ON_DEMAND
	}
	private Mechanism negoMechanism;
	
	// every iterator, price will reduce by 0.25%
	private double negoIterateReduceBy;
		
	
	// if the agent can buy power from a home agent (only one) such as solar
	private String buyFrom;
	private double buyAmount;
	private double buyPrice;
	private Date buyDate;
		
	// create an agent default setup
	public RetailerAgent() {
		// initialize default value
		init();
		
		// Register the interface that must be accessible by an external program through the O2A interface
		registerO2AInterface(GUIListener.class, this);
	}
	
	 
	/**
	 * initialize the agent in setup
	 */
	private void init() {
		agentId = "";
		agentName = "Retailer";
		agentType = "Retailer";
		
		usageCharge = 25.0;
		overCharge = usageCharge + (usageCharge * 0.15);	// plus 15%
		
		negoPrice = usageCharge;
		negoLimitPrice = negoPrice - (negoPrice * 0.10);	// eg. no more than 10%
		negoIterateReduceBy = 0.05; // reduce by 0.05 percent in each counter
		
		negoMechanism = Mechanism.GENERAL;
		negoCounterOffer = 3;
		negoCounter = 0;
		negoTimeWait = 5;	// 5 seconds
		
		
		buyFrom = null;
		buyAmount = 0.0;
		buyPrice = 0.0;
		buyDate = null;
		
	} // end init
	
	
	
	/**
	 * Getter and Setter
	 */
	public double getUsageCharge() {
		return usageCharge;
	}
	public void setUsageCharge(double usageCharge) {
		this.usageCharge = usageCharge;
	}
	
	
	public double getOverCharge() {
		return overCharge;
	}
	public void setOverCharge(double overCharge) {
		this.overCharge = overCharge;
	}


	public double getNegotiationPrice() {
		return negoPrice;
	}
	public void setNegotiationPrice(double negoStartPrice) {
		this.negoPrice = negoStartPrice;
	}


	public Mechanism getNegotiationMechanism() {
		return negoMechanism;
	}
	public void setNegotiationMechanism(Mechanism negoMechanism) {
		this.negoMechanism = negoMechanism;
	}


	public double getNegotiationLimitPrice() {
		return negoLimitPrice;
	}
	public void setNegotiationLimitPrice(double negoLimitPrice) {
		this.negoLimitPrice = negoLimitPrice;
	}


	public int getNegotiationCounterOffer() {
		return negoCounterOffer;
	}
	public void setNegotiationCounterOffer(int negoCounterOffer) {
		this.negoCounterOffer = negoCounterOffer;
	}


	public long getNegotiationTimeWait() {
		return negoTimeWait;
	}
	public void setNegotiationTimeWait(long negoTimeWait) {
		this.negoTimeWait = negoTimeWait;
	}

	
	public double getNegotiationIterationReduceBy() {
		return negoIterateReduceBy;
	}
	public void setNegotiationIterationReduce(double negoIterateReduce) {
		this.negoIterateReduceBy = negoIterateReduce;
	}
	

	public String getBuyFrom() {
		return buyFrom;
	}
	public void setBuyFrom(String buyFrom) {
		this.buyFrom = buyFrom;
	}


	public double getBuyAmount() {
		return buyAmount;
	}
	public void setBuyAmount(double buyAmount) {
		this.buyAmount = buyAmount;
	}


	public double getBuyPrice() {
		return buyPrice;
	}
	public void setBuyPrice(double buyPrice) {
		this.buyPrice = buyPrice;
	}


	public Date getBuyDate() {
		return buyDate;
	}
	public void setBuyDate(Date buyDate) {
		this.buyDate = buyDate;
	}

	
	/* --- Negotiation --- */
	/**
	 * perform counter offer
	 * @param double offer price
	 * @return double counter offer or 0
	 */
	private double doCounterOffer(double offerPrice) {
		// reject any offer below the limited price
		if ( offerPrice < negoLimitPrice ) {
			return 0;
		}
		// if no reach the limit counter
		double nextOffer = 0;
		if ( negoCounter < negoCounterOffer ) {
			switch (negoMechanism) {
				case BY_TIME:
					break;
				case ON_DEMAND:
					break;
				default:
					double reducePercentage = (negoCounter + 1) * negoIterateReduceBy;
					nextOffer = usageCharge - (usageCharge * reducePercentage);
			}
			// count the offer
			negoCounter++;
		}
		
		return truncatedDouble(nextOffer);
	}

	/* --- Jade function --- */
	/**
	 * setup jade agent
	 */
	@Override
	protected void setup() {
		agentId = getAID().getLocalName();
		agentName += "_" + agentId;
		System.out.println( agentName +" Started.");
		
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
		
	}
	
	
	
	/**
	 * clean up
	 * Jade agent destroyed
	 */
	@Override
	protected void takeDown() {
		// Deregister
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
				fe.printStackTrace();
		}
		
		System.out.println( agentName +" stopped.");
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
				
				// get content
				String content = msg.getContent();
			
				// create a reply message
				ACLMessage reply = msg.createReply();
				
				// correspondent for individual response from home agent
				switch(msg.getPerformative()) {
					// step 1: receive a request from home agent, then propose the negotiation price
					case ACLMessage.REQUEST:
					case ACLMessage.QUERY_REF:
						// reset negotiate
						negoPrice = usageCharge;
						negoCounter = 0;
						negoTimeStart = System.currentTimeMillis() / 1000;
						System.out.println( agentName + " sends the first offer for " + negoPrice);
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(Double.toString(negoPrice));
						break;
						
					// step 2: receive the counter offer from home agent, then calculate the offer
					case ACLMessage.PROPOSE:
						double offer = 0;
						// check the content is double
						try {
							offer = Double.parseDouble(content);
						}catch ( NumberFormatException nfe) {
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("NOT UNDERSTOOD");
							break;
						}
						
						// Time out
						long timeNow = System.currentTimeMillis() / 1000;
						if ( timeNow - negoTimeStart > negoTimeWait ) {
							System.out.println( agentName + " sends expire message");
							reply.setPerformative(ACLMessage.REFUSE);
							reply.setContent("Sorry, your counter is expired.");
							break;
						}
						
						// start counter offer
						double nextOffer = doCounterOffer(offer);
						
						// if the next offer is available
						if ( nextOffer > 0 ) {
							negoPrice = nextOffer;
							
							// accept if in range
							if ( offer >= negoPrice ) {
								System.out.println( agentName + " sends accept message");
								reply.setPerformative(ACLMessage.AGREE);
								reply.setContent("Thanks for your purchase.");
							} else {
								// send next round
								negoTimeStart = System.currentTimeMillis() / 1000;
								System.out.println( agentName + " sends counter offer for " + negoPrice);
								reply.setPerformative(ACLMessage.PROPOSE);
								reply.setContent(Double.toString(negoPrice));
							}
							break;
						} else {
							System.out.println( agentName + " sends reject counter message");
							reply.setPerformative(ACLMessage.REFUSE);
							reply.setContent("Sorry, your counter offer is limited.");
						}
						
						break;
						
					// step 3: receive the reception from home agent, whether accept, reject
					case ACLMessage.ACCEPT_PROPOSAL:
					case ACLMessage.AGREE:
						// Timeout
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
						// do nothing
						//reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
						//reply.setContent("NOT UNDERSTOOD");
				}
				
				// send back
				myAgent.send(reply);
				
			} else {
				// wait for message
				block();
			}// end if msg
		}// end action
	}// end ServicesBehaviour
	
	
	/* ----- GUI ---- */
	@Override
	public void showGUI() {
		RetailerGUIDetails gui = new RetailerGUIDetails(this);
		gui.showGUI();
	}
	
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("Agent Id:" + agentId);
		
		str.append(", Usage Charge:" + usageCharge);
		str.append(", Over Charge:" + overCharge);
		str.append(", Negotiation Price:" + negoPrice);
		str.append(", Negotiation Limit Price:" + negoLimitPrice);
		str.append(", Limit counter offer:" + negoCounterOffer);
		str.append(", Waiting Time:" + negoTimeWait);
		str.append(", Mechanism:" + negoMechanism.toString());
		
		return str.toString();
		
	}
	
	
	/**
	 * Helper functions
	 */
	private double truncatedDouble(double value) {
		return java.math.BigDecimal.valueOf(value).setScale(3, java.math.RoundingMode.HALF_UP).doubleValue();
	}
	
	
	/**
	 * *** Not used ***
	 * Database for saving configuration
	 */
	private void createTable() {
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
