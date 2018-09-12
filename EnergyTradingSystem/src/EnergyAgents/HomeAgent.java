package EnergyAgents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * This agent will calculate the demand/ supplys, negotiate with Retailer agent and decide to choose the right offers.
 */
public class HomeAgent extends Agent
{
    protected void setup(){
        System.out.println(getLocalName() + ": created");
        //Add behaviours
        addBehaviour(new ReceiveTotalDemand());
    }

    private class ReceiveTotalDemand extends CyclicBehaviour{
        public void action(){
            System.out.println(getLocalName() + ": waiting for message");
            ACLMessage msg = receive();
            if(msg!= null){
                //Print message content
                System.out.println(getLocalName() + ": received response " + msg.getContent() + " from " + msg.getSender().getLocalName());
            }
            //Continue listening
            block();
        }
    }
}