# Home Energy System
Home energy trading system is an application developed using Java programming language to demonstrate communcation between agents.
The application has been implemented on Jade (Java Agent DEvelopment) framework that is agents-based interopration.
It performs on FIPA interaction protocol specification to communicate and negotiate between agents, peer-to-peer interaction.


## How to run the application
1. It can be run by clicking the batch file **RUN_ME.bat** in the root directory.
    
2. Run from command line
Navigate to the Compiled directory, run command 
	> #>java -jar ./HomeEnergySys.jar
	
    
## How to start the agents communication
Bring up the main user interface
* To start the communication, user will need to click on the **Start button**: interaction between Agents will be printed out the main GUI with the key information.
* Additionally, user have option to pause the communication with the **Pause** button: after the agents finish the current negotation, the system will remain unchanged.
* The system can be resumed with the **Resume** button: the Agents will then start the new communication session with new request sent and new negotiation.


## Update the agents' properties
In the main user interface
1. Select on an icon represent the agent, Home, Appliances, or Retailers. Depend on different Agent Type, there will be different options to modify the Agent Properties.
    1.1 If the Agent is Home, new pop-up windows will be appeared to modify its property
    1.2 If the Agent is Retailer, you can set the properties by clicking the "Set Properties" button on the right side of the window
    1.3 There is no option to modify Appliance Agent, since its generated data is extracted from the provided CSV file.
2. After the modification, click "Save" to apply your changes.


##Tool and IDE
The application has been developed in Eclipse Photon 4.8.0 2018 JAVA DEVELOPMENT IDE.
Java version 1.8.0