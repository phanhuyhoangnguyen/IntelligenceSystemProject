package EnergyAgents;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import EnergyAgents.RetailerAgent;


import database.DbHelper;

public class Main {

	public static void main(String[] args) {
		System.out.println("Main Class running.");
		
		// start Jade Platform
		JadeController.start();
		
		// start retailer agents
		createRetailerAgent();
		
		//start home agent
		createHomeAgent();
		
		System.out.println("Main Class finished.");
	}
	
	
	private static void createRetailerAgent() {
		System.out.println("Retailer Agent start");
		
		
		ContainerController retailerContainer = JadeController.createContainer("Retailer-Container");
		
		AgentController retailerAgent = JadeController.createAgent("AAA", "EnergyAgents.RetailerAgent", retailerContainer);
		
		JadeController.showAgentGUI(retailerAgent);
		
	}
	
	//Create Home Agent
	private static void createHomeAgent()
	{
			System.out.println("Home Agent is starting");

			// Create container for the home agent
			ContainerController homeContainer = JadeController.createContainer("Home-Container");

			AgentController homeAgent = JadeController.createAgent("Home Agent", "EnergyAgents.HomeAgent", homeContainer);
			//TODO: Link with the GUI
			//JadeController.showAgentGUI(homeAgent);
	}
}
