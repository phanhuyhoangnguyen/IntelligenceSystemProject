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
	
	// price always not change
	private double fixedPrice;
	
	// charge is over the demand in %
	private double overCharge;
	
	// demand usage
	private double demandUsage;
	
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
		RANDOM ,ON_DEMAND, FIXED_PRICE
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
		
		demandUsage = 0;
		fixedPrice = 0.35; // always
		usageCharge = Utilities.truncatedDouble(Utilities.getRandomDouble(20, 30)/100);	// 20 to 30 cents per kwh
		overCharge = 10;	// plus 10%
		
		
		negoPrice = calcNegoPrice(0);	// calculate negoPrice based on demand
		negoLimitPrice = Utilities.truncatedDouble( usageCharge - (usageCharge * 0.15) );	// eg. no more than 15%
		negoIterateReduceBy = 0.2; // reduce 0.2 percent in each counter
		
		negoMechanism = Mechanism.RANDOM;
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

	public double getFixedPrice() {
		return fixedPrice;
	}
	public void setFixedPrice(double fixedPrice) {
		this.fixedPrice = fixedPrice;
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
		demandUsage = 0;
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
		switch( negoMechanism ) {
			case FIXED_PRICE:
				nextOffer = fixedPrice;
				break;
			default:
				if ( negoCounter < negoCounterOffer ) {
					double reducePercentage = (negoCounter + 1) * negoIterateReduceBy;
					nextOffer = negoPrice - (negoPrice * reducePercentage);
				}
		}
		
		// count the offer
		negoCounter++;
		return Utilities.truncatedDouble(nextOffer);
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
			case FIXED_PRICE:
				price = fixedPrice;
				break;
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
		
		return Utilities.truncatedDouble(price);
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
		
		// get arguments
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			
			// if the args[0] is mechanism
			for ( Mechanism m : Mechanism.values() ) {
				if ( m.toString().equalsIgnoreCase(args[0].toString()) ) {
					negoMechanism = m;
				}
			}
			
			// if the args[1] is price
			try {
				double price = Double.parseDouble(args[1].toString());
				usageCharge = price;
				negoLimitPrice = Utilities.truncatedDouble( usageCharge - (usageCharge * 0.15) );
			}catch(Exception ex) {}
			
		}// end args loop
		
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
		
		System.out.println( agentName +" was deleted.");
	}
		
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
				
				// correspondent for individual response from home agent
				switch(msg.getPerformative()) {
					// step 1: receive a request from home agent, then propose the negotiation price
					case ACLMessage.CFP:
						resetNegotiation();
						
						// calculate negotiation price based on demand
						try {
							double demand = Double.parseDouble(content);
							demandUsage = demand;
							negoPrice = calcNegoPrice(demandUsage);
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
							reply.setContent(Double.toString(negoPrice));
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
								negoPrice = offer;
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
						System.out.println( agentName + " completed for $" + negoPrice);
						printGUI( agentName + " completed for <b>$" + negoPrice + "</b>");
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
					
					case ACLMessage.QUERY_REF:
						try {
							double  actualUsage = Double.parseDouble(content);
							// over charge
							if ( actualUsage > demandUsage ) {
								double overUsage = actualUsage - demandUsage;
								double price = negoPrice + ( negoPrice * (overCharge/100) );
								double calOverCharge = Utilities.truncatedDouble( overUsage * price );
								System.out.println("Send overcharge price: $"+ calOverCharge);
								reply.setPerformative(ACLMessage.QUERY_REF);
								reply.setContent(String.valueOf(calOverCharge));
							}else {
								System.out.println("No overchage");//@Dave
								reply.setPerformative(ACLMessage.QUERY_REF);
								reply.setContent("0");
							}
							break;	
						}catch ( NumberFormatException nfe) {
							nfe.printStackTrace();
							System.out.println( agentName + " not understood.");
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("NOT UNDERSTOOD");
						}catch ( NullPointerException npe) {
							npe.printStackTrace();
							System.out.println( agentName + " not understood.");
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("NOT UNDERSTOOD");
						}
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
	
	
	
	
}
