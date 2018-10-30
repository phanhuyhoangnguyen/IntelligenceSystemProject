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

    private double spentMoney;// The used money
    
    private double idealBestPrice; // the best price that home agent wish
    private double bestPrice; // the best price from the best offer
    private double negoBestPrice;// get the new offer price based on the current offer

    //To check whether negotiation is finished
    private boolean hasNegotiationFinished;

    // Tola: check if get all appliances
    private boolean isApplianceFinished;

    // Best offer
    private ACLMessage bestOffer;

    // Behaviours
    private SequentialBehaviour retailerSequenceBehaviour;
    private SequentialBehaviour homeSequenceBehaviour;

    /**
     * Initialize value for home agent
     */
    private void init() {
        this.totalPredictedEnergyConsumption = 0;
        this.totalActualedEnergyConsumption = 0;
        this.applianceCount = 0;
        this.budgetLimit = getRandomDouble(3500, 5000);

        // Agent name and type
        this.agentName = "Home";
        this.agentType = "Home";

        // Conditions for communication
        this.hasNegotiationFinished = false;

        // For negotiation
        this.bestOffer = null;
    }

    private void resetDefault() {
        this.totalPredictedEnergyConsumption = 0;
        this.totalActualedEnergyConsumption = 0;
        this.applianceCount = 0;
        this.budgetLimit = getRandomDouble(3500, 5000);

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
        ServiceDescription sd = new ServiceDescription();
        sd.setType(this.agentType);
        sd.setName(this.agentName);
        register(sd);

        // Declare behaviours
        homeSequenceBehaviour = new SequentialBehaviour();
        retailerSequenceBehaviour = new SequentialBehaviour();

        // Communicate with Appliance Agent
        // CommunicateWithAppliance(homeSequenceBehaviour);

        AID[] appliances = getAgentList("Appliance");
        totalAppliances = appliances.length;
        // Message template to listen only for messages matching te correct interaction
        // protocol and performative
        MessageTemplate applianceTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        homeSequenceBehaviour.addSubBehaviour(new CommunicateWithApplianceBehaviour(this, applianceTemplate));

        addBehaviour(homeSequenceBehaviour);
    }

    /* --- Jade Agent behaviour classes --- */
    private class CommunicateWithApplianceBehaviour extends AchieveREResponder {
        public CommunicateWithApplianceBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
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
                    + "</b>, Total <b>" + totalPredictedEnergyConsumption + "</b>");

            // Tola: Communicate with the retailer agents if get all the demand
            if (totalAppliances == applianceCount) {
                isApplianceFinished = true;
                CommunicateWithRetailer(retailerSequenceBehaviour);
                myAgent.addBehaviour(retailerSequenceBehaviour);
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

        // If the agent agreed to the request received, then it has to perform the
        // associated action and return the result of the action
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
                throws FailureException {
            System.out.println(getLocalName() + ": Action successfully performed, informing initiator");
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent("successfully");
            return inform;
        }
    }

    // Send result to appliance agents
    private class SendResultToApplianceBehaviour extends OneShotBehaviour {
        public void action() {
            System.out.println("\n**** Send Result ****");
            System.out.println("Result:" + bestPrice);

            printGUI("");
            printGUI("Send result, which is <b>$" + bestPrice + "</b> to appliance agents");

            // Send the result after finishing negotiation
            AID[] appliances = getAgentList("Appliance");
            ACLMessage resultMessage = new ACLMessage(ACLMessage.CONFIRM);
            for (AID appliance : appliances) {
                resultMessage.addReceiver(appliance);
            }
            resultMessage.setContent("" + bestPrice);

            Iterator receivers = resultMessage.getAllIntendedReceiver();
            while (receivers.hasNext()) {
                System.out.println(((AID) receivers.next()).getLocalName());
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
                printGUI("Total Actual Consumption: <b>" + totalActualedEnergyConsumption + "</b>");
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
     * @param retailerSeQue add sequence behaviour
     */

    private void CommunicateWithRetailer(SequentialBehaviour retailerSeQue) {
        retailerSeQue.addSubBehaviour(new DelayBehaviour(this, 1000) {
            public void handleElapsedTimeout() {
                System.out.println("**** NEGOTIATION **** ");
                System.out.println("Home Budget: " + budgetLimit);
                System.out.println("Total Consumption: " + totalPredictedEnergyConsumption);

                
                idealBestPrice = budgetLimit / totalPredictedEnergyConsumption;//Set the ideal Best price once had the total predicted energy consumption
                bestPrice = idealBestPrice; // assign ideal best price to best price

                System.out.println("Ideal Best Price: $" + idealBestPrice);
                System.out.println("");
                // Print to GUI
                printGUI("<font color='black'>---- NEGOTIATION ---- </font> ");
                printGUI("Home Budget: <b>$" + budgetLimit + "</b>");
                printGUI("Total Consumption: <b>" + totalPredictedEnergyConsumption + "</b>");
                printGUI("Ideal Best Price: <b>$" + truncatedDouble(idealBestPrice)+"</b>");
                printGUI("");
            }
        });

        // Initialize message and template for communicating with Retailer Agent
        ACLMessage messageRetailer = newMessage(ACLMessage.CFP);

        MessageTemplate negoTemplate = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);

        // Get all retailer agents
        AID[] retailers = getAgentList("Retailer");

        /** 1ST --- Inform retailers agent to send their offer */
        for (AID retailer : retailers) {
            messageRetailer.addReceiver(retailer);

            // Got 5s to get the receiver the offers before timeout
            retailerSeQue.addSubBehaviour(new MyReceiverBehaviour(this, 0, negoTemplate, "1ST") {
                public void handle(ACLMessage message) {
                    if (message != null) {
                        // Tola: add try catche, change to double
                        try {
                            double offer = Double.parseDouble(message.getContent());

                            System.out.println(myAgent.getLocalName() + " received offer $" + offer + " from "
                                    + message.getSender().getLocalName());
                            // printGUI(myAgent.getLocalName() + " received offer <b>$" + offer + "</b> from
                            // " + message.getSender().getLocalName());
                            System.out.println(offer + " < " + bestPrice);

                            // Compare with budgetLimit
                            if (offer <= bestPrice) {
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
        printGUI(getLocalName() + " asks for a proposal, total comsuption <b>" + totalPredictedEnergyConsumption
                + "</b>");
        messageRetailer.setContent(String.valueOf(totalPredictedEnergyConsumption));
        ;

        send(messageRetailer);
        /**
         * 2ND --- Get the orders, choose the best deal and decide whether ask for a
         * better deal
         */
        // Delay 3s before sending the request
        retailerSeQue.addSubBehaviour(new DelayBehaviour(this, 3000) {
            public void handleElapsedTimeout() {
                if (bestOffer == null) {
                    System.out.println("No offers.");
                } else {
                    System.out.println("");
                    System.out.println("2ND");
                    System.out.println("Best Price $" + bestPrice + " from " + bestOffer.getSender().getLocalName());
                    System.out.println("Ideal Best Price: " + idealBestPrice);

                    printGUI("");
                    ACLMessage reply = bestOffer.createReply();
                    if (bestPrice > idealBestPrice * 0.8) {// negotiate the new price if the original offer is not good
                                                           // (reduce 20% of ideal best price)

                        reply.setPerformative(ACLMessage.REQUEST);

                        negoBestPrice = truncatedDouble(bestPrice * 0.9);// reduce 10% of the current deal

                        reply.setContent(String.valueOf(negoBestPrice));

                        printGUI("The best offer is from " + bestOffer.getSender().getLocalName() + ", which is $"
                                + bestPrice);
                        printGUI("Ideal Best Price: $" + idealBestPrice);
                        printGUI("<font color='gray'>---- Start Negotiation -----</font>");
                        printGUI("<font color='gray' size='-1'>Stage 1:</font>");
                        System.out.println("Negotiation: Asking for price at $" + reply.getContent());
                        printGUI(getLocalName() + " received an offer <b>$" + bestPrice + "</b> from <font color='red'>"
                                + bestOffer.getSender().getLocalName() + "</font>");
                        printGUI(getLocalName() + " sends a new offer <b>$" + negoBestPrice + "</b>");
                        send(reply);
                    } else {// Accept the original offer

                        // Send agree message to retailer

                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setContent("" + bestPrice);
                        System.out.println("Accept Current Offer: $" + reply.getContent());

                        // Tola: print gui
                        printGUI(getLocalName() + " accepted the offer <b>$" + bestPrice
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
        retailerSeQue.addSubBehaviour(new MyReceiverBehaviour(this, 0, negoTemplate, "3RD") {
            public void handle(ACLMessage message) {
                if (message != null && !hasNegotiationFinished) {
                    System.out.println("");
                    System.out.println("3RD");
                    // Get the counter offer from retailer
                    if (message.getPerformative() == ACLMessage.REQUEST) {
                        ACLMessage reply = message.createReply();
                        if (bestPrice > idealBestPrice) {// negotiate the new price
                            reply.setPerformative(ACLMessage.REQUEST);
                            try {
                                double offer = Double.parseDouble(bestOffer.getContent());
                                negoBestPrice = (offer * getRandomDouble(0.5, 0.10)) / 100; // negotiation 5% off
                                negoBestPrice = truncatedDouble(negoBestPrice);
                            } catch (NumberFormatException nfe) {
                                System.out.println("Not understand");
                                return;
                            }

                            reply.setContent(String.valueOf(negoBestPrice));

                            System.out.println("Second Negotiation: Asking for price at $" + reply.getContent());
                            printGUI("<font color='gray' size='-1'>Stage 2:</font>");
                            printGUI(getLocalName() + " sends the second offer <b>$" + negoBestPrice + "</b>");
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
                                getLocalName() + "'s second offer is accepted, which is <b>$" + negoBestPrice + "</b>");
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
                        // TODo: If possible do 1 more stage
                        ACLMessage thirdMessage = message.createReply();
                        thirdMessage.setPerformative(ACLMessage.REQUEST);
                        negoBestPrice = truncatedDouble(negoBestPrice + negoBestPrice * 0.1); // increase 10%
                        thirdMessage.setContent("" + negoBestPrice);
                        printGUI("<font color='gray' size='-1'>Stage 2:</font>");
                        System.out.println(getLocalName() + "send the third offer, which is " + negoBestPrice);
                        printGUI(getLocalName() + " send the third offer, which is <b>$" + negoBestPrice + "</b>");
                        send(thirdMessage);
                    }
                } else {
                    System.out.println("3RD: message is null");
                }
            }
        }); // end counter behaviour

        // 4TH --- Final decision
        // have 3s before timeout
        retailerSeQue.addSubBehaviour(new MyReceiverBehaviour(this, 0, negoTemplate, "4TH") {
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
                        printGUI(bestOffer.getSender().getLocalName() + " accepted the offer for <b>$" + negoBestPrice
                                + "</b>");

                        // Assign new best price
                        bestPrice = negoBestPrice;

                        fourthMessage.setContent("" + negoBestPrice);
                        fourthMessage.setPerformative(ACLMessage.AGREE);
                        send(fourthMessage);
                    } else {// Refuse the proposal
                        System.out.println("Propsal Refused");
                        System.out.println("Original Offer: $" + bestPrice);
                        printGUI(bestOffer.getSender().getLocalName() + " reject the offer for <b>$" + negoBestPrice
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
                    System.out.println("No message for 4th round");
                }
            }
        });

        // Print the summary
        retailerSeQue.addSubBehaviour(new OneShotBehaviour() {
            public void action() {
                if (hasNegotiationFinished && bestOffer != null) {
                    System.out.println("Negotiation finished");
                    System.out.println("Total Predicted Comsumption: " + totalPredictedEnergyConsumption);
                    System.out.println("Best Price: $" + bestPrice);

                    spentMoney = totalPredictedEnergyConsumption*bestPrice;// calculate spent money after get the best offer
                    System.out.println("Amount of spent money: $" + spentMoney);

                    remainingBudget = budgetLimit - spentMoney;// calculate the budget remaining
                    System.out.println("Remaining Budget: $" + budgetLimit);

                    printGUI("");
                    printGUI("<font color='black'>---- SUMMARY ----</font>");
                    printGUI("Total Predicted Comsumption: " + totalPredictedEnergyConsumption);
                    printGUI("Best Price: <b>$" + bestPrice + "</b>");
                    printGUI("Best Offer: <b>"+ bestOffer.getSender().getLocalName()+"</b>");
                    printGUI("Amount of spent money: <b>$" + truncatedDouble(spentMoney) + "</b>");
                    printGUI("Remaining Budget: <b>$" + truncatedDouble(remainingBudget) + "</b>");
                } else {
                    System.out.println("Negotiation has not finished yet!!");
                }
            }
        });
       
        // Send result back to the retailer agent
        retailerSeQue.addSubBehaviour(new SendResultToApplianceBehaviour());

        MessageTemplate actualMessageTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
        // Receive actual consumption
        retailerSeQue.addSubBehaviour(new GetActualConsumptionBehaviour(this, actualMessageTemplate));

        // Send the actual consumption to retailer agent
        retailerSeQue.addSubBehaviour(new DelayBehaviour(this, 1000) {
            public void handleElapsedTimeout() {
                if (totalActualedEnergyConsumption != 0) {
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

        // Get the overcharge price and final report
        retailerSeQue.addSubBehaviour(new MyReceiverBehaviour(this, 2000, negoTemplate, "Get Overcharge Price Stage") {
            public void handle(ACLMessage message){
                if(message != null && bestOffer!=null && hasNegotiationFinished){
                    System.out.println("\nOvercharge Stage");
                    System.out.println(getLocalName() + ": received overcharge price, which is $"+message.getContent()+ " from " + message.getSender().getLocalName());

                    double overchargePrice = Double.parseDouble(message.getContent());
                    System.out.println("---------- Final Report ----------");
                    printGUI("\n<font color='black'>---- Final Report ----</font>");
                    printGUI("Total Predicted Comsumption: " + totalPredictedEnergyConsumption);
                    printGUI("Total Actual Comsumption: " + totalActualedEnergyConsumption);
                    printGUI("Best Price: <b>$" + bestPrice + "</b>");
                    printGUI("Best Offer: <b>"+ bestOffer.getSender().getLocalName()+"</b>");

                    System.out.println("Total Predicted Comsumption: " + totalPredictedEnergyConsumption);
                    System.out.println("Total Actual Comsumption: " + totalActualedEnergyConsumption);
                    if(overchargePrice != 0){// get overchage
                        //Before Overcharge GUI
                        printGUI("");
                        printGUI("<b>Before adding overcharge </b>");
                        printGUI("Overcharge Price: <b>$" + overchargePrice + "</b>");
                        printGUI("Amount of spent money: <b>$" + truncatedDouble(spentMoney) + "</b>");
                        printGUI("Remaining Budget: <b>$" + truncatedDouble(remainingBudget) + "</b>");
                        
                        
                        System.out.println("Overcharge Price: $"+overchargePrice);
                        spentMoney = spentMoney + overchargePrice;
                        System.out.println("Amount of spent money: $"+spentMoney);
                        remainingBudget = budgetLimit - spentMoney;
                        System.out.println("Remaining Budget: $"+remainingBudget);

                        printGUI("");
                        printGUI("<b>After adding overcharge </b>");
                        printGUI("Amount of spent money: <b>$" + truncatedDouble(spentMoney) + "</b>");
                        printGUI("Remaining Budget: <b>$" + truncatedDouble(remainingBudget) + "</b>");
                        
                        
                    }else{// No overcharge
                        System.out.println("No Overcharge");
                        System.out.println("Amount of spent money: $"+spentMoney);
                        System.out.println("Remaining Budget: $"+remainingBudget);
                        
                        //Print GUI
                        printGUI("<b>No overcharge </b>");
                        printGUI("Amount of spent money: <b>$" + truncatedDouble(spentMoney) + "</b>");
                        printGUI("Remaining Budget: <b>$" + truncatedDouble(remainingBudget) + "</b>");
                    }
                }
                else{
                    System.out.println("No Overcharge Message");
                }
            }
        });
        
    }

    /*---- Ultility methods to access DF ---- */

    /**
     * Test and remove old duplicate DF entries before add new one
     * 
     * @param sd
     */
    private void register(ServiceDescription sd) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        try {
            DFAgentDescription list[] = DFService.search(this, dfd);
            if (list.length > 0) {
                DFService.deregister(this);
            }
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            // TODO: handle exception
            fe.printStackTrace();
        }
    }

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
            // TODO: handle exception
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

    /* --- Utility methods --- */
    protected static int cidCnt = 0;
    String cidBase;

    /**
     * This method is used to generate unique ID for each conversations
     * 
     * @return
     */
    private String generateCID() {
        if (cidBase == null) {
            cidBase = getLocalName() + hashCode() + System.currentTimeMillis() % 10000 + "_";
        }
        return cidBase + (cidCnt++);
    }

    /**
     * This method is used to initialize ACLMessages
     * 
     * @param perf        perfomative (Ex: AGREE, PROPOSAL, REQUEST)
     * @param content
     * @param destination
     * @return
     */
    private ACLMessage newMessage(int perf, String content, AID destination) {
        ACLMessage message = newMessage(perf);
        if (destination != null) {
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
    private ACLMessage newMessage(int perf) {
        ACLMessage message = new ACLMessage(perf);
        message.setConversationId(generateCID());
        return message;
    }

    /**
     * Get a random number between two numbers
     * 
     * @param min
     * @param max
     * @return random number
     */
    public double getRandomDouble(double min, double max) {
        double d = (Math.random() * ((max - min) + 1)) + min;
        return Math.round(d * 100.0) / 100.0;
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