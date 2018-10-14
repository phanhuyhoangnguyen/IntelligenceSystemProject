package EnergyAgents;

import GUI.PrintGUI;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class PrintAgent extends Agent {

	private PrintGUI printGUI;
	
	@Override
	protected void setup() {
		
		// show gui
		printGUI = new PrintGUI(this);
		printGUI.showGUI();
		printGUI.append("<font color='blue'>Print Agent: started.</font>");
		
		
		// add listener behaviour for Inform message to print the content
		addBehaviour(new CyclicBehaviour() {

			@Override
			public void action() {
				// Receive all messages
				ACLMessage msg = myAgent.receive();
				
				// There is a message
				if (msg != null) {
					// print text
					if ( msg.getPerformative() == ACLMessage.INFORM ) {
						// get content
						String content = msg.getContent();
						printGUI.append(content);
					} else if ( msg.getPerformative() == ACLMessage.CANCEL ) {
						printGUI.clearText();
					}
				} else {
					// wait for message
					block();
				}// end if msg
			}
		});
		
	}
	
	
}
