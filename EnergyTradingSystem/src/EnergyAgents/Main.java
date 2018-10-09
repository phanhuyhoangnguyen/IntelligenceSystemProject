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
		
		
		System.out.println("Main Class finished.");
	}
	
	
	private static void createRetailerAgent() {
		System.out.println("Retailer Agenet start");
		
		
		ContainerController retailerContainer = JadeController.createContainer("Retailer-Container");
		
		AgentController retailerAgent = JadeController.createAgent("AAA", "EnergyAgents.RetailerAgent", retailerContainer);
		
		JadeController.showAgentGUI(retailerAgent);
		
	}
	
	
}
