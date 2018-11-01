package EnergyAgents;

import java.awt.EventQueue;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.omg.PortableServer.POAManagerPackage.State;

import jade.wrapper.AgentController;
import jade.wrapper.AgentState;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import EnergyAgents.RetailerAgent;
import GUI.MainGUI;


public class Main {

	public static void main(String[] args) {
		System.out.println("Main Class running.");
		
		 // Start Jade Platform
		JadeController.start();
		
		// Create Print GUI
		AgentController printAgent = createPrintAgent();
		
		// Create Retailer agents
		try {
			Thread.sleep(200);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<AgentController> retailerAgents = createRetailerAgents();
		
		
		// Create appliance agents
		try {
			Thread.sleep(200);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<AgentController> applianceAgents = createApplianceAgents();
		
		//Create home agent
		try {
			Thread.sleep(200);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		AgentController homeAgent = createHomeAgent();
		
		
		// Start Main GUI
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainGUI mainGUI = new MainGUI();
					
					// attach the agents list
					mainGUI.setPrintAgent(printAgent);
					mainGUI.setRetailerAgents(retailerAgents);
					mainGUI.setApplianceAgents(applianceAgents);
					mainGUI.setHomeAgent(homeAgent);
					mainGUI.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		
		System.out.println("Main Class finished.");
	}
	
	
	private static List<AgentController> createRetailerAgents() {
		System.out.println("Retailer Agenet start");
		
		// create container
		ContainerController retailerContainer = JadeController.createContainer("Retailer-Container");
		
		// create agents
		List<AgentController> retailerAgents= new ArrayList<>();
		retailerAgents.add( JadeController.createAgent("Today Energy", "EnergyAgents.RetailerAgent", retailerContainer) );
		retailerAgents.add( JadeController.createAgent("Saving Energy", "EnergyAgents.RetailerAgent", retailerContainer) );
		retailerAgents.add( JadeController.createAgent("DayTime Energy", "EnergyAgents.RetailerAgent", retailerContainer) );
		retailerAgents.add( JadeController.createAgent("Budget Energy", "EnergyAgents.RetailerAgent", retailerContainer, new Object[]{"fixed_price", "30"}) );
		retailerAgents.add( JadeController.createAgent("Yellow Energy", "EnergyAgents.RetailerAgent", retailerContainer) );
		
		return retailerAgents;
		
	}
	private static List<AgentController> createApplianceAgents() {
		System.out.println("Appliant Agents start");
		
		// create container
		ContainerController applianceContainer = JadeController.createContainer("Appliance-Container");
		
		// create agents
		List<AgentController> applianceAgents= new ArrayList<>();
		applianceAgents.add( JadeController.createAgent("Cloth Washer", "EnergyAgents.ApplianceAgent", applianceContainer, new Object[]{"Appliance","CWE"}) );
		applianceAgents.add( JadeController.createAgent("Dishwasher", "EnergyAgents.ApplianceAgent", applianceContainer, new Object[]{"Appliance","DWE"}) );
		applianceAgents.add( JadeController.createAgent("Force Air Energy", "EnergyAgents.ApplianceAgent", applianceContainer, new Object[]{"Appliance","FRE"}) );
		applianceAgents.add( JadeController.createAgent("Fridge Energy", "EnergyAgents.ApplianceAgent", applianceContainer, new Object[]{"Appliance","FGE"}) );
		applianceAgents.add( JadeController.createAgent("Ultility Energy", "EnergyAgents.ApplianceAgent", applianceContainer, new Object[]{"Appliance","UTE"}) );
		
		return applianceAgents;
	}
	
	//Create Home Agent
	private static AgentController createHomeAgent()
	{
			System.out.println("Home Agent is starting");

			// Create container for the home agent
			ContainerController homeContainer = JadeController.createContainer("Home-Container");

			AgentController homeAgent = JadeController.createAgent("Home", "EnergyAgents.HomeAgent", homeContainer);
			
			return homeAgent;
	}
	
	
	private static AgentController createPrintAgent() {
		return JadeController.createAgent(PrintAgent.AGENT_NAME, "EnergyAgents.PrintAgent", null);
	}
	
	private static void createRetailerTest() {
		ContainerController testContainer = JadeController.createContainer("Test-Container");
		JadeController.createAgent("RetailerTest", "EnergyAgents.RetailerTest", testContainer);
	}
	
}
