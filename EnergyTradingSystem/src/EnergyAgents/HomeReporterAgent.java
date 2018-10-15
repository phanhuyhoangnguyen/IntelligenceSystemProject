package EnergyAgents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;


import java.util.*;

public class HomeReporterAgent extends Agent{
    Random  rnd = newRandom();	
	MessageTemplate query  = MessageTemplate.MatchPerformative
                                ( ACLMessage.QUERY_REF );
                                
	protected void setup() 
	{
        
        //Register the service
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Test");
        sd.setName("Test");
        register(sd);

		addBehaviour( new CyclicBehaviour(this) 
		{
			public void action() 
			{
				ACLMessage msg = receive( query );
				if (msg!=null) 
					addBehaviour( new Transaction(myAgent, msg) );
				block();
			}
		});
		// addBehaviour( new GCAgent( this, 5000));
	}


	class Transaction extends SequentialBehaviour 
	{
		ACLMessage msg,
                    reply ;
		String     ConvID ;
		
		int    price  = rnd.nextInt(100);
		//int    price  = 100;

		public Transaction(Agent a, ACLMessage msg) 
		{
			super( a );
			this.msg = msg;
			ConvID = msg.getConversationId();
		}
		
		public void onStart() 
		{
		    int delay = delay = rnd.nextInt( 2000 );
			System.out.println( " - " +
				myAgent.getLocalName() + " <- QUERY from " +
				msg.getSender().getLocalName() +
				".  Will answer $" + price + " in " + delay + " ms");
				
			addSubBehaviour( new DelayBehaviour( myAgent, delay)
          	{
				public void handleElapsedTimeout() { 
					reply = msg.createReply();
					reply.setPerformative( ACLMessage.INFORM );
					reply.setContent("" + price );
					send(reply); 
				}
          	});

			MessageTemplate template = MessageTemplate.and( 
				MessageTemplate.MatchPerformative( ACLMessage.REQUEST ),
				MessageTemplate.MatchConversationId( ConvID ));
        
			addSubBehaviour( new MyReceiverBehaviour( myAgent, 15000, template) 
			{
				public void handle( ACLMessage msg1) 
				{  
					if (msg1 != null ) {
						
						double offer = Double.parseDouble( msg1.getContent());
						System.out.println("Got proposal $" + offer +
							" from " + msg1.getSender().getLocalName() +
						    " & my price is $" + price );
							
						reply = msg1.createReply();
						if ( offer >= (rnd.nextInt(price)*0.8) )
							reply.setPerformative( ACLMessage.AGREE );
						else
							reply.setPerformative( ACLMessage.REFUSE );
						send(reply);
						System.out.println("  == " + 
							ACLMessage.getPerformative(reply.getPerformative() ));
                    } 
                    else {
                        System.out.println("Timeout ! quote $" + price +
                            " from " + getLocalName() +
						    " is no longer valid");
					}
				}	
			});
		}
            
	}  // --- Answer class ---

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

        System.out.println(getLocalName() + ": closed.");
    }
    // ========== Utility methods =========================
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

    //  --- generating Conversation IDs -------------------

    protected static int cidCnt = 0;
    String cidBase ;
    
    String genCID() 
    { 
        if (cidBase==null) {
            cidBase = getLocalName() + hashCode() +
                        System.currentTimeMillis()%10000 + "_";
        }
        return  cidBase + (cidCnt++); 
    }

    //  --- generating distinct Random generator -------------------

    private Random newRandom() 
    {	return  new Random( hashCode() + System.currentTimeMillis()); }



    

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
}
