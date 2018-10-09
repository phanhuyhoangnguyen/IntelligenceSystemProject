package EnergyAgents;


import jade.util.leap.List;
import jade.util.leap.Properties;

import java.util.ArrayList;
import java.util.Vector;

import GUI.GUIListener;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.AgentContainer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.ContainerID;
import jade.core.Runtime;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.QueryAgentsOnLocation;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import jade.wrapper.gateway.JadeGateway;

/**
 * Jade Main Controller
 * Run Jade Boot, manage an agent
 * 
 * @author Tola Veng
 * @date 2018-09-10 
 */


public class JadeController {
	
	private static JadeController jadeController = null;
	private static final String MAINHOST = "localhost";
	private static final String MAINPORT = "1099";
	
	
	// Jade
	private Runtime jadeRuntime;
	private ContainerController mainContainer;
	
	// prevent instantiated
	private JadeController() {
		printLog("initiated.");
		jadeRuntime = null;
	}
	
	
	/**
	 * Start Jade Controller
	 */
	public static void start() {
		if ( jadeController == null ) {
			jadeController = new JadeController();
			
			jadeController.printLog("starting.");
			jadeController.jadeBoot();
			
		}else {
			jadeController.printLog("already started.");
		}
	}
	
	
	/**
	 * Stop Jade Controller
	 */
	public static void stop() {
		if ( jadeController != null ) {
			jadeController.jadeShutdown();
			jadeController = null;
		}
	}
	
	
	/**
	 *  Boot Jade GUI
	 *  running in the local host port 1099
	 */
	private void jadeBoot() {
		
		//Check is Jade already launched
		if ( jadeRuntime != null ) {
			printLog("Jade has already Booted.");
			return;
		}
		
		// Get a hold on JADE runtime
		jadeRuntime = Runtime.instance();
	    // Exit the JVM when there are no more containers around
		jadeRuntime.setCloseVM(true);
	    
	    // Creates a profile
	    Profile profile = new ProfileImpl(true);
	    profile.setParameter(Profile.MAIN_HOST, MAINHOST);
	    profile.setParameter(Profile.MAIN_PORT, MAINPORT);
	    profile.setParameter(Profile.GUI, "true");
	    
	    // Creates a main container
	    mainContainer = jadeRuntime.createMainContainer(profile);
	 
	    // Wait for some time
	    try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	    
	    printLog("Jade Booted.");
	}
	
