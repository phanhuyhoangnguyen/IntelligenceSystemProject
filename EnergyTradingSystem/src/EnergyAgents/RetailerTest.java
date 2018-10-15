package EnergyAgents;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

public class RetailerTest extends Agent {

	public RetailerTest() {
		System.out.print("Retailer Test class startted.");
	}
	
	@Override
	protected void setup() {
		String agentName = getAID().getLocalName();
		System.out.println( "Retailer Test: " + agentName +" Started.");
		
		
		// contact net CFP message
		ACLMessage msg = new ACLMessage(ACLMessage.CFP);
		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
		
		// We want to receive a reply in 10 secs
		msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
		
		// Grab Retailer Agents
		List<AID> retailers = getAgentsByService("Retailer");
		if ( retailers.size() == 0 ) {
			System.out.println("Hi Dave, run retailer agents first.");
			return;
		}
		
		// add retailers to message
		for ( int i=0; i<retailers.size(); i++) {
			msg.addReceiver(retailers.get(i));
		}
		
		// ok, retailer can I ask a proposal, the price for 100 k
		msg.setContent("100");
		
		// attach contract net initialitor
		addBehaviour(new ContractNetInit(this, msg));
		
		// add behaviour to accept the offer
		addBehaviour(new NegoBehaviour());
	}
	
	
	
	/**
	 * Inner class initialize proposal
	 * @author Tola
	 *
	 */
	private class ContractNetInit extends ContractNetInitiator {

		public ContractNetInit(Agent a, ACLMessage cfp) {
			super(a, cfp);
		}

		/*
		 *	 handle response from retailer agents
		 */
		protected void handleAllResponses(Vector responses, Vector acceptances) {
			
			AID successAgent = null;
			ACLMessage successACL = null;
			
			Double offerPrice = Double.MAX_VALUE;	// set to infinite value
			
			// Loop through all responses, find the best one
			Enumeration e = responses.elements();
			while (e.hasMoreElements()) {
				ACLMessage msg = (ACLMessage) e.nextElement();
				System.out.println("Propose from " + msg.getSender().getLocalName() + " for " + msg.getContent());
				
				// get the offer price
				double proposePrice = 0.0;
				try {
					proposePrice = Double.parseDouble(msg.getContent());
				}catch( NumberFormatException nfe) {
						nfe.printStackTrace();
				}
				
				// Receive a propose
				if ( msg.getPerformative() == ACLMessage.PROPOSE ) {
					System.out.println(msg.getSender().getLocalName() + " proposes " + msg.getContent());
					
					// create a reply
					ACLMessage reply = msg.createReply();
					reply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
					
					// compare to find the best offer
					if ( proposePrice < offerPrice ) {
						// accept
						offerPrice = proposePrice;
						
						if ( successACL != null) {
							successACL.setPerformative(ACLMessage.REJECT_PROPOSAL);
						}
						
						successACL = reply;
						successACL.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						successAgent = msg.getSender();
						acceptances.addElement(successACL);
					}else {
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						acceptances.addElement(reply);
					}
					
					
				} // end if propose
			}// end while
			
			// if there is one best offer agent, start negotiate
			if ( successAgent != null ) {
				System.out.println("Success retailer agent:" + successAgent.getLocalName());
				printGUI("Success retailer is " + successAgent.getLocalName());
				
				// start negotiate
				System.out.println(myAgent.getLocalName() + " start negotiate with " + successAgent.getLocalName());
				printGUI(myAgent.getLocalName() + " start negotiate with " + successAgent.getLocalName());
				
				successACL.setPerformative(ACLMessage.PROPOSE);
				// can you lower by 10 percent
				successACL.setContent(String.valueOf(offerPrice*(1-0.10)));
				successACL.setConversationId("12345");
			}
									
		} // end handle all
		
		
		
		protected void handleInform(ACLMessage inform) {
			System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
		}
		
		protected void handlePropose(ACLMessage propose, Vector v) {
			System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContent());
			printGUI(myAgent.getLocalName() + " received a propose " + propose.getContent() + " from " + propose.getSender().getLocalName());
		}
		
		protected void handleRefuse(ACLMessage refuse) {
			System.out.println("Agent "+refuse.getSender().getName()+" refused");
		}
		
		protected void handleFailure(ACLMessage failure) {
			if (failure.getSender().equals(myAgent.getAMS())) {
				// FAILURE notification from the JADE runtime: the receiver
				// does not exist
				System.out.println("Responder does not exist");
			}
			else {
				System.out.println("Agent "+failure.getSender().getName()+" failed");
			}
			
		}
		
	} // end contract Net

	
	/** 
	 * Negotiate handle behaviour
	 */
	private class NegoBehaviour extends CyclicBehaviour {
		public void action() {
			
			// Receive all messages
			ACLMessage msg = myAgent.receive();
						
			// There is a message
			if (msg != null) {
				
				// get sender AID
				AID sender = msg.getSender();
				String senderName = sender.getLocalName();
				System.out.println( getLocalName() + " receives " + msg.getContent() + " from " + senderName);
				
				// get content
				String content = msg.getContent();
			
				// create a reply message
				ACLMessage reply = msg.createReply();
			
				
				// correspondent for individual response from home agent
				switch(msg.getPerformative()) {
					case ACLMessage.PROPOSE:
						double myBudget = 25.00;
						
						double offer = 0;
						try {
							offer = Double.parseDouble(content);
						}catch ( NumberFormatException nfe) {
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("NOT UNDERSTOOD");
							break;
						}
						
						// if the offer greater than budget, deny
						if ( offer > myBudget ) {
							System.out.println( getLocalName() + " reject the counter offer");
							printGUI( getLocalName() + " reject the counter offer");
							reply.setPerformative(ACLMessage.REFUSE);
							reply.setContent("Sorry, that's beyond my budget");
						}else {
							//TODO: negotiation by 10 percent
							// accept
							System.out.println( getLocalName() + " accept the offer");
							printGUI(getLocalName() + " accept the offer");
							reply.setPerformative(ACLMessage.AGREE);
							reply.setContent("Thanks for your offer.");
						}
						
					break;
				} // end switch
				
				// send back
				myAgent.send(reply);
				
			} else {
				// wait for message
				block();
			}// end if msg
		}// end action
	} // end negoBehaviour
	
	
	/**
	 * Get agent id by service type
	 * @return list of agent id
	 */
	private List<AID> getAgentsByService ( String serviceType ) {
		List<AID> aidList = new ArrayList<>();
		
		DFAgentDescription dfd = new DFAgentDescription();
   		ServiceDescription sd = new ServiceDescription();
   		sd.setType( serviceType );
		dfd.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, dfd);
			for( int i = 0; i<result.length; i++) {
				aidList.add(result[i].getName());
			}
		}catch (Exception fe) {}
      	return aidList;
	}

	private void printGUI(String text) {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME ));
		msg.setContent("<font color='blue'>" + text + "</font>");
		send(msg);
	}
	
}
