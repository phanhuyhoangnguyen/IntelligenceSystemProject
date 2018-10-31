package EnergyAgents;

import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.core.Agent;


import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;



/**
 * Utilities Class
 * 
 * @author The 'Dangerous' Dave
 *
 * @Description This class is for common methods
 */

public final class Utilities{

     /**
     * 
     * Generate new ACL message
     * 
     * @param perf performative
     * @return return ACL message
     */
    public static ACLMessage newMessage(int perf) {
        ACLMessage message = new ACLMessage(perf);
        return message;
    }


    /**
     *  This method is used to initialize ACLMessages
     * 
     * @param perf        perfomative (Ex: AGREE, PROPOSAL, REQUEST)
     * @param content     content of the message (Ex: Hello)
     * @param destination destination of the message
     * @return return ACL message
     */
    public static ACLMessage newMessage(int perf, String content, AID destination) {
        ACLMessage message = newMessage(perf);
        if (destination != null) {
            message.addReceiver(destination);
        }
        message.setContent(content);
        return message;
    }

     /**
     * Get a random double number between two numbers
     * 
     * @param min the minimum double number
     * @param max the maximum double number
     * @return a random number
     */
    public static double getRandomDouble(double min, double max) {
        double d = (Math.random() * ((max - min) + 1)) + min;
        return Math.round(d * 100.0) / 100.0;
    }


     /**
     * 
     * @param value
     * @return round to 2 decimal numbers
     */
    public static double truncatedDouble(double value) {
        return java.math.BigDecimal.valueOf(value).setScale(3, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Register service
     * 
     * @param a agent
     * @param agentName
     * @param agentType
     */
    public static void registerService(Agent a, String agentName, String agentType){
        ServiceDescription sd = new ServiceDescription();
        sd.setType(agentType);
        sd.setName(agentName);
        register(sd, a);
    }

    /**
     * Test and remove old duplicate DF entries before add new one
     * 
     * @param sd service description
     * @param a agent
     */
    private static void register(ServiceDescription sd, Agent a) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(a.getAID());
        try {
            DFAgentDescription list[] = DFService.search(a, dfd);
            if (list.length > 0) {
                DFService.deregister(a);
            }
            dfd.addServices(sd);
            DFService.register(a, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

}