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
import database.DbHelper;

public class Main {

	public static void main(String[] args) {
		System.out.println("Main Class running.");
		
		 // Start Jade Platform
		JadeController.start();
		
		// Create Print GUI
		createPrintAgent();
		
		// Create retailer agents
		List<AgentController> retailerAgents = createRetailerAgents();
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//start home agent
		AgentController homeAgent = createHomeAgent();
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Create retailer Test agent
		//createRetailerTest();
		
		List<AgentController> applianceAgents = createApplianceAgents();
		
		// Start Main GUI
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainGUI mainGUI = new MainGUI();
					
					// attach the agents list
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
		retailerAgents.add( JadeController.createAgent("Budget Energy", "EnergyAgents.RetailerAgent", retailerContainer) );
		retailerAgents.add( JadeController.createAgent("Yellow Energy", "EnergyAgents.RetailerAgent", retailerContainer) );
		
		return retailerAgents;
		
	}
	private static List<AgentController> createApplianceAgents() {
		System.out.println("Appliant Agents start");
		
		// create container
		ContainerController applianceContainer = JadeController.createContainer("Appliance-Container");
		
		// create agents
		List<AgentController> applianceAgents= new ArrayList<>();
		applianceAgents.add( JadeController.createAgent("Solar", "EnergyAgents.ApplianceAgent", applianceContainer) );
		applianceAgents.add( JadeController.createAgent("TV", "EnergyAgents.ApplianceAgent", applianceContainer) );
		applianceAgents.add( JadeController.createAgent("PC", "EnergyAgents.ApplianceAgent", applianceContainer) );
		applianceAgents.add( JadeController.createAgent("AirCon", "EnergyAgents.ApplianceAgent", applianceContainer) );
		applianceAgents.add( JadeController.createAgent("Microwave", "EnergyAgents.ApplianceAgent", applianceContainer) );
		
		return applianceAgents;
	}
	
	//Create Home Agent
	private static AgentController createHomeAgent()
	{
			System.out.println("Home Agent is starting");

			// Create container for the home agent
			ContainerController homeContainer = JadeController.createContainer("Home-Container");

			AgentController homeAgent = JadeController.createAgent("Home Agent", "EnergyAgents.HomeAgent", homeContainer);
			
			return homeAgent;
	}
	
	
	private static void createPrintAgent() {
		JadeController.createAgent(PrintAgent.AGENT_NAME, "EnergyAgents.PrintAgent", null);
	}
	
	private static void createRetailerTest() {
		ContainerController testContainer = JadeController.createContainer("Test-Container");
		JadeController.createAgent("RetailerTest", "EnergyAgents.RetailerTest", testContainer);
	}
	
}
