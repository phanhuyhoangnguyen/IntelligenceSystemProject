package EnergyAgents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.proto.AchieveREResponder;
import jade.domain.FIPANames;

import java.util.*;

import GUI.GUIListener;
import GUI.HomeGUI;

/**
 * Home Agent
 * 
 * @author The 'Dangerous' Dave
 *
 * @Description This agent will calculate the demand/ supplys, negotiate with
 *              Retailer agent and decide to choose the right offers.
 */

public class HomeAgent extends Agent implements GUIListener {
    /* Variables for agents (Dynamic) */
    // Agent Identification
    private String agentName;
    private String agentType;

    // Total energy consumption get from appliance agent
    private float totalPredictedEnergyConsumption;
    private double totalActualedEnergyConsumption;
    private int applianceCount;
    private int totalAppliances;

    // The budget is set by user
    private double budgetLimit;//   budget of home agent
    private double remainingBudget;// The remaining budget

    private double predictedPayment;// The predicted payment
    private double actualPayment;// The actual payment
    
    private double maximumPrice; // the best price that home agent wish
    private double bestPrice; // the best price from the best offer
    private double negoBestPrice;// get the new offer price based on the current offer

    //To check whether negotiation is finished
    private boolean hasNegotiationFinished;
    private boolean areAbleToNegotitate;

    // Tola: check if get all appliances
    private boolean isApplianceFinished;

    // Best offer
    private ACLMessage bestOffer;

    // Behaviours
    private SequentialBehaviour retailerSequentialBehaviour;

    /**
     * Initialize value for home agent
     */
    private void init() {
        this.totalPredictedEnergyConsumption = 0;
        this.totalActualedEnergyConsumption = 0;
        this.applianceCount = 0;
        this.totalAppliances = 0;
        this.budgetLimit = Utilities.getRandomDouble(2000, 1000);
        this.maximumPrice = Utilities.truncatedDouble(Utilities.getRandomDouble(25, 30)/100);

        // Agent name and type
        this.agentName = "Home";
        this.agentType = "Home";

        // Conditions for communication
        this.hasNegotiationFinished = false;
        this.areAbleToNegotitate = true;
        // For negotiation
        this.bestOffer = null;
    }

    private void resetDefault() {
        this.totalPredictedEnergyConsumption = 0;
        this.totalActualedEnergyConsumption = 0;
        this.applianceCount = 0;
        this.predictedPayment = 0;
        this.actualPayment = 0;
        this.budgetLimit = this.remainingBudget;// set to remaining budget
        

        // Conditions for communication
        this.hasNegotiationFinished = false;

        // For negotiation
        this.bestOffer = null;
    }

    /**
     * End of initialize value for home agent
     */

    /**
     * Getter and Setter
     */
    public double getBudgetLimit() {
        return this.budgetLimit;
    }

    public void setBudgetLimit(double newBudgetLimit) {
        this.budgetLimit = newBudgetLimit;
    }


    public double getMaximumPrice(){
        return this.maximumPrice;
    }

    public void setMaximumPrice(double newPrice){
        this.maximumPrice = newPrice; 
    }
    /**
     * End of Getter and Setter
     */

    public HomeAgent() {
        init();

        // Register the interface that must be accessible by an external program through
        // the O2A interface
        registerO2AInterface(GUIListener.class, this);
    }

    /* --- Jade functions --- */
    /**
     * Set Up Home Agent
     */
    @Override
    protected void setup() {
        this.agentName += "_" + getAID().getLocalName();
        System.out.println(agentName + " " + agentType + ": created.");

        // Register the service for Home Agent
        Utilities.registerService(this, this.agentName, this.agentType);

        retailerSequentialBehaviour = new SequentialBehaviour();

        AID[] appliances = getAgentList("Appliance");
        totalAppliances = appliances.length;
        // Message template to listen only for messages matching te correct interaction
        // protocol and performative
        MessageTemplate applianceTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        addBehaviour(new CommunicateWithApplianceBehaviour(this, applianceTemplate));

    }

