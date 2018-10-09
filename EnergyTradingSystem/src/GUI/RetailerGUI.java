package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import EnergyAgents.RetailerAgent;
import jade.core.Agent;

/**
 * Retailer GUI
 * 
 * @author Tola
 *
 */
public class RetailerGUI {

	private RetailerAgent myAgent;
	
	
	public RetailerGUI(RetailerAgent agent) {
		myAgent = agent;
	}
	
	public void showGUI() {
		EventQueue.invokeLater(new Runnable() {
	         public void run() {
	        	 RetailerFrame frame = new RetailerFrame();
		    		frame.setVisible(true);
		    		System.out.println(myAgent.getLocalName() + " GUI is shown");
	         }
	      });
	}

	
	
	/**
	 * Inner class - Retailer Frame
	 */
	class RetailerFrame extends JFrame {
		private String title = "Retailer Agent: " + myAgent.getLocalName();
		private JFrame thisFrame;
		
		public RetailerFrame() {
			thisFrame = this;
			
			JLabel lblAgentName = new JLabel("Retailer Agent : " + myAgent.getName());
			lblAgentName.setFont(new Font(lblAgentName.getFont().getFontName(), Font.BOLD, 14));
			
			JPanel inputPane = new JPanel();
			inputPane.setLayout(new GridLayout(0, 2, 2, 5));	// row,col, vspace, hspace
			inputPane.setBorder(new EmptyBorder(5, 15, 10, 10));
			
			JLabel l1 = new JLabel("Usage Charge Price :", SwingConstants.RIGHT);
			l1.setPreferredSize(l1.getPreferredSize());
			inputPane.add(l1);
			
			JTextField txtUsageCharge = new JTextField(16);
			txtUsageCharge.setText( Double.toString(myAgent.getUsageCharge()) );
			inputPane.add(txtUsageCharge);
			
			
			JLabel l2 = new JLabel("Over Charge Price :", SwingConstants.RIGHT);
			l2.setPreferredSize(l2.getPreferredSize());
			inputPane.add(l2);
			
			JTextField txtOverCharge = new JTextField(16);
			txtOverCharge.setText(Double.toString(myAgent.getOverCharge()));
			inputPane.add(txtOverCharge);
			
			
			
			JLabel l3 = new JLabel("Limit Negotiation Price :", SwingConstants.RIGHT);
			l3.setPreferredSize(l3.getPreferredSize());
			inputPane.add(l3);
			
			JTextField txtLimitPrice = new JTextField(16);
			txtLimitPrice.setText(Double.toString(myAgent.getNegotiationLimitPrice()));
			inputPane.add(txtLimitPrice);
			
			
			
			JLabel l4 = new JLabel("Limit Counter Offer :", SwingConstants.RIGHT);
			l4.setPreferredSize(l4.getPreferredSize());
			inputPane.add(l4);
			
			JTextField txtCounterOffer = new JTextField(16);
			txtCounterOffer.setText(Integer.toString(myAgent.getNegotiationCounterOffer()));
			inputPane.add(txtCounterOffer);
			
			
			
			JLabel l5 = new JLabel("Waiting Time (sec) :", SwingConstants.RIGHT);
			l5.setPreferredSize(l5.getPreferredSize());
			inputPane.add(l5);
			
			JTextField txtWaitTime = new JTextField(10);
			txtWaitTime.setText(Long.toString(myAgent.getNegotiationTimeWait()));
			inputPane.add(txtWaitTime);
			
			
			
			JLabel l6 = new JLabel("Mechanism :", SwingConstants.RIGHT);
			l6.setPreferredSize(l6.getPreferredSize());
			inputPane.add(l6);
			
			JComboBox cbMechanism = new JComboBox();
			cbMechanism.setModel( new DefaultComboBoxModel<>(RetailerAgent.Mechanism.values()));
			inputPane.add(cbMechanism);
			
			
			// button group
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.Y_AXIS));
			buttonPane.setBorder(new EmptyBorder(10, 10, 10, 10));
			
			Dimension buttonSize = new Dimension(100, 25);
			
			JButton btnSet = new JButton(" Save ");
			btnSet.setMinimumSize(buttonSize);
			btnSet.setPreferredSize(buttonSize);
			btnSet.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						myAgent.setUsageCharge(Double.parseDouble(txtUsageCharge.getText()));
						myAgent.setOverCharge(Double.parseDouble(txtOverCharge.getText()));
						myAgent.setNegotiationLimitPrice(Double.parseDouble(txtLimitPrice.getText()));
						myAgent.setNegotiationCounterOffer(Integer.parseInt(txtCounterOffer.getText()));
						myAgent.setNegotiationTimeWait(Long.parseLong(txtWaitTime.getText()));
						
						RetailerAgent.Mechanism mechanism = RetailerAgent.Mechanism.valueOf(cbMechanism.getSelectedItem().toString());
						myAgent.setNegotiationMechanism(mechanism);
						
						
						// test
						System.out.println(myAgent.toString());
					} catch( NumberFormatException nfe) {
						nfe.printStackTrace();
						return;
					}catch( Exception ex) {
						ex.printStackTrace();
						return;
					}
					btnSet.setEnabled(false);
					thisFrame.dispose();
				}
			});
			buttonPane.add(btnSet);
			
			// space
			buttonPane.add(Box.createRigidArea(new Dimension(10, 10))); 
			
			JButton btnClose = new JButton(" Close ");
			btnClose.setMinimumSize(buttonSize);
			btnClose.setPreferredSize(buttonSize);
			btnClose.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					thisFrame.dispose();
				}
			});
			buttonPane.add(btnClose);
			
			// add to frame
			Container pane = getContentPane();
			pane.add(lblAgentName, BorderLayout.NORTH);
			pane.add(inputPane, BorderLayout.CENTER);
			pane.add(buttonPane, BorderLayout.EAST);
			
			setTitle(title);
			//setPreferredSize(getPreferredSize());
			pack();
			
			// center
			//Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			//this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
			setLocationRelativeTo(null);
			setResizable(false);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
		}
		
		
		
		
		
	} // end retailer frame
	
	
}
