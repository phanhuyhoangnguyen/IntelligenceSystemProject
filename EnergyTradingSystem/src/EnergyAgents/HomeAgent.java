package EnergyAgents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;



/**
 * Home Agent 
 * @author The 'Dangerous' Dave
 * 
 * @Description This agent will calculate the demand/ supplys, negotiate with Retailer agent and decide to choose the right offers.
 */

public class HomeAgent extends Agent
{
    //Variables for agents (Dynamic)
    private float totalEnergyConsumption;
    private float budgetLimit;
    private String agentName;
    private String agentType;

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

    public HomeAgent()
    {
        init();
    }
    /**
     * Set Up Home Agent
     */
    @Override
    protected void setup(){
        this.agentName += "_" + getAID().getLocalName();
        System.out.println(agentName + ": created.");
        
        //Register the service
        ServiceDescription sd = new ServiceDescription();
        sd.setType(this.agentType);
        sd.setName(this.agentName);
        register(sd);
        
        

        //Add behaviours
        //addBehaviour(new ReceiveDemand());
        addBehaviour(new TestBehaviour());
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

    /**
     * Receive demand from Appliances
     */
    private class ReceiveDemand extends CyclicBehaviour{
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
    private class negotiateContract extends CyclicBehaviour{
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
}