    /* --- Jade Agent behaviour classes --- */
    private class CommunicateWithApplianceBehaviour extends AchieveREResponder {
        public CommunicateWithApplianceBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            if(areAbleToNegotitate){//budget is enough
                System.out.println("");
                System.out.println(getLocalName() + ": REQUEST received from " + request.getSender().getLocalName()
                        + ".\nThe received demand is " + request.getContent() + "");

                // Tola: reset value if talk with appliance finished
                
                if (isApplianceFinished) {
                    resetDefault();
                    isApplianceFinished = false;
                }

                // just in case content is empty
                double consume = 0;
                try {
                    consume = Double.parseDouble(request.getContent());
                } catch (NumberFormatException nfe) {
                }

                totalPredictedEnergyConsumption += consume;

                System.out.println("Total Demand of all appliances is: " + totalPredictedEnergyConsumption);
                ++applianceCount;

                printGUI(getLocalName() + " added <b>" + request.getSender().getLocalName() + "</b>  consumes <b>" + consume
                        + "</b>, Total <b>" + Utilities.truncatedDouble(totalPredictedEnergyConsumption) + "</b>");

                // Tola: Communicate with the retailer agents if get all the demand
                if (totalAppliances == applianceCount) {
                    isApplianceFinished = true;
                    CommunicateWithRetailer(retailerSequentialBehaviour);
                    myAgent.addBehaviour(retailerSequentialBehaviour);
                }
                
                // Respond to the request
                // Agent agrees to perform the action. Note that in the FIPA-Request
                // protocol the AGREE message is optional. Return null if you
                // don't want to send it.
                System.out.println(getLocalName() + ": Agreeing to the request and responding with AGREE");
                ACLMessage agree = request.createReply();
                agree.setPerformative(ACLMessage.AGREE);
                return agree;
            }
            else{
                printGUI("The budget is insufficient");
                System.out.println("The budget is insufficient");
                throw new RefuseException("check-failed");
            }
        }