	/**
	 * Shutdown Jade GUI
	 */
	private void jadeShutdown() {
		if ( jadeRuntime != null ) {
			jadeController.printLog("Jade is shutting down.");
			try {
				// wait
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				mainContainer.kill();
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
			jadeRuntime.shutDown();
			jadeRuntime = null;
			jadeController.printLog("Jade was shutdown.");
		}
	}
	
	/**
	 * Create container
	 * @param String container name
	 * @return AgentContainer
	 */
	public static ContainerController createContainer( String containerName) {
		ContainerController container = null;
		// Creates a profile
	    Profile profile = new ProfileImpl(false);
	    profile.setParameter(Profile.CONTAINER_NAME, containerName);
	    container = jadeController.jadeRuntime.createAgentContainer(profile);
	 
		return container;
	}
	
	
	/**
	 * Create an agent
	 * @param String agent name, String classname, container
	 * @return Agent Controller AID if success
	 */
	public static AgentController createAgent( String agentAlias, String agentClassName, ContainerController container ) {
		if ( jadeController == null ) {
			return null;
		}
		if ( container == null ) {
			container = jadeController.mainContainer;
		}
		try {
			AgentController agentCtrl = container.createNewAgent( agentAlias, agentClassName, null);
			agentCtrl.start();
			//jadeController.printLog(agentAlias + " is created.");
			return agentCtrl;
			
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	/**
	 * Get list of agents' names
	 * @param String container name
	 */
	public static java.util.List getAgentsList ( ContainerController containerCtl ) {
		
		java.util.List agentList = new ArrayList<String>();
		String containerName = AgentContainer.MAIN_CONTAINER_NAME;
		
		if ( jadeController == null ) {
			return null;
		}
		
		if ( containerCtl != null ) {
			try {
				containerName = containerCtl.getContainerName();
			} catch (ControllerException e) {
				e.printStackTrace();
			}	
		}
		
		Properties pp = new Properties();
		pp.setProperty(Profile.MAIN_HOST, MAINHOST);
		pp.setProperty(Profile.MAIN_PORT, MAINPORT);
		JadeGateway.init(null, pp);
		
		try {
			AgentsListRetriever alr = new AgentsListRetriever(containerName);
			JadeGateway.execute(alr);
			
			List  agents = alr.getAgents();
			if (agents != null) {
				for (int i = 0; i < agents.size(); ++i) {
					AID agentId = (AID) agents.get(i);
					agentList.add(agentId.getLocalName());
				}
			}

		}catch(Exception e) {
		}finally{
			JadeGateway.shutdown();
		}
		
		return agentList;
	}
	
	
	
	/**
	 * delete and kill an agent
	 * @param AID Jade agent identification
	 */
	
	/**
	 * stop an agent
	 * @param AID Jade agent identification
	 */
	
	/**
	 * start an agent
	 * @param AID Jade agent identification
	 */
	
	
	/* --- GUI --- */
	/**
	 * Invoke the agent show gui method
	 */
	public static void showAgentGUI (AgentController agentCtl) {
		try {
			GUIListener gui = agentCtl.getO2AInterface(GUIListener.class);
			gui.showGUI();
		}catch( StaleProxyException e) {
			jadeController.printLog("cannot get the interface");
			e.printStackTrace();
		}
	}

	
	
	/* --- Helper --- */

	/**
	 * Inner class to get agents list
	 * 
	 */ 
	private static class ListAgentsBehaviour extends OneShotBehaviour {

		private AMSAgentDescription [] agents = null;
		
		@Override
			
		public void action() {
			try {
	            SearchConstraints c = new SearchConstraints();
	            c.setMaxResults (new Long(-1));
				agents = AMSService.search( myAgent, new AMSAgentDescription (), c );
			} catch (Exception e) {
	            System.out.println( "Problem searching AMS: " + e );
	            e.printStackTrace();
			}
		}
	}

	
	
	
	/**
	 * Inner class AgentsListRetriever extends the AchieveREInitiator behaviour. It
	 * requests the AMS for the list of all agents running in the Platform
	 */
	private static class AgentsListRetriever extends AchieveREInitiator {

		private List agents;
		private String containerName;

		public AgentsListRetriever(String containerName) {
			super(null, null);
			this.containerName = containerName;
		}

		public List getAgents() {
			return agents;
		}
		
		public void onStart() {
			super.onStart();

			myAgent.getContentManager().registerLanguage(new SLCodec());
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
		}

		@Override
		protected Vector prepareRequests(ACLMessage initialMsg) {
			Vector v = null;

			// Prepare the request to be sent to the AMS
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.addReceiver(myAgent.getAMS());
			request.setOntology(JADEManagementOntology.getInstance().getName());
			request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);

			// This class is used to retrieve the list of agents running on a given
			// container
			// In this example, it is the main container
			QueryAgentsOnLocation qaol = new QueryAgentsOnLocation();
			qaol.setLocation(new ContainerID(containerName, null));
			Action actExpr = new Action(myAgent.getAMS(), qaol);
			try {
				// Fills the message content of the 'request' message with the action 'qaol'
				myAgent.getContentManager().fillContent(request, actExpr);
				v = new Vector(1);
				v.add(request);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return v;
		}

		
		@Override
		protected void handleInform(ACLMessage inform) {
			try {
				// Get the result from the AMS, parse it and store the list of agents
				Result result = (Result) myAgent.getContentManager().extractContent(inform);
				agents = (List) result.getValue();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	} // end AgentsListRetriever
	
	
	private void printLog(String log) {
		System.out.println("Jade Controller:" + log);
	}
	
}// end Jade controller class