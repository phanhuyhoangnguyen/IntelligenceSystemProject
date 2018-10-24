package EnergyAgents;

import jade.core.Agent;
import jade.core.behaviours.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * MyReceiverBehaviour  
 * @author The 'Dangerous' Dave
 *
 * @Description this behaviour controls the characteristic of behaviour as well as it life time
 */
public class MyReceiverBehaviour extends SimpleBehaviour
{
    private MessageTemplate template;
    private long timeOut, wakeupTime;
    private boolean finished;
    private ACLMessage message;
    private String behaviourName;

    public ACLMessage getMessage(){
        return message;
    }
    public MyReceiverBehaviour(Agent a, int millis, MessageTemplate mt, String name){
        super(a);
        timeOut = millis;
        template = mt;
        behaviourName = name;
    }

    public void onStart()
    {
        wakeupTime = (timeOut < 0 ? Long.MAX_VALUE:System.currentTimeMillis() + timeOut);
    }

    public boolean done()
    {
        System.out.println(behaviourName+" has done.");
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
        /**can be redfined  */
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