package EnergyAgents;

import jade.core.Agent;
import jade.core.behaviours.*;

/**
 * This class is used for delay when the behaviour STARTS not CREATED
 * @author The 'Dangerous' Dave
 */
public class DelayBehaviour extends SimpleBehaviour 
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