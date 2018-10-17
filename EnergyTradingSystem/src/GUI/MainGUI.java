package GUI;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;

import EnergyAgentGUI.views.HomeGUI;
import EnergyAgents.JadeController;
import jade.wrapper.AgentController;

public class MainGUI extends JFrame {
	private JPanel contentPane;
	
	private AgentController homeAgent;
	private List<AgentController> retailerAgents;
	private List<AgentController> applianceAgents;
	
	
	/* --- Getter / Setter --- */
	public AgentController getHomeAgent() {
		return homeAgent;
	}
	public void setHomeAgent(AgentController homeAgent) {
		this.homeAgent = homeAgent;
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
		setContentPane(contentPane);
		
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
						System.out.println("Appliance click");
						break;
						
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
	
	
	
	
}
