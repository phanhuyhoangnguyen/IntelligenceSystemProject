package GUI;
import java.awt.BorderLayout;
import java.awt.Dimension;
/**
 * Retailer GUI
 * 
 * @author Anh
 *
 */
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerBuilder;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;


import EnergyAgents.JadeController;
import EnergyAgents.PrintAgent;
import jade.core.AID;
import jade.core.Profile;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import jade.wrapper.gateway.JadeGateway;

public class MainGUI extends JFrame {
	private JPanel contentPane;
	
	private AgentController printAgent;
	private AgentController homeAgent;
	private List<AgentController> retailerAgents;
	private List<AgentController> applianceAgents;
	
	
	private boolean isNegoStarted = false;
	private JButton mStartButton;
	private JButton mPauseButton;
	
	/* --- Getter / Setter --- */
	public AgentController getHomeAgent() {
		return homeAgent;
	}
	public void setHomeAgent(AgentController homeAgent) {
		this.homeAgent = homeAgent;
	}

	public AgentController getPrintAgent() {
		return printAgent;
	}
	public void setPrintAgent(AgentController printAgent) {
		this.printAgent = printAgent;
	}

	public List<AgentController> getRetailerAgents() {
		return retailerAgents;
	}
	public void setRetailerAgents(List<AgentController> retailerAgents) {
		this.retailerAgents = retailerAgents;
	}


	public List<AgentController> getApplianceAgents() {
		return applianceAgents;
	}
	public void setApplianceAgents(List<AgentController> applianceAgents) {
		this.applianceAgents = applianceAgents;
	}

	
	/**
	 * Create the frame.
	 */
	public MainGUI() {
		setTitle("Energy System");
		
		// set default
		printAgent = null;
		homeAgent = null;
		retailerAgents = null;
		applianceAgents = null;
		
		// create components
		initComponent();
		
	}
	
	


