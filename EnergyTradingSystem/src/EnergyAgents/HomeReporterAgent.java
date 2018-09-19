package EnergyAgents;

import java.util.Iterator;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;

/**
 * This agent will get the all the demands from AAs and then combine and send the total demand to home agent for further actions
 */

public class HomeReporterAgent extends Agent
{
    //The total demand
    private float totalDemand = 14;

    protected void setup(){
        System.out.println(getLocalName() + ": I have been created");
        //Add behaviours
        addBehaviour(new SendTotalDemand());
    }

    //Send the total demand to Home Agent
    private class SendTotalDemand extends CyclicBehaviour{
        SendTotalDemand(){
            System.out.println(getBehaviourName() + ": I have been created.");
        }

        public void action(){
            
            if(totalDemand != 0){
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(Float.toString(totalDemand));
                msg.addReceiver(new AID("HomeAgent", AID.ISLOCALNAME ));

                //Send message (only once)
                System.out.println(getLocalName()+ ": Sending message " + msg.getContent() + " to ");

                Iterator receivers = msg.getAllIntendedReceiver();
                while(receivers.hasNext()){
                    System.out.println(((AID)receivers.next()).getLocalName());
                }

                send(msg);

                totalDemand = 0;
            }
            block();
        }
    }

}