package EnergyAgents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.domain.FIPANames;


import java.util.*;

import GUI.GUIListener;
import GUI.HomeGUI;




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
    private float totalPredictedEnergyConsumption;
    private float totalActualedEnergyConsumption;
    private int applianceCount;
    private int totalAppliances;

    //The budget is set by user
    private double budgetLimit;
    private double idealBestPrice;
    private double bestPrice;
    private double negoBestPrice;

    private boolean hasNegotiationFinished;

    // Tola: check if get all appliances
    private boolean isApplianceFinished;
    
    //For waiting
    Random rand = newRandom();

    //Best offer
    private ACLMessage bestOffer;

    //Behaviours
    private SequentialBehaviour retailerSequenceBehaviour;
    private SequentialBehaviour homeSequenceBehaviour;
    /**
     * Initialize value for home agent
     */
    private void init()
    {
        this.totalPredictedEnergyConsumption = 0;
        this.totalActualedEnergyConsumption = 0;
        this.applianceCount = 0;
        this.budgetLimit = getRandomDouble(3500, 5000);
        //this.idealBestPrice = truncatedDouble( this.budgetLimit * 0.7); // 70% of the budget

        //Agent name and type
        this.agentName = "Home";
        this.agentType = "Home";

        //Conditions for communication
        this.hasNegotiationFinished = false;

        //For negotiation
        this.bestOffer = null;
        this.bestPrice = this.budgetLimit;
        //this.bestPrice = this.budgetLimit / this.totalPredictedEnergyConsumption;
    }
    
    private void resetDefault() {
        this.totalPredictedEnergyConsumption = 0;
        this.totalActualedEnergyConsumption = 0;
        this.applianceCount = 0;
        //this.idealBestPrice = this.budgetLimit * 0.7; // 70% of the budget
        
        //For negotiation
        this.bestOffer = null;
        this.bestPrice = this.budgetLimit;
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
        //this.idealBestPrice = truncatedDouble( this.budgetLimit * 0.7); // 70% of the budget
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
        
        //Register the service for Home Agent
        ServiceDescription sd = new ServiceDescription();
        sd.setType(this.agentType);
        sd.setName(this.agentName);
        register(sd);      
        
        //Declare behaviours
        homeSequenceBehaviour = new SequentialBehaviour();
        retailerSequenceBehaviour = new SequentialBehaviour();
        
        //Communicate with Appliance Agent
        //CommunicateWithAppliance(homeSequenceBehaviour);
        
        AID[] appliances = getAgentList("Appliance");
        totalAppliances = appliances.length;
        //Message template to listen only for messages matching te correct interaction protocol and performative
        MessageTemplate applianceTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        
        homeSequenceBehaviour.addSubBehaviour(new CommunicateWithApplianceBehaviour(this, applianceTemplate));
        

        addBehaviour(homeSequenceBehaviour);
    }


    /* --- Jade Agent behaviour classes --- */
    private class CommunicateWithApplianceBehaviour extends AchieveREResponder{
        public CommunicateWithApplianceBehaviour(Agent a, MessageTemplate mt){
            super(a,mt);
        }

        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            System.out.println("");
            System.out.println(getLocalName() + ": REQUEST received from "
                    + request.getSender().getLocalName() + ".\nThe demand is " + request.getContent()+ "");
            
            // Tola: reset value if talk with appliance finished
            if ( isApplianceFinished ) {
                resetDefault();
                isApplianceFinished = false;
            }
            
            // just in case content is empty
            double consume = 0;
            try {
                consume = Double.parseDouble(request.getContent());
            }catch( NumberFormatException nfe ) {}
            
            totalPredictedEnergyConsumption += consume;

            System.out.println("Total Demand: " + totalPredictedEnergyConsumption);
            ++applianceCount;
            System.out.println("Appliance Number: " + applianceCount);
            System.out.println("Appliance length: " + totalAppliances);

            printGUI(getLocalName() +" added <b>" + request.getSender().getLocalName() + "</b>  consumes <b>" + consume + "</b>, Total <b>" + totalPredictedEnergyConsumption + "</b>");
            
            // Tola: Communicate with the retailer agents if get all the demand
            if ( totalAppliances == applianceCount) {
                isApplianceFinished = true;
                CommunicateWithRetailer(retailerSequenceBehaviour);
                myAgent.addBehaviour(retailerSequenceBehaviour);
            }

            
            
            // Method to determine how to respond to request
            if (true) {
                // Agent agrees to perform the action. Note that in the FIPA-Request
                // protocol the AGREE message is optional. Return null if you
                // don't want to send it.
                System.out.println(getLocalName() + ": Agreeing to the request and responding with AGREE");
                ACLMessage agree = request.createReply();
                agree.setPerformative(ACLMessage.AGREE);
                
                
                return agree;
            } else {
                // Agent refuses to perform the action and responds with a REFUSE
                System.out.println("Agent " + getLocalName() + ": Refuse");
                throw new RefuseException("check-failed");
            }
            

        }

        // If the agent agreed to the request received, then it has to perform the associated action and return the result of the action
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
                throws FailureException {
            // Perform the action (dummy method)
            if (true) {
                System.out.println(getLocalName() + ": Action successfully performed, informing initiator");
                ACLMessage inform = request.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setContent("successfully");
                return inform;
            } else {
                // Action failed
                System.out.println(getLocalName() + ": Action failed, informing initiator");
                throw new FailureException("unexpected-error");
            }
        }
    }
    

    //Send result to appliance agents
    private class SendResultToApplianceBehaviour extends OneShotBehaviour{
        public void action(){
            System.out.println("\n**** Send Result ****");
            System.out.println("Result:"+bestPrice);
            
            printGUI("");
            printGUI("Send result, which is $"+bestPrice+" to appliance agents");

            //Send the result after finishing negotiation
            AID[] appliances = getAgentList("Appliance");
            ACLMessage resultMessage = new ACLMessage(ACLMessage.CONFIRM);
            for(AID appliance: appliances){
                resultMessage.addReceiver(appliance);
            }
            resultMessage.setContent(""+bestPrice);

            Iterator receivers = resultMessage.getAllIntendedReceiver();
            while(receivers.hasNext()) {
                System.out.println(((AID)receivers.next()).getLocalName());
            }
            send(resultMessage);
        }
    }

    //Get actual total consumption from appliance agents
    private class GetActualConsumptionBehaviour extends Behaviour {

        private MessageTemplate msgTemplate;
        private int count;
		
		public GetActualConsumptionBehaviour(Agent a, MessageTemplate msgTemplate) {
			super (a);
            this.msgTemplate = msgTemplate;
            count = 0 ;
		}
		
		@Override
		public void action() {
			System.out.println(getLocalName() + ": Waiting for acutal consumption....");

			// Retrieve message from message queue if there is
	        ACLMessage msg= receive(this.msgTemplate);
	        if (msg!=null) {
		        // Print out message content
                System.out.println(getLocalName()+ ": Received consumption " + msg.getContent() + " from " + msg.getSender().getLocalName());
                totalActualedEnergyConsumption += Double.parseDouble(msg.getContent());
                System.out.println("Total Actual consumption: "+ totalActualedEnergyConsumption);
                ++count;
            }
	        // Block the behaviour from terminating and keep listening to the message
            block();
        }
        
        @Override
        public boolean done(){
            return count == totalAppliances;
        }

        public int onEnd() {
            System.out.println(this.getBehaviourName() + ": I have finished executing");
            return super.onEnd();
        }
	}
    
    /***
     * Communicate with Retailer Agent
     * @param retailerSeQue add sequence behaviour
     */
    
    private void CommunicateWithRetailer(SequentialBehaviour retailerSeQue)
    {
        retailerSeQue.addSubBehaviour(new DelayBehaviour(this, 1000){
            public void handleElapsedTimeout(){
                System.out.println("**** NEGOTIATION **** ");
                System.out.println("Home Budget: "  + budgetLimit);
                System.out.println("Total Consumption: "  + totalPredictedEnergyConsumption);
                idealBestPrice = budgetLimit/totalPredictedEnergyConsumption;
                bestPrice = idealBestPrice;
                System.out.println("Ideal Best Price: $"  + idealBestPrice);
                System.out.println("");
                //Print to GUI
                printGUI("<font color='black'>---- NEGOTIATION ---- </font> ");
                printGUI("Home Budget: <b>$"  + budgetLimit+"</b>");
                printGUI("Total Consumption: <b>"  + totalPredictedEnergyConsumption+"</b>");
                printGUI("Ideal Best Price: $"  + idealBestPrice);
                printGUI("");
            }
        });

        //Initialize message and template for communicating with Retailer Agent
        ACLMessage messageRetailer = newMessage(ACLMessage.CFP);	
        
        MessageTemplate template = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
    
        //Get all retailer agents
        AID[] retailers = getAgentList("Retailer");
        
        System.out.println("Retailer agents found:" + retailers.length);

        /** 1ST --- Inform retailers agent to send their offer */
        for( AID retailer : retailers){
            messageRetailer.addReceiver(retailer);
            
            // Got 5s to get the receiver the offers before timeout
            retailerSeQue.addSubBehaviour( new MyReceiverBehaviour(this, 5000, template,"1ST"){
                public void handle(ACLMessage message)
                {
                    if(message!=null){
                    	// Tola: add try catche, change to double
                    	try {
                    		double offer = Double.parseDouble(message.getContent());
                    		
                    		System.out.println(myAgent.getLocalName() + " received offer $" + offer + " from " + message.getSender().getLocalName());
                    		//printGUI(myAgent.getLocalName() + " received offer <b>$" + offer + "</b> from " + message.getSender().getLocalName());
                            System.out.println( offer  + " < " + bestPrice);

                    		//Compare with budgetLimit
                            if(offer <= idealBestPrice){
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

        // Tola: print asking for propose
        printGUI("");
        printGUI(getLocalName() + " asks for a proposal, total comsuption <b>" + totalPredictedEnergyConsumption + "</b>");
        messageRetailer.setContent(String.valueOf(totalPredictedEnergyConsumption));;
        
        send(messageRetailer);
        
        /** 2ND --- Get the orders, choose the best deal and decide whether ask for a better deal */
        // Delay 2s before sending the request
        retailerSeQue.addSubBehaviour( new DelayBehaviour(this, rand.nextInt(2000)){
            public void handleElapsedTimeout()
            {
                if(bestOffer == null){
                    System.out.println("No offers.");
                    printGUI("There is no offer.");
                }
                else{
                    System.out.println("");
                    System.out.println("2ND");
                    System.out.println("Best Price $" + bestPrice + " from " + bestOffer.getSender().getLocalName());
                    System.out.println("Ideal Best Price: "  + idealBestPrice);

                    printGUI("");
                    ACLMessage reply = bestOffer.createReply();
                    if( bestPrice > idealBestPrice*0.8){//negotiate the new price if the original offer is not good (reduce 20% of ideal best price)
                    	
                        reply.setPerformative(ACLMessage.REQUEST);
                        
                        negoBestPrice = truncatedDouble( bestPrice * 0.9 );//reduce 10% of the current deal

                        reply.setContent(String.valueOf(negoBestPrice));
                        
                        printGUI("The best offer is from " + bestOffer.getSender().getLocalName() +", which is $"+ bestPrice);
                        printGUI("Ideal Best Price: $"+idealBestPrice);
                        printGUI("<font color='gray'>---- Start Negotiation -----</font>");
                        printGUI("<font color='gray' size='-1'>Stage 1:</font>");
                        System.out.println("Negotiation: Asking for price at $"+ reply.getContent());
                        printGUI(getLocalName() + " received an offer <b>$" + bestPrice + "</b> from <font color='red'>" + bestOffer.getSender().getLocalName() + "</font>");
                        printGUI(getLocalName() + " sends a new offer <b>$" + negoBestPrice + "</b>");
                        send(reply);
                    }
                    else{//Accept the original offer
                        
                        //Send agree message to retailer
                        
                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setContent(""+bestPrice);
                        System.out.println("Accept Current Offer: $"+ reply.getContent());
                        
                        // Tola: print gui
                        printGUI(getLocalName() + " accepted the offer <b>$" + bestPrice +"</b> from <font color='red'>" + bestOffer.getSender().getLocalName() + "</font>");
                        send(reply);         
                        
                        //Finish negotiation
                        hasNegotiationFinished = true;
                        negoBestPrice = bestPrice;
						System.out.println("  --------- Finished ---------\n");
						printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");
                    }
                }
            }
        });
        
        /** 3RD --- Get the counter offer if have from the retailer agent */
        //Tola : handle the counter offer
        retailerSeQue.addSubBehaviour(new MyReceiverBehaviour( this, 5000, template, "3RD" ) {
			public void handle( ACLMessage message) {
                /*
				if( bestPrice <= idealBestPrice) {
                    System.out.println("");
                    System.out.println("3RD");
                    // no need to re-negotiate
                    System.out.println("Best price ("+bestPrice+") <= ideal budget limit ("+idealBestPrice+")");
					return;
				}*/
				if (message != null ) {
                    System.out.println("");
                    System.out.println("3RD");
					//Get the counter offer from retailer
					if ( message.getPerformative() == ACLMessage.REQUEST) {
						ACLMessage reply = bestOffer.createReply();
	                    if( bestPrice > idealBestPrice ){//negotiate the new price
	                        reply.setPerformative(ACLMessage.REQUEST);
	                        try {
	                        	double offer = Double.parseDouble(bestOffer.getContent());
	                        	negoBestPrice =  (offer * getRandomDouble(0.5, 0.10))/100;	// negotiation 5% off
	                        	negoBestPrice = truncatedDouble( negoBestPrice );
	                        }catch( NumberFormatException nfe) {
                                System.out.println("Not understand");
	                        	return;
	                        }
	                    
	                        reply.setContent(String.valueOf(negoBestPrice));
	                        
	                        System.out.println("Second Negotiation: Asking for price at $"+ reply.getContent());
	                        printGUI("<font color='gray' size='-1'>Stage 2:</font>");
	                        printGUI(getLocalName() + " sends the second offer <b>$" + negoBestPrice + "</b>");
	                        send(reply);
	                    }
	                    else{//Accept current offer
	                        reply.setPerformative(ACLMessage.AGREE);
	                        reply.setContent(""+bestPrice);
	                        System.out.println("Accept the second offer: $"+ reply.getContent());
	                        // Tola: print gui
                            printGUI(getLocalName() + " accepted the second offer");
                            //Finish nego
                            hasNegotiationFinished = true;
	                    	negoBestPrice = bestPrice;
	                        send(reply);                        
	                    }

						try {
							double offer = Double.parseDouble(message.getContent());
						}catch ( NumberFormatException nfe) {
							return;
						}
						
					// retailer agree the first offer
					} else if ( message.getPerformative() == ACLMessage.AGREE) {
						System.out.println("The Second Proposal Accepted");
						System.out.println("The Second Proposal Offer: $"+ negoBestPrice);
                        printGUI(getLocalName() + "'s second offer is accepted, which is <b>$" + negoBestPrice+"</b>");
                        //Finish nego
                        hasNegotiationFinished = true;
                        bestPrice = negoBestPrice;
						System.out.println("  --------- Finished ---------\n");
						printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");
						
					// retailer refuse the first offer
					} else if ( message.getPerformative() == ACLMessage.REFUSE) {
                        //Print out 
						System.out.println("The Second Propsal Refused");
                        System.out.println("The Second Offer: $"+bestPrice);
                        printGUI(getLocalName() + "'s second offer is rejected, which is <b>$" + negoBestPrice+"</b>");
                        //TODo: If possible do 1 more stage
                        ACLMessage thirdMessage = bestOffer.createReply();
                        thirdMessage.setPerformative(ACLMessage.REQUEST);
                        negoBestPrice = truncatedDouble( negoBestPrice + negoBestPrice * 0.1); //increase 10%
                        thirdMessage.setContent(""+negoBestPrice);
                        printGUI("<font color='gray' size='-1'>Stage 2:</font>");
                        System.out.println(getLocalName()+ "send the third offer, which is " + negoBestPrice);
                        printGUI(getLocalName()+" send the third offer, which is <b>$" + negoBestPrice+"</b>");
                        send(thirdMessage);
					}
				}else{
                    System.out.println("3RD: message is null");
                }
			}
		}); // end counter behaviour

        // 4TH --- Final decision 
        // have 3s before timeout
        retailerSeQue.addSubBehaviour( new MyReceiverBehaviour( this, 7000, template, "4TH") 
				{
					public void handle( ACLMessage message) 
					{  
						if (message != null ) {
                            System.out.println("");
                            System.out.println("4TH");
							System.out.println("Got " + 
								ACLMessage.getPerformative(message.getPerformative() ) +
								" from " + message.getSender().getLocalName());
                                
                            ACLMessage fourthMessage = bestOffer.createReply();

							if( message.getPerformative() == ACLMessage.AGREE){
                                System.out.println("Proposal Accepted");
                                System.out.println("Proposal Offer: $"+ negoBestPrice);
                                printGUI(bestOffer.getSender().getLocalName() + " accepted the offer for <b>$" + negoBestPrice+"</b>");

                                //Assign new best price
                                bestPrice = negoBestPrice;
                                
                                fourthMessage.setContent(""+negoBestPrice);
                                fourthMessage.setPerformative(ACLMessage.AGREE);
                                send(fourthMessage);
                            }
							else{//Refuse the proposal
                                System.out.println("Propsal Refused");
                                System.out.println("Original Offer: $"+bestPrice);
                                printGUI(bestOffer.getSender().getLocalName()+ " reject the offer for <b>$" + negoBestPrice+"</b>");
                                printGUI(getLocalName() +" accepts the original offer, which is <b>$"+ bestPrice+"</b>");

                                fourthMessage.setContent(""+bestPrice);
                                fourthMessage.setPerformative(ACLMessage.AGREE);
                                send(fourthMessage);
                            }

                            hasNegotiationFinished = true;
							System.out.println("  --------- Finished ---------\n");
							printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");
						} 
						else {
							System.out.println("No message for 4th round");
						}
					}	
                });
                
        //Print the summary   
        retailerSeQue.addSubBehaviour(new OneShotBehaviour(){
            public void action(){
                if(hasNegotiationFinished){
                    System.out.println("Negotiation finished");
                    System.out.println("Total Predicted Comsumption: "+ totalPredictedEnergyConsumption);
                    System.out.println("Best Price: $"+ bestPrice);
                    System.out.println("Amount of paid money: $" + (totalPredictedEnergyConsumption*bestPrice));
                    budgetLimit = budgetLimit - (totalPredictedEnergyConsumption*bestPrice);
                    System.out.println("Remaining Budget: $"+ budgetLimit);

                    printGUI("");
                    printGUI("<font color='black'>---- SUMMARY ----</font>");
                    printGUI("Total Predicted Comsumption: "+ totalPredictedEnergyConsumption);
                    printGUI("Best Price: $"+ bestPrice);
                    printGUI("Amount of paid money: $" + (totalPredictedEnergyConsumption*bestPrice));
                    printGUI("Remaining Budget: $"+ budgetLimit);
                }else{
                    System.out.println("Negotiation has not finished yet!!");
                }
            }
        });
    
        
        //Send result back to the retailer agent
        retailerSeQue.addSubBehaviour(new SendResultToApplianceBehaviour());


        MessageTemplate actualMessageTemplate =MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
        //Receive actual consumption
        //retailerSeQue.addSubBehaviour(new GetActualConsumptionBehaviour(this, actualMessageTemplate));
    }

    


    


    
    /*---- Ultility methods to access DF ---- */
    
    /**
     * Test and remove old duplicate DF entries before add new one
     * @param sd
     */
    private void register(ServiceDescription sd)
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

    //
    /**
     * Search service from DF
     * @param serviceType
     * @return list of agents
     */
    private AID[] getAgentList(String serviceType)
    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxDepth(new Long(0));

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
	private void printGUIClean() {
		ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
		msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME ));
		msg.setContent("");
		send(msg);
	}





    /* --- Utility methods --- */
    protected static int cidCnt = 0;
    String cidBase;

    /**
     * This method is used to extract the consumption based on the total demand
     * @param demand 
     * @param duartion
     * @return
     */
    private double extractToBestPrice(double demand, double budget){
        double bestPrice;
        bestPrice = budget / demand;
        return bestPrice;
    }


    /**
     * This method is used to generate unique ID for each conversations
     * @return
     */
    private String generateCID()
    {
        if(cidBase==null){
            cidBase = getLocalName() + hashCode() + System.currentTimeMillis()%10000 + "_";
        }
        return cidBase + (cidCnt++);
    }

    /**
     * This method is used to generate unique Random generator
     * @return
     */
    private Random newRandom()
    {
        return new Random(hashCode() + System.currentTimeMillis());
    }

    /**
     * This method is used to initialize ACLMessages
     * @param perf perfomative (Ex: AGREE, PROPOSAL, REQUEST)
     * @param content
     * @param destination
     * @return
     */
    private ACLMessage newMessage(int perf, String content, AID destination)
    {
        ACLMessage message = newMessage(perf);
        if ( destination != null){
            message.addReceiver(destination);
        }
        message.setContent(content);
        return message;
    }
    
    /**
     * 
     * @param perf
     * @return
     */
    private ACLMessage newMessage(int perf)
    {
        ACLMessage message = new ACLMessage(perf);
        message.setConversationId(generateCID());
        return message;
    }
    
    /**
     * Get a random number between two numbers
     * @param min
     * @param max
     * @return random number 
     */
    public double getRandomDouble(double min, double max){
	    double d = (Math.random()*((max-min)+1))+min;
	    return Math.round(d*100.0)/100.0;
	}
    
    /**
     * 
     * @param value
     * @return round to 2 decimal numbers
     */
    private double truncatedDouble(double value) {
		return java.math.BigDecimal.valueOf(value).setScale(3, java.math.RoundingMode.HALF_UP).doubleValue();
	}
}