	public void initComponent()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 520, 397);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		JLabel mAppliance = new JLabel("appliance");
		mAppliance.setIcon(new ImageIcon(getClass().getResource("/resources/Tools-icon.png"), "Appliances"));
		mAppliance.addMouseListener(new OnMouseListener());
		
		JLabel mHome = new JLabel("home");
		mHome.setIcon(new ImageIcon(getClass().getResource("/resources/Home-icon.png"), "Home"));
		mHome.addMouseListener(new OnMouseListener());
		
		JLabel mRetailers = new JLabel("retailer");
		mRetailers.setIcon(new ImageIcon(getClass().getResource("/resources/dollar-icon.png"), "Retailers"));
		mRetailers.addMouseListener(new OnMouseListener());
		
		
		JLabel mApplianceText = new JLabel("Appliance");
		mApplianceText.setFont(new Font("Tahoma", Font.BOLD, 19));
		mApplianceText.addMouseListener(new OnMouseListener());
		
		JLabel mHomeText = new JLabel("Home");
		mHomeText.setFont(new Font("Tahoma", Font.BOLD, 19));
		mHomeText.addMouseListener(new OnMouseListener());
		
		JLabel mRetailerText = new JLabel("Retailer");
		mRetailerText.setFont(new Font("Tahoma", Font.BOLD, 19));
		mRetailerText.addMouseListener(new OnMouseListener());
		
		mStartButton = new JButton(" Start ");
		mStartButton.setPreferredSize(new Dimension(100, 50));
		mStartButton.addActionListener(new StartClick());
		
		mPauseButton = new JButton(" Pause ");
		mPauseButton.setPreferredSize(new Dimension(100, 50));
		mPauseButton.addActionListener(new PauseClick());
		mPauseButton.setEnabled(false);
		
		JPanel bottomPane = new JPanel();
		bottomPane.add(mStartButton);
		bottomPane.add(mPauseButton);
		
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGap(22)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(mAppliance, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE)
							.addGap(88)
							.addComponent(mHome, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(10)
							.addComponent(mApplianceText)
							.addGap(100)
							.addComponent(mHomeText)))
					.addPreferredGap(ComponentPlacement.RELATED, 88, Short.MAX_VALUE)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(10)
							.addComponent(mRetailerText))
						.addComponent(mRetailers, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE))
					.addContainerGap())
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGap(97)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(mAppliance, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
						.addComponent(mHome, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
						.addComponent(mRetailers, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(mApplianceText)
						.addComponent(mHomeText)
						.addComponent(mRetailerText))
					.addContainerGap(127, Short.MAX_VALUE))
		);
		
			
		contentPane.setLayout(gl_contentPane);
		
		//setContentPane(contentPane);
		add(contentPane, BorderLayout.CENTER);
		add(bottomPane, BorderLayout.SOUTH);
		
		
	}	// end components
	
	/**
	 * Inner class handle mouse click
	 */
	private class OnMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e)  
	    {  
			if (e.getSource() instanceof JLabel) {
				JLabel lbl = (JLabel) e.getSource();
				
				switch(lbl.getText().toLowerCase()){
					case "home":
						if ( homeAgent != null) {
							JadeController.showAgentGUI(homeAgent);
						}
						System.out.println("Home click");
						break;
						
					case "appliance":
						if ( applianceAgents != null ) {
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									try {
										AppliantsGUI gui = new AppliantsGUI(applianceAgents);
										gui.setVisible(true);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							});
						break;
						}
						
					case "retailer":
						if ( retailerAgents != null ) {
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									try {
										RetailerGUI gui = new RetailerGUI(retailerAgents);
										gui.setVisible(true);
										
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							});
						}
						System.out.println("Retailer click");
						break;
				}
				
			}// if
			
	    } // click 
	}
	
	
	/*
	 * Handle button start listener
	 */
	private class StartClick implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			
			// check is already started
			if ( isNegoStarted ) {
				return;
			} else {
				isNegoStarted = true;
			}
			// enable disable button
			mStartButton.setEnabled(!isNegoStarted);
			mPauseButton.setEnabled(isNegoStarted);
			
			System.out.println("Main GUI: start negotiation.");
			
			// Invoke appliance agents
			Properties pp = new Properties();
			pp.setProperty(Profile.MAIN_HOST, JadeController.MAINHOST);
			pp.setProperty(Profile.MAIN_PORT, JadeController.MAINPORT);
			JadeGateway.init(null, pp);
			
			OneShotBehaviour sendMessage = new OneShotBehaviour(){
				@Override
				public void action() {
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent("start");
					// tell home
					AID home = new AID("Home", AID.ISLOCALNAME);
					msg.addReceiver(home);
					
					// tell appliance
					for ( AgentController agent : applianceAgents) {
						try {
							AID aid = new AID(agent.getName(), AID.ISGUID);
							msg.addReceiver(aid);
						} catch (StaleProxyException e1) {
							e1.printStackTrace();
						}
					}
					
					myAgent.send(msg);
				}
			};
			
			try {
				JadeGateway.execute(sendMessage);
			} catch (ControllerException | InterruptedException e1) {
				e1.printStackTrace();
			}
			JadeGateway.shutdown();
			
			
			
		} // action perform
		  
	} // end click


	/*
	 * Handle button Pause listener
	 */
	private class PauseClick implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			
			// check is already paused
			if ( !isNegoStarted ) {
				return;
			} else {
				isNegoStarted = false;
			}
			// enable disable button
			mStartButton.setEnabled(!isNegoStarted);
			mPauseButton.setEnabled(isNegoStarted);
						
			System.out.println("Main GUI: pause negotiation.");
			
			// Invoke appliance agents
			Properties pp = new Properties();
			pp.setProperty(Profile.MAIN_HOST, JadeController.MAINHOST);
			pp.setProperty(Profile.MAIN_PORT, JadeController.MAINPORT);
			JadeGateway.init(null, pp);
			
			OneShotBehaviour sendMessage = new OneShotBehaviour(){
				@Override
				public void action() {
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent("pause");
					// tell home
					AID home = new AID("Home", AID.ISLOCALNAME);
					msg.addReceiver(home);
					
					// tell appliance
					for ( AgentController agent : applianceAgents) {
						try {
							AID aid = new AID(agent.getName(), AID.ISGUID);
							msg.addReceiver(aid);
						} catch (StaleProxyException e1) {
							e1.printStackTrace();
						}
					}
					
					myAgent.send(msg);
				}
			};
			
			try {
				JadeGateway.execute(sendMessage);
			} catch (ControllerException | InterruptedException e1) {
				e1.printStackTrace();
			}
			JadeGateway.shutdown();
			
			
		} // action perform
		  
	} // end click





}
