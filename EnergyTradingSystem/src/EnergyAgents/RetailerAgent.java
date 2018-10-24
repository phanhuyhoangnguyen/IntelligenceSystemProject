package EnergyAgents;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.proto.ContractNetResponder;
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
	
	// current usage
	private double currentUsage;
	
	// negotiation price in cent per KWH, time in second
	
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
		
		currentUsage = 0;
		usageCharge = getRandomDouble(20.0, 30.0);	// 20 to 30 cents per kwh
		overCharge = usageCharge + (usageCharge * 0.05);	// plus 5%
		
		negoPrice = calcNegoPrice(0);	// calculate negoPrice based on demand
		negoLimitPrice = truncatedDouble( negoPrice - (negoPrice * 0.15) );	// eg. no more than 15%
		negoIterateReduceBy = 0.2; // reduce 0.2 percent in each counter
		
		negoMechanism = Mechanism.GENERAL;
		negoCounterOffer = 3;
		negoCounter = 0;
		negoTimeWait = 15;	// 15 seconds
		
		
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
	 * restart negotiation
	 */
	private void resetNegotiation() {
		// reset negotiate
		currentUsage = 0;
		negoPrice = usageCharge;
		negoCounter = 0;
		negoTimeStart = System.currentTimeMillis() / 1000;
	}
	
	
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
			double reducePercentage = (negoCounter + 1) * negoIterateReduceBy;
			nextOffer = negoPrice - (negoPrice * reducePercentage);
			// count the offer
			negoCounter++;
		}
		
		return truncatedDouble(nextOffer);
	}

	
	/**
	 * calculate negotiation price based on demand and mechanism
	 * @param double demand
	 * @return double price
	 */
	private double calcNegoPrice(double demand) {
		double price = usageCharge;
		if ( demand == 0) {
			return price;
		}
		switch( negoMechanism ) {
			case BY_TIME:
			case ON_DEMAND:
				if ( demand >= 150 ) {
					price *= 0.2;
				} else if ( demand >= 100 ) {
					price *= 0.15;
				} else  if ( demand >= 50 ) {
					price *= 0.1;
				}
				break;
			default:
		}
		
		return truncatedDouble(price);
	}


	
	
	/* --- Jade function --- */
	/**
	 * setup jade agent
	 */
	@Override
	protected void setup() {
		agentId = getAID().getName();
		agentName = getAID().getLocalName();
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
		
		// add contact net behaviour
		/*
		MessageTemplate template = MessageTemplate.and(
		  		MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
		  		MessageTemplate.MatchPerformative(ACLMessage.CFP) );
		addBehaviour( new ServicesReponder(this, template));
		*/
		
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
	
	
		
	
	/**
	 * ContractNet Behaviour
	 */
	private class ServicesReponder extends ContractNetResponder {

		public ServicesReponder(Agent a, MessageTemplate mt) {
			super(a, mt);
			
		}

		/*
		 * First step
		 * send usage charge to home agent 
		 */
		@Override
		protected ACLMessage prepareResponse(ACLMessage cfp) {
			System.out.println(agentName + " start proposing " + String.valueOf(usageCharge) + " to " + cfp.getSender().getLocalName());
			printGUI(agentName + " start proposing <b>" + String.valueOf(usageCharge) + "</b> to " + cfp.getSender().getLocalName());
			// create propose
			ACLMessage propose = cfp.createReply();
			propose.setPerformative(ACLMessage.PROPOSE);
			propose.setContent(String.valueOf(usageCharge));
			
			resetNegotiation();
			
			return propose;
		}
		
		
		

		
		/**
		 * Handle the offer from home agent
		 */
		@Override
		protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
			
			System.out.println(agentName + " received " + cfp.getContent() + " from " + cfp.getSender().getLocalName() + " , acl : " + cfp.getPerformative());
			
			ACLMessage reply = cfp.createReply();
			
			switch ( cfp.getPerformative() ) {
				
				case ACLMessage.PROPOSE:
					System.out.println("Agent "+getLocalName()+": send proposal ");
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(26));
					return reply;
					
				// counter offer
				case ACLMessage.QUERY_REF:
					System.out.println("Agent "+getLocalName()+": query ref ");
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(27));
					return reply;
					
				default:
					return super.handleCfp(cfp);
			}
			
			
		}
		
		
		
		/**
		 * when home agent accept
		 */
		@Override
		protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
			System.out.println(accept.getSender().getLocalName() +" accepted the proposal");
			printGUI(accept.getSender().getLocalName() +" accepted the proposal");
			// say thank
			ACLMessage inform = accept.createReply();
			inform.setProtocol(accept.getProtocol());
			inform.setContent("Thank you for your support from " + agentName);
			inform.setPerformative(ACLMessage.INFORM);
			return inform;
		}

		
		/**
		 * when home agent reject
		 */
		protected ACLMessage handleRejectProposal(ACLMessage reject) {
			System.out.println(reject.getSender().getLocalName() +" reject the proposal");
			printGUI(reject.getSender().getLocalName() +" rejected the proposal");
			// say sorry
			ACLMessage inform = reject.createReply();
			inform.setProtocol(reject.getProtocol());
			inform.setContent("Sorry, to hear that from " + agentName);
			inform.setPerformative(ACLMessage.INFORM);
			return inform;
		}


	}
	// end contractNetBehaviour
	
	/* --- Jade Agent behaviour classes --- */
	/**
	 * Negotiation
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
				System.out.println( agentName + " receives " + msg.getContent() + " from " + senderName);
				
				// get content
				String content = msg.getContent();
			
				// create a reply message
				ACLMessage reply = msg.createReply();
				//reply.setConversationId(msg.getConversationId());
				reply.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
				//reply.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
				
				// correspondent for individual response from home agent
				switch(msg.getPerformative()) {
					// step 1: receive a request from home agent, then propose the negotiation price
					case ACLMessage.CFP:
						resetNegotiation();
						
						// calculate negotiation price based on demand
						try {
							double demand = Double.parseDouble(content);
							negoPrice = calcNegoPrice(demand);
						}catch ( NumberFormatException nfe) {
							nfe.printStackTrace();
							System.out.println( agentName + " not understood.");
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("NOT UNDERSTOOD");
							break;
						}catch ( NullPointerException npe) {
							npe.printStackTrace();
							System.out.println( agentName + " not understood.");
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("NOT UNDERSTOOD");
							break;
						}
						
						
						System.out.println( agentName + " sends the first negotiation $" + negoPrice);
						printGUI(agentName + " sends the first negotiation for $" + negoPrice);
						
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent(Double.toString(negoPrice));
						break;
						
					// step 2: receive the counter offer from home agent, then calculate the offer
					case ACLMessage.REQUEST:
						// Time out
						long timeNow = System.currentTimeMillis() / 1000;
						if ( timeNow - negoTimeStart > negoTimeWait ) {
							System.out.println( agentName + " sends expire message");
							printGUI( agentName + "sends time-out");
							reply.setPerformative(ACLMessage.REFUSE);
							reply.setContent("0");
							break;
						}
						
						double offer = 0;
						// check the content is double
						try {
							offer = Double.parseDouble(content);
						}catch ( NumberFormatException nfe) {
							nfe.printStackTrace();
							System.out.println( agentName + " not understood.");
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("NOT UNDERSTOOD");
							break;
						}catch ( NullPointerException npe) {
							npe.printStackTrace();
							System.out.println( agentName + " not understood.");
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("NOT UNDERSTOOD");
							break;
						}
						
						
						
						// start counter offer
						double nextOffer = doCounterOffer(offer);
						
						// if the next offer is available
						if ( nextOffer > 0 ) {
							negoPrice = nextOffer;
							
							// accept if in range
							if ( offer >= negoPrice ) {
								System.out.println( agentName + " sends accept message for $" + offer);
								printGUI(agentName + " accept the offer for $" + offer);
								reply.setPerformative(ACLMessage.AGREE);
								reply.setContent(String.valueOf(offer));
							} else {
								// send next round
								System.out.println( agentName + " sends counter offer for $" + negoPrice);
								printGUI(agentName + " sends counter offer for <b>$" + negoPrice + "</b>");
								reply.setPerformative(ACLMessage.REQUEST);
								reply.setContent(Double.toString(negoPrice));
								negoTimeStart = System.currentTimeMillis() / 1000;
							}
							break;
						} else {
							System.out.println( agentName + " reject the counter offer");
							printGUI( agentName + " reject the counter offer is too low");
							reply.setPerformative(ACLMessage.REFUSE);
							reply.setContent(Double.toString(negoPrice));
						}
						
						break;
						
					// step 3: receive the reception from home agent, accept or agree
					case ACLMessage.ACCEPT_PROPOSAL:
					case ACLMessage.AGREE:
						// Timeout
						System.out.println( agentName + " completed for $" + negoPrice);
						printGUI( agentName + " completed for <b>$" + negoPrice + "</b>");
						// TODO: sign new contract
						reply.setPerformative(ACLMessage.AGREE);
						reply.setContent(Double.toString(negoPrice));
						break;
					
					//Home agent reject the offer
					case ACLMessage.REJECT_PROPOSAL:
					case ACLMessage.REFUSE:
						System.out.println( agentName + " was reject by " + senderName);
						printGUI( agentName + " was reject by " + senderName);
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("0");
						break;
								
					default:
						// do nothing
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

	
	
	/* ----- GUI ---- */
	@Override
	public void showGUI() {
		RetailerGUIDetails gui = new RetailerGUIDetails(this);
		gui.showGUI();
	}
	
	
	private void printGUI(String text) {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME ));
		msg.setContent("<font color='red'>" + text + "</font>");
		send(msg);
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
	
	public double getRandomDouble(double min, double max){
	    double d = (Math.random()*((max-min)+1))+min;
	    return Math.round(d*100.0)/100.0;
	}
	
	/* --- NOT USE --- */
	
	
	
	/**
	 * Database for saving configuration
	 
	
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
	*/
	
	
	
	
}