        // If the agent agreed to the request received, then it has to perform the
        // associated action and return the result of the action
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
                throws FailureException {
            if(areAbleToNegotitate){
                return response;
            }else{
                System.out.println(getLocalName() + ": refusing to the request and responding with REFUSE");
                ACLMessage refuse = request.createReply();
                refuse.setPerformative(ACLMessage.REFUSE);
                return refuse;
            }
        }
    }

    // Send result to appliance agents
    private class SendResultToApplianceBehaviour extends OneShotBehaviour {
        public void action() {
            // Send the result after finishing negotiation
            AID[] appliances = getAgentList("Appliance");
            ACLMessage resultMessage = new ACLMessage(ACLMessage.CONFIRM);
            for (AID appliance : appliances) {
                resultMessage.addReceiver(appliance);
            }
            System.out.println("\n**** Send Result ****");
            if(bestOffer!=null){// has offer and send result back 
                System.out.println("Result:" + bestPrice);
    
                printGUI("");
                printGUI("Send result, which is <b>$" +Utilities.truncatedDouble( bestPrice) + "</b> to appliance agents");
    
                resultMessage.setContent("" + bestPrice);
    
                Iterator receivers = resultMessage.getAllIntendedReceiver();
                while (receivers.hasNext()) {
                    System.out.println(((AID) receivers.next()).getLocalName());
                }
            }
            else{//send failure if there is no a 
                System.out.println("Result: NO OFFER");
                resultMessage.setContent("failure");
                printGUI("");
                printGUI("Budget is not enough");
                Iterator receivers = resultMessage.getAllIntendedReceiver();
                while (receivers.hasNext()) {
                    System.out.println(((AID) receivers.next()).getLocalName());
                } 
            }
            send(resultMessage);
        }
    }

    // Get actual total consumption from appliance agents
    private class GetActualConsumptionBehaviour extends Behaviour {

        private MessageTemplate msgTemplate;
        private int count;

        public GetActualConsumptionBehaviour(Agent a, MessageTemplate msgTemplate) {
            super(a);
            this.msgTemplate = msgTemplate;
            count = 0;
        }

        @Override
        public void action() {
            System.out.println("\nGET ACTUAL CONSUMPTION: " + getLocalName() + ": Waiting for acutal consumption....");

            // Retrieve message from message queue if there is
            ACLMessage msg = receive(this.msgTemplate);
            if (msg != null) {
                // Print out message content
                System.out.println("GET ACTUAL CONSUMPTION: " + getLocalName() + ": Received consumption "
                        + msg.getContent() + " from " + msg.getSender().getLocalName());
                totalActualedEnergyConsumption += Double.parseDouble(msg.getContent());
                System.out.println("TOTAL ACTUAL CONSUMPTION: " + totalActualedEnergyConsumption);
                System.out.println("Count: " + count);
                ++count;
            } else {
                // Block the behaviour from terminating and keep listening to the message
                block();
            }
            // Print out the total consumption
            if (count == totalAppliances) {
                printGUI("Total Actual Consumption: <b>" + Utilities.truncatedDouble(totalActualedEnergyConsumption) + "</b>");
            }
        }

        @Override
        public boolean done() {
            return count == totalAppliances;
        }

    }

    /***
     * Communicate with Retailer Agent
     * 
     * @param sequentialBehaviour add sequence behaviour
     */

    private void CommunicateWithRetailer(SequentialBehaviour sequentialBehaviour) {
        sequentialBehaviour.addSubBehaviour(new DelayBehaviour(this, 1000) {
            public void handleElapsedTimeout() {
                System.out.println("**** NEGOTIATION **** ");
                System.out.println("Home Budget: " + budgetLimit);
                System.out.println("Total Predicted Consumption: " + totalPredictedEnergyConsumption);

                
                //maximumPrice = Utilities.truncatedDouble(budgetLimit / totalPredictedEnergyConsumption); //Set the ideal Best price once had the total predicted energy consumption
                // Tola: @Dave the idea price should depend on the market, not your budget
                bestPrice = maximumPrice; // assign maximumPrice to best price

                System.out.println("Ideal Best Price: $" + maximumPrice);
                System.out.println("");
                // Print to GUI
                printGUI("<font color='black'>---- NEGOTIATION ---- </font> ");
                printGUI("Home Budget: <b>$" + Utilities.truncatedDouble(budgetLimit) + "</b>");
                printGUI("Total Predicted Consumption: <b>" + Utilities.truncatedDouble(totalPredictedEnergyConsumption) + "</b>");
                printGUI("Ideal Best Price: <b>$" + Utilities.truncatedDouble(maximumPrice)+"</b>");
                printGUI("");

                //If exceed the money
                if((budgetLimit-(maximumPrice*totalPredictedEnergyConsumption))<0){
                    areAbleToNegotitate = false;
                }
            }
        });

        // Initialize message and template for communicating with Retailer Agent
        ACLMessage messageRetailer = Utilities.newMessage(ACLMessage.CFP);

        MessageTemplate negoTemplate = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);

        // Get all retailer agents
        AID[] retailers = getAgentList("Retailer");

        /** 1ST --- Inform retailers agent to send their offer */
        for (AID retailer : retailers) {
            messageRetailer.addReceiver(retailer);

            // Got 100ms to get the receiver the offers before timeout
            sequentialBehaviour.addSubBehaviour(new MyReceiverBehaviour(this, 100, negoTemplate, "1ST") {
                public void handle(ACLMessage message) {
                    if (message != null) {
                        // Tola: add try catche, change to double
                        try {
                            double offer = Double.parseDouble(message.getContent());

                            System.out.println(myAgent.getLocalName() + " received offer $" + offer + " from "
                                    + message.getSender().getLocalName());
                            System.out.println(offer + " < " + bestPrice);

                            // Compare with budgetLimit
                            if (offer <= bestPrice && areAbleToNegotitate) {
                                bestPrice = offer;// set new better limit
                                bestOffer = message;
                            }
                        } catch (NumberFormatException nfe) {
                            // nfe.printStackTrace();
                            System.out.println("There is no offer");
                        }
                    }
                }
            });
        }

        // Tola: print asking for propose
        printGUI("");
        printGUI(getLocalName() + " asks for a proposal, total comsuption <b>" + Utilities.truncatedDouble(totalPredictedEnergyConsumption)+ "</b>");
        messageRetailer.setContent(String.valueOf(totalPredictedEnergyConsumption));
        ;

        send(messageRetailer);
        /**
         * 2ND --- Get the orders, choose the best deal and decide whether ask for a
         * better deal
         */
        // Delay 2s before sending the request
        sequentialBehaviour.addSubBehaviour(new DelayBehaviour(this, 2000) {
            public void handleElapsedTimeout() {
                if (bestOffer == null) {
                    System.out.println("No offers.");
                    areAbleToNegotitate = false;
                    printGUI("There is no suitable offer.");
                } else {
                    System.out.println("");
                    System.out.println("2ND");
                    System.out.println("Best Price $" + bestPrice + " from " + bestOffer.getSender().getLocalName());
                    System.out.println("Ideal Best Price: " + maximumPrice);

                    printGUI("");
                    ACLMessage reply = bestOffer.createReply();
                    if (bestPrice > maximumPrice * 0.8) {// negotiate the new price if the original offer is not good
                                                           // (reduce 20% of ideal best price)

                        reply.setPerformative(ACLMessage.REQUEST);

                        negoBestPrice = Utilities.truncatedDouble(bestPrice * 0.8);// reduce 20% of the current deal

                        reply.setContent(String.valueOf(negoBestPrice));

                        printGUI("The best offer is from " + bestOffer.getSender().getLocalName() + ", which is <b>$"
                                + Utilities.truncatedDouble(bestPrice)+"</b>");
                        printGUI("Ideal Best Price: <b>$" + Utilities.truncatedDouble(maximumPrice)+"</b>");
                        printGUI("<font color='gray'>---- Start Negotiation -----</font>");
                        printGUI("<font color='gray' size='-1'>Stage 1:</font>");
                        System.out.println("Negotiation: Asking for price at $" + reply.getContent());
                        printGUI(getLocalName() + " received an offer <b>$" + Utilities.truncatedDouble(bestPrice )+ "</b> from <font color='red'>"
                                + bestOffer.getSender().getLocalName() + "</font>");
                        printGUI(getLocalName() + " sends a new offer <b>$" + Utilities.truncatedDouble(negoBestPrice) + "</b>");
                        send(reply);
                    } else {// Accept the original offer

                        // Send agree message to retailer

                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setContent("" + bestPrice);
                        System.out.println("Accept Current Offer: $" + reply.getContent());

                        // Tola: print gui
                        printGUI(getLocalName() + " accepted the offer <b>$" + Utilities.truncatedDouble(bestPrice)
                                + "</b> from <font color='red'>" + bestOffer.getSender().getLocalName() + "</font>");
                        send(reply);

                        // Finish negotiation
                        hasNegotiationFinished = true;
                        negoBestPrice = bestPrice;
                        System.out.println("  --------- Finished ---------\n");
                        printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");
                    }
                }
            }
        });

        /** 3RD --- Get the counter offer if have from the retailer agent */
        // Tola : handle the counter offer
        // have 3s before timeout
        sequentialBehaviour.addSubBehaviour(new MyReceiverBehaviour(this, 3000, negoTemplate, "3RD") {
            public void handle(ACLMessage message) {
                if (message != null && !hasNegotiationFinished) {
                    System.out.println("");
                    System.out.println("3RD");
                    // Get the counter offer from retailer
                    if (message.getPerformative() == ACLMessage.REQUEST) {
                        ACLMessage reply = message.createReply();
                        if (bestPrice > maximumPrice) {// negotiate the new price
                            reply.setPerformative(ACLMessage.REQUEST);
                            try {
                                double offer = Double.parseDouble(bestOffer.getContent());
                                negoBestPrice = (offer * Utilities.getRandomDouble(0.5, 0.10)) / 100; // negotiation 5% - 10% off
                                negoBestPrice = Utilities.truncatedDouble(negoBestPrice);
                            } catch (NumberFormatException nfe) {
                                System.out.println("Not understand");
                                return;
                            }

                            reply.setContent(String.valueOf(negoBestPrice));

                            System.out.println("Second Negotiation: Asking for price at $" + reply.getContent());
                            printGUI("<font color='gray' size='-1'>Stage 2:</font>");
                            printGUI(getLocalName() + " sends the second offer <b>$" + Utilities.truncatedDouble(negoBestPrice) + "</b>");
                            send(reply);
                        } else {// Accept current offer
                            reply.setPerformative(ACLMessage.AGREE);
                            reply.setContent("" + bestPrice);
                            System.out.println("Accept the second offer: $" + reply.getContent());
                            // Tola: print gui
                            printGUI(getLocalName() + " accepted the second offer");
                            // Finish nego
                            hasNegotiationFinished = true;
                            negoBestPrice = bestPrice;
                            send(reply);
                        }

                        try {
                            double offer = Double.parseDouble(message.getContent());
                        } catch (NumberFormatException nfe) {
                            return;
                        }

                        // retailer agree the first offer
                    } else if (message.getPerformative() == ACLMessage.AGREE) {
                        System.out.println("The Second Proposal Accepted");
                        System.out.println("The Second Proposal Offer: $" + negoBestPrice);
                        printGUI(
                                getLocalName() + "'s second offer is accepted, which is <b>$" + Utilities.truncatedDouble(negoBestPrice) + "</b>");
                        // Finish nego
                        hasNegotiationFinished = true;
                        bestPrice = negoBestPrice;
                        System.out.println("  --------- Finished ---------\n");
                        printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");

                        // retailer refuse the first offer
                    } else if (message.getPerformative() == ACLMessage.REFUSE) {
                        // Print out
                        System.out.println("The Second Propsal Refused");
                        System.out.println("The Second Offer: $" + bestPrice);
                        printGUI(
                                getLocalName() + "'s second offer is rejected, which is <b>$" + negoBestPrice + "</b>");

                        ACLMessage thirdMessage = message.createReply();
                        thirdMessage.setPerformative(ACLMessage.REQUEST);
                        negoBestPrice = Utilities.truncatedDouble(negoBestPrice + negoBestPrice * 0.05); // increase 5%
                        thirdMessage.setContent("" + negoBestPrice);
                        printGUI("<font color='gray' size='-1'>Stage 2:</font>");
                        System.out.println(getLocalName() + "send the third offer, which is " + negoBestPrice);
                        printGUI(getLocalName() + " send the third offer, which is <b>$" + Utilities.truncatedDouble(negoBestPrice) + "</b>");
                        send(thirdMessage);
                    }
                } else {
                    System.out.println("3RD: !Negotitaion: "+!hasNegotiationFinished);
                    System.out.println("3RD: message is null");
                }
            }
        }); // end counter behaviour

        // 4TH --- Final decision
        // have 5s before timeout
        sequentialBehaviour.addSubBehaviour(new MyReceiverBehaviour(this, 5000, negoTemplate, "4TH") {
            public void handle(ACLMessage message) {
                if (message != null &&  !hasNegotiationFinished) {
                    System.out.println("");
                    System.out.println("4TH");
                    System.out.println("Got " + ACLMessage.getPerformative(message.getPerformative()) + " from "
                            + message.getSender().getLocalName());

                    ACLMessage fourthMessage = message.createReply();

                    if (message.getPerformative() == ACLMessage.AGREE) {
                        System.out.println("Proposal Accepted");
                        System.out.println("Proposal Offer: $" + negoBestPrice);
                        printGUI(bestOffer.getSender().getLocalName() + " accepted the offer for <b>$" + Utilities.truncatedDouble(negoBestPrice)
                                + "</b>");

                        // Assign new best price
                        bestPrice = negoBestPrice;

                        fourthMessage.setContent("" + negoBestPrice);
                        fourthMessage.setPerformative(ACLMessage.AGREE);
                        send(fourthMessage);
                    } else {// Refuse the proposal
                        System.out.println("Propsal Refused");
                        System.out.println("Original Offer: $" + bestPrice);
                        printGUI(bestOffer.getSender().getLocalName() + " reject the offer for <b>$" + Utilities.truncatedDouble(negoBestPrice)
                                + "</b>");
                        printGUI(getLocalName() + " accepts the original offer, which is <b>$" + bestPrice + "</b>");

                        fourthMessage.setContent("" + bestPrice);
                        fourthMessage.setPerformative(ACLMessage.AGREE);
                        send(fourthMessage);
                    }

                    hasNegotiationFinished = true;
                    System.out.println("  --------- Finished ---------\n");
                    printGUI("<font color='gray'>---- Finished the Negotiation -----</font>");
                } else {
                    System.out.println("4TH: !Negotitaion: "+!hasNegotiationFinished);
                    System.out.println("4TH: message is null");
                }
            }
        });

        // Print the summary
        sequentialBehaviour.addSubBehaviour(new OneShotBehaviour() {
            public void action() {
                if (hasNegotiationFinished && bestOffer != null) {
                    System.out.println("Negotiation finished");
                    System.out.println("Total Predicted Comsumption: " + totalPredictedEnergyConsumption);
                    System.out.println("Best Price: $" + bestPrice);

                    predictedPayment = totalPredictedEnergyConsumption*bestPrice;// calculate spent money after get the best offer
                    System.out.println("Amount of predicted payment: $" + predictedPayment);


                    printGUI("");
                    printGUI("<font color='black'>---- SUMMARY ----</font>");
                    printGUI("Total Predicted Comsumption: " + Utilities.truncatedDouble(totalPredictedEnergyConsumption));
                    printGUI("Best Price: <b>$" + Utilities.truncatedDouble(bestPrice) + "</b>");
                    printGUI("Best Offer: <b>"+ bestOffer.getSender().getLocalName()+"</b>");
                    printGUI("Amount of predicted payment: <b>$" + Utilities.truncatedDouble(predictedPayment) + "</b>");
                } else {
                    System.out.println("Negotiation has not finished yet!!");
                }
            }
        });
        // Send result back to the retailer agent
        sequentialBehaviour.addSubBehaviour(new SendResultToApplianceBehaviour());

        MessageTemplate actualMessageTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
        // Receive actual consumption from appliances
        sequentialBehaviour.addSubBehaviour(new GetActualConsumptionBehaviour(this, actualMessageTemplate));

        // Send the actual consumption to retailer agent
        // have 1s before timeout
        sequentialBehaviour.addSubBehaviour(new DelayBehaviour(this, 1000) {
            public void handleElapsedTimeout() {
                if (totalActualedEnergyConsumption != 0 && bestOffer!=null) {
                    if (bestOffer != null) {
                        ACLMessage actualConsumptionMessage = bestOffer.createReply();
                        actualConsumptionMessage.setPerformative(ACLMessage.QUERY_REF);
                        actualConsumptionMessage.setContent("" + totalActualedEnergyConsumption);
                        System.out.println("\nSend actual usage to Retailer Agent: " + totalActualedEnergyConsumption);
                        send(actualConsumptionMessage);
                    } else {
                        System.out.println("The offer is null");
                    }
                } else {
                    System.out.println("The actual consumption is 0");
                }
            }
        });

        // Get the overcharge price from retailer agent and prompt the final report
        // Have 2s before timeout
        // Tola: match this template
        MessageTemplate overchargeTemplate = MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF);
        sequentialBehaviour.addSubBehaviour(new MyReceiverBehaviour(this, 2000, overchargeTemplate, "Get Overcharge Price Stage") {
            public void handle(ACLMessage message){
                if(message != null && bestOffer!=null && hasNegotiationFinished){
                    System.out.println("\nOvercharge Stage");
                    System.out.println(getLocalName() + ": received overcharge price, which is $"+message.getContent()+ " from " + message.getSender().getLocalName());

                    double overchargePrice = Double.parseDouble(message.getContent());
                    System.out.println("---------- Final Report ----------");
                    printGUI("\n<font color='black'>---- Final Report ----</font>");
                    printGUI("Total Predicted Comsumption: " + Utilities.truncatedDouble(totalPredictedEnergyConsumption));
                    printGUI("Total Actual Comsumption: " + Utilities.truncatedDouble(totalActualedEnergyConsumption));
                    printGUI("Best Price: <b>$" + Utilities.truncatedDouble(bestPrice) + "</b>");
                    printGUI("Best Offer: <b>"+ bestOffer.getSender().getLocalName()+"</b>");

                    System.out.println("Total Predicted Comsumption: " + totalPredictedEnergyConsumption);
                    System.out.println("Total Actual Comsumption: " + totalActualedEnergyConsumption);

                    //Calculate the actual payment
                    actualPayment = bestPrice * totalActualedEnergyConsumption;
                    remainingBudget = budgetLimit - actualPayment;

                    //Overacharge part
                    if(overchargePrice != 0){// get overchage
                        //Before Overcharge GUI
                        printGUI("");
                        printGUI("<b>Before adding overcharge </b>");
                        printGUI("Overcharge Price: <b>$" + Utilities.truncatedDouble(overchargePrice) + "</b>");
                        printGUI("Amount of predicted payment: <b>$" + Utilities.truncatedDouble(predictedPayment) + "</b>");
                        printGUI("Amount of actual payment: <b>$" + Utilities.truncatedDouble(actualPayment) + "</b>");
                        printGUI("Remaining Budget: <b>$" + Utilities.truncatedDouble(remainingBudget) + "</b>");
                        
                        
                        System.out.println("Overcharge Price: $"+overchargePrice);
                        actualPayment = actualPayment + overchargePrice;
                        System.out.println("Amount of actual payment: $"+actualPayment);
                        remainingBudget = budgetLimit - actualPayment;
                        System.out.println("Remaining Budget: $"+remainingBudget);

                        printGUI("");
                        printGUI("<b>After adding overcharge </b>");
                        printGUI("Amount of actual payment: <b>$" + Utilities.truncatedDouble(actualPayment) + "</b>");
                        printGUI("Remaining Budget: <b>$" + Utilities.truncatedDouble(remainingBudget) + "</b>");
                        
                        
                    }else{// No overcharge
                        System.out.println("No Overcharge");
                        System.out.println("Amount of actual payment: $"+actualPayment);
                        System.out.println("Remaining Budget: $"+remainingBudget);
                        
                        //Print GUI
                        printGUI("<b>No overcharge </b>");
                        printGUI("Amount of actual payment: <b>$" + Utilities.truncatedDouble(actualPayment) + "</b>");
                        printGUI("Remaining Budget: <b>$" + Utilities.truncatedDouble(remainingBudget) + "</b>");
                    }
                    printGUI("");
                    printGUI("NEXT ROUND");
                    printGUI("");
                }
                else{
                    System.out.println("No Overcharge Message");
                }
            }
        });
        
    }



    
    /*---- Ultility methods to access DF ---- */
    //
    /**
     * Search service from DF
     * 
     * @param serviceType
     * @return list of agents
     */
    private AID[] getAgentList(String serviceType) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxDepth(new Long(0));

        try {
            DFAgentDescription[] result = DFService.search(this, dfd);
            AID[] agents = new AID[result.length];
            for (int i = 0; i < result.length; i++)
                agents[i] = result[i].getName();
            return agents;
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }

    /**
     * Clean up when agent die
     */
    @Override
    protected void takeDown() {
        // Deregister from DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
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
        msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME));
        msg.setContent("<font color='blue'>" + text + "</font>");
        send(msg);
    }

    private void printGUIClean() {
        ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
        msg.addReceiver(new AID(PrintAgent.AGENT_NAME, AID.ISLOCALNAME));
        msg.setContent("");
        send(msg);
    }

}