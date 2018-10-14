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



/**
 * Home Agent 
 * @author The 'Dangerous' Dave
 * 
 * @Description This agent will calculate the demand/ supplys, negotiate with Retailer agent and decide to choose the right offers.
 */

public class HomeAgent extends Agent
{
    /*Variables for agents (Dynamic)*/
    // Agent Identification
    private String agentName;
    private String agentType;

    //Total energy consumption get from appliance agent
    private float totalEnergyConsumption;

    //The budget is set by user
    private double budgetLimit;

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
        this.totalEnergyConsumption = 0;
        this.budgetLimit = 75;
        agentName = "Home";
        agentType = "Home";
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
        
        message = newMessage(ACLMessage.QUERY_REF);


        //Add behaviours
        //addBehaviour(new ReceiveDemand());
        addBehaviour(new TestBehaviour());
        SequentialBehaviour seq = new SequentialBehaviour();
        addBehaviour(seq);
        
        //Add parallel behaviour to handle conversations
        //WHEN_ALL: terminiate the behaviour when all its children are done
        ParallelBehaviour par = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);

        seq.addSubBehaviour(par);

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

    


    /* --- GUI --- */
    //TODO: Add GUI

    /* --- Utility methods --- */
    protected static int cidCnt = 0;
    String cidBase;

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
}