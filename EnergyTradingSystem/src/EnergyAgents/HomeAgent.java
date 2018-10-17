package EnergyAgents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.util.*;

import GUI.GUIListener;
import GUI.HomeGUI;
import GUI.RetailerGUIDetails;



/**
 * Home Agent 
 * @author The 'Dangerous' Dave
 * 
 * @Description This agent will calculate the demand/ supplys, negotiate with Retailer agent and decide to choose the right offers.
 */

public class HomeAgent extends Agent implements GUIListener
{
    /*Variables for agents (Dynamic)*/
    // Agent Identification
    private String agentName;
    private String agentType;

    //Total energy consumption get from appliance agent
    private float totalEnergyConsumption;

    //The budget is set by user
    private double budgetLimit;
    private double duration;
    private double idealBudgetLimit;
    private double bestPriceProposal;
    private double bestPrice;
    private double negoBestPrice;

    //For waiting
    Random rand = newRandom();

    //Best offer
    private ACLMessage bestOffer;
    private ACLMessage message;

    /**
     * Initialize value for home agent
     */
    private void init()
    {
        this.totalEnergyConsumption = 200;
        this.duration = 1;
        this.budgetLimit = Math.round(Math.random()*51) + 20;	// from 20 - 50

        agentName = "Home";
        agentType = "Home";

        this.bestOffer = null;
        this.bestPrice = extractToBestPrice(this.totalEnergyConsumption, this.duration);
        this.idealBudgetLimit = this.budgetLimit * 0.7;
    }
    /**
     * End of initialize value for home agent
     */

    
    /**
     * Getter and Setter
      */
    public double getBudgetLimit()
    {
        return this.budgetLimit;
    }

    public void setBudgetLimit(double newBudgetLimit)
    {
        this.budgetLimit = newBudgetLimit;
    }
     /**
      * End of Getter and Setter
      */

    public HomeAgent()
    {
        init();
        
     // Register the interface that must be accessible by an external program through the O2A interface
     registerO2AInterface(GUIListener.class, this);
    }

    /* --- Jade functions --- */
    /**
     * Set Up Home Agent
     */
    @Override
    protected void setup(){
        this.agentName += "_" + getAID().getLocalName();
        System.out.println(agentName + " " + agentType + ": created.");
        
        //Register the service
        ServiceDescription sd = new ServiceDescription();
        sd.setType(this.agentType);
        sd.setName(this.agentName);
        register(sd);
        
        //initialize message and template
        message = newMessage(ACLMessage.CFP);	// Tola: change to CFP
        //MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId(message.getConversationId() ));
        MessageTemplate template = MessageTemplate.MatchAll();
        
        //Add behaviours
        //addBehaviour(new ReceiveDemand());
        //addBehaviour(new TestBehaviour());
        SequentialBehaviour seq = new SequentialBehaviour();
        addBehaviour(seq);
        
        //Add parallel behaviour to handle conversations
        //WHEN_ALL: terminiate the behaviour when all its children are done
        ParallelBehaviour par = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);

        seq.addSubBehaviour(par);

        //Get all retailer agents
        AID[] aids = getRetailerAgents("Retailer");
        //AID[] aids = getRetailerAgents("Test");
        
        System.out.println("Retailer agents total number:" + aids.length);
        //Get offer from retailer agents
        for( AID aid : aids){
            message.addReceiver(aid);
            // Tola: print added agent
            printGUI(getLocalName() + " add <font color='red'>" + aid.getLocalName() + "</font>");
            
            // Got 5s to get the receiver the offers before timeout
            par.addSubBehaviour( new MyReceiverBehaviour(this, 5000, template){
                public void handle(ACLMessage message)
                {
                    if(message!=null){
                    	// Tola: add try catche, change to double
                    	try {
                    		double offer = Double.parseDouble(message.getContent());
                    		
                    		System.out.println(myAgent.getLocalName() + " received offer $" + offer + " from " + message.getSender().getLocalName());
                    		printGUI(myAgent.getLocalName() + " received offer <b>" + offer + "</b> from " + message.getSender().getLocalName());
                            
                    		//Compare with budgetLimit
                            if(offer < bestPrice){
                                bestPrice = offer;// set new better limit
                                bestOffer = message;
                            }
                    	}catch(NumberFormatException nfe) {
                    		// nfe.printStackTrace();
                    		System.out.println("There is no offer");
                    	}
                    }
                }
            });
        }
        
        //Handle request 
        // Delay 2s before sending the request
        seq.addSubBehaviour( new DelayBehaviour(this, rand.nextInt(2000)){
            public void handleElapsedTimeout()
            {
                if(bestOffer == null){
                    System.out.println("No offers.");
                }
                else{
                    System.out.println("Best Price $" + bestPrice + " from " + bestOffer.getSender().getLocalName());

                    System.out.println(getLocalName() + "'s ideal budget is " + idealBudgetLimit);

                    ACLMessage reply = bestOffer.createReply();
                    if( bestPrice > idealBudgetLimit){//negotiate the new price
                    	
                        reply.setPerformative(ACLMessage.REQUEST);
                        
                        negoBestPrice = truncatedDouble( bestPrice * 0.9 );

                        reply.setContent(String.valueOf(negoBestPrice));
                        
                        printGUI("<font color='gray'>---- Start Negotiation -----</font>");
                        printGUI("<font color='gray' size='-1'>Step 1:</font>");
                        System.out.println("Negotiation: Asking for price at $"+ reply.getContent());
                        printGUI(getLocalName() + " received an offer <b>" + bestPrice + "</b> from <font color='red'>" + bestOffer.getSender().getLocalName() + "</font>");
                        printGUI(getLocalName() + " sends a new offer <b>" + negoBestPrice + "</b>");
                        send(reply);
                    }
                    else{//Accept current offer
                    	negoBestPrice = bestPrice;
                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setContent(""+bestPrice);
                        System.out.println("Accept Current Offer: $"+ reply.getContent());
                        // Tola: print gui
                        printGUI(getLocalName() + " accepted the offer <b>" + bestPrice +"</b> from <font color='red'>" + bestOffer.getSender().getLocalName() + "</font>");
                        send(reply);                        
                    }
                }
            }
        });
        
        
        //Tola : handle the counter offer
        MyReceiverBehaviour counterOfferBehaviour = new MyReceiverBehaviour( this, 3000, null ) {
			public void handle( ACLMessage message) {
				if( bestPrice <= idealBudgetLimit) {
					// no need to re-negotiate
					return;
				}
				if (message != null ) {
					// counter offer
					if ( message.getPerformative() == ACLMessage.REQUEST) {
						ACLMessage reply = bestOffer.createReply();
	                    if( bestPrice > idealBudgetLimit){//negotiate the new price
	                        reply.setPerformative(ACLMessage.REQUEST);
	                        try {
	                        	double offer = Double.parseDouble(bestOffer.getContent());
	                        	negoBestPrice =  (offer * getRandomDouble(0.5, 0.10))/100;	// negotiation 5% off
	                        	negoBestPrice = truncatedDouble( negoBestPrice );
	                        }catch( NumberFormatException nfe) {
	                        	return;
	                        }
	                    
	                        reply.setContent(String.valueOf(negoBestPrice));
	                        
	                        System.out.println("Second Negotiation: Asking for price at $"+ reply.getContent());
	                        printGUI("<font color='gray' size='-1'>Step 2:</font>");
	                        printGUI(getLocalName() + " sends the second offer <b>" + negoBestPrice + "</b>");
	                        send(reply);
	                    }
	                    else{//Accept current offer
	                    	negoBestPrice = bestPrice;
	                        reply.setPerformative(ACLMessage.AGREE);
	                        reply.setContent(""+bestPrice);
	                        System.out.println("Accept the second offer: $"+ reply.getContent());
	                        // Tola: print gui
	                        printGUI(getLocalName() + " accepted the second offer");
	                        send(reply);                        
	                    }

						try {
							double offer = Double.parseDouble(message.getContent());
						}catch ( NumberFormatException nfe) {
							return;
						}
						
					// retailer agree
					} else if ( message.getPerformative() == ACLMessage.AGREE) {
						System.out.println("The Second Propsal Accepted");
						System.out.println("The Second Proposal Offer: $"+ negoBestPrice);
                        printGUI(getLocalName() + " accepted the second offer for " + negoBestPrice);
						System.out.println("  --------- Finished ---------\n");
						printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");
						
					// retailer refuse
					} else if ( message.getPerformative() == ACLMessage.REFUSE) {
						System.out.println("The Second Propsal Refused");
                        System.out.println("The Second Offer: $"+bestPrice);
                        printGUI(getLocalName() + " reject the second offer for " + negoBestPrice);
						System.out.println("  --------- Finished ---------\n");
						printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");
					}
				}
			}
		}; // end counter behaviour
		seq.addSubBehaviour(counterOfferBehaviour);

        //Get decision from retailers
        // have 3s before timeout
        seq.addSubBehaviour( new MyReceiverBehaviour( this, 3000,
                null) 
				{
					public void handle( ACLMessage message) 
					{  
						if (message != null ) {
							System.out.println("Got " + 
								ACLMessage.getPerformative(message.getPerformative() ) +
								" from " + message.getSender().getLocalName());
							
							if( message.getPerformative() == ACLMessage.AGREE){
                                System.out.println("Proposal Accepted");
                                System.out.println("Proposal Offer: $"+ negoBestPrice);
                                printGUI(getLocalName() + " accepted the offer for " + negoBestPrice);
                            }
							else{//Refuse the proposal
                                System.out.println("Propsal Refused");
                                System.out.println("Original Offer: $"+bestPrice);
                                printGUI(getLocalName() + " reject the offer for " + negoBestPrice);
                            }
							System.out.println("  --------- Finished ---------\n");
							printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");
						} 
						else {
							//System.out.println("==" + getLocalName() +" timed out");
							//setup();//loop ultil get the order
						}
					}	
				});
        send(message);
    }

    /**
     * Clean up when agent die
     */
    @Override
    protected void takeDown()
    {
        //Deregister from DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            //TODO: handle exception
            fe.printStackTrace();
        }

        System.out.println(agentName + ": closed.");
    }
    /*---- Ultility methods to access DF ---- */
    
    /**
     * Test and remove old duplicate DF entries before add new one
     * @param sd
     */
    void register(ServiceDescription sd)
    {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        try {
            DFAgentDescription list[] = DFService.search(this, dfd);
            if(list.length > 0){
                DFService.deregister(this);
            }
            dfd.addServices(sd);
            DFService.register(this, dfd);      
        } catch (FIPAException fe) {
            //TODO: handle exception
            fe.printStackTrace();
        }
    }

    //Search service from DF
    AID[] getRetailerAgents(String serviceType)
    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxDepth(new Long(0));//what the hell is it

        try {
            DFAgentDescription[] result = DFService.search(this, dfd);
            AID[] agents = new AID[result.length];
            for( int i =0 ; i < result.length; i++)
                agents[i] = result[i].getName();
            return  agents;
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;        
    }


    /* --- Jade Agent behaviour classes --- */
    /**
     * Receive demand from Appliances
     */
    private class ReceiveDemandBehaviour extends CyclicBehaviour{
        public void action(){
            System.out.println(getLocalName() + ": waiting for demand from Applicant Agents");
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);
            if(msg!= null){
                String demand, receiver, sender;
                
                demand = msg.getContent();
                receiver = getLocalName();
                sender = msg.getSender().getLocalName();

                //Print message content
                System.out.println(receiver + ": received response " + demand + " from " + sender);
                totalEnergyConsumption += Float.parseFloat(demand);
                System.out.println("Total consumption: " + totalEnergyConsumption);
                System.out.println("**********************************************");
            }
            //Continue listening
            block();
        }
    }
    //TODO: start from here
    /**
     * Negotitate with Retailer Agents
     */
    private class negotiateContractBehaviour extends CyclicBehaviour{
        public void action(){
            System.out.println(getLocalName() + ": begin negotiate with Retailer Agents ");
            ACLMessage msg = receive();

            if(msg != null){
                //get sender information
                String offer, receiver, sender;
                
                offer = msg.getContent();
                receiver = getLocalName();
                sender = msg.getSender().getLocalName();

                System.out.println(receiver + ": received response " + offer + " from " + sender);
                
            }
            //Continue listening 
            block();
        }
    }

    /**
     * Test Behaviour
     */

    private class TestBehaviour extends OneShotBehaviour
    {
        public void action(){
            AID[] aids = getRetailerAgents("Retailer");
            System.out.println("Retailer agents total number:" + aids.length);
            for(AID aid : aids){
                System.out.println(aid.getLocalName());
                System.out.println("----------------------------------------");
            
                //Send message
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(aid);
                msg.setContent("Hello");
                myAgent.send(msg);
            }
        }
    }


    //Declare ReiverBehaviour - this behaviour controls the characteristic of behaviour as well as it life time
    private class MyReceiverBehaviour extends SimpleBehaviour
    {
        private MessageTemplate template;
        private long timeOut, wakeupTime;
        private boolean finished;
        private ACLMessage message;

        public ACLMessage getMessage(){
            return message;
        }
        public MyReceiverBehaviour(Agent a, int millis, MessageTemplate mt){
            super(a);
            timeOut = millis;
            template = mt;
        }

        public void onStart()
        {
            wakeupTime = (timeOut < 0 ? Long.MAX_VALUE:System.currentTimeMillis() + timeOut);
        }

        public boolean done()
        {
            return finished;
        }

        public void action()
        {
            /**Check whether tempalte is available */
            if(template == null){
                message = myAgent.receive();
            }
            else{
                message = myAgent.receive(template);
            }

            if(message != null){
                finished = true;
                handle(message);
                return;
            }

            long dt = wakeupTime - System.currentTimeMillis();
            if( dt > 0){
                block(dt);
            }
            else{
                finished = true;
                handle(message);
            }
        }

        public void handle(ACLMessage m){
            /**can be redfined in sub_class */
        }

        /**Rest the behaviour */
        public void reset(){
            message = null;
            finished = false;
            super.reset();
        }

        public void reset(int dt){
            timeOut = dt;
            reset();
        }
    }

    //This method is used for delay when the behaviour STARTs
    private class DelayBehaviour extends SimpleBehaviour 
    {
        private long    timeout, 
                        wakeupTime;
        private boolean finished = false;
        
        public DelayBehaviour(Agent a, long timeout) {
            super(a);
            this.timeout = timeout;
        }
        
        public void onStart() {
            wakeupTime = System.currentTimeMillis() + timeout;
        }
            
        public void action() 
        {
            long dt = wakeupTime - System.currentTimeMillis();
            if (dt <= 0) {
                finished = true;
                handleElapsedTimeout();
            } else 
                block(dt);
                
        } //end of action
        
        protected void handleElapsedTimeout() // by default do nothing !
            { } 
                    
        public boolean done() { return finished; }
    }

    /* --- GUI --- */
    // Tola : add GUI
    @Override
	public void showGUI() {
		HomeGUI gui = new HomeGUI(this);
		gui.showGUI();
	}
	
    
	private void printGUI(String text) {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME ));
		msg.setContent("<font color='blue'>" + text + "</font>");
		send(msg);
	}
    

    /* --- Utility methods --- */
    protected static int cidCnt = 0;
    String cidBase;

    //This method is used to extract the consumption based on the total demand
    private double extractToBestPrice(double demand, double duartion){
        double bestPrice;
        bestPrice = demand / duartion;
        return bestPrice;
    }

    //This method is used to generate unique ID for each conversations
    private String generateCID()
    {
        if(cidBase==null){
            cidBase = getLocalName() + hashCode() + System.currentTimeMillis()%10000 + "_";
        }
        return cidBase + (cidCnt++);
    }

    //This method is used to generate unique Random generator
    private Random newRandom()
    {
        return new Random(hashCode() + System.currentTimeMillis());
    }

    //This method is used to initialize ACLMessages
    private ACLMessage newMessage(int perf, String content, AID destination)
    {
        ACLMessage message = newMessage(perf);
        if ( destination != null){
            message.addReceiver(destination);
        }
        message.setContent(content);
        return message;
    }
    private ACLMessage newMessage(int perf)
    {
        ACLMessage message = new ACLMessage(perf);
        message.setConversationId(generateCID());
        return message;
    }
    
    public double getRandomDouble(double min, double max){
	    double d = (Math.random()*((max-min)+1))+min;
	    return Math.round(d*100.0)/100.0;
	}
    
    private double truncatedDouble(double value) {
		return java.math.BigDecimal.valueOf(value).setScale(3, java.math.RoundingMode.HALF_UP).doubleValue();
	}
    
}