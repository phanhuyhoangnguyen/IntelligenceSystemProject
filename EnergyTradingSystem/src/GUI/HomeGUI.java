package GUI;
/**
 * Retailer GUI
 * 
 * @author Anh
 *
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import EnergyAgents.HomeAgent;
import GUI.RetailerGUIDetails.RetailerFrame;

public class HomeGUI {

	
	private HomeAgent myAgent;

		/**
	 * Create the frame.
	 */
	public HomeGUI(HomeAgent homeAgent) {
		this.myAgent = homeAgent;
			
	}
	
	public void showGUI() {
		EventQueue.invokeLater(new Runnable() {
	         public void run() {
	        	 HomeFrame frame = new HomeFrame();
		    		frame.setVisible(true);
		    		//System.out.println(myAgent.getLocalName() + " GUI is shown");
	         }
	      });
	}
	
	private class HomeFrame extends JDialog {
		private String title = "Home Agent: " + myAgent.getLocalName();
		private JDialog thisFrame;
		
		public HomeFrame(){
			thisFrame = this;
			
			setTitle(title);
			setResizable(false);
			setModal(true);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			JLabel lblAgentName = new JLabel("Home Agent : " + myAgent.getName());
			lblAgentName.setFont(new Font(lblAgentName.getFont().getFontName(), Font.BOLD, 14));
			
			
			JPanel inputPane = new JPanel();
			inputPane.setLayout(new GridLayout(0, 2, 2, 5));	// row,col, vspace, hspace
			inputPane.setBorder(new EmptyBorder(15, 15, 15, 15));
			
			JLabel l1 = new JLabel("Budget :", SwingConstants.RIGHT);
			l1.setPreferredSize(l1.getPreferredSize());
			inputPane.add(l1);
			
			JTextField txtBudget = new JTextField(16);
			txtBudget.setText( Double.toString(myAgent.getBudgetLimit()) );
			inputPane.add(txtBudget);

			
			JLabel l2 = new JLabel("Maximum Price :", SwingConstants.RIGHT);
			l2.setPreferredSize(l2.getPreferredSize());
			inputPane.add(l2);
			
			JTextField txtMaximumPrice = new JTextField(16);
			txtMaximumPrice.setText( Double.toString(myAgent.getMaximumPrice()) );
			inputPane.add(txtMaximumPrice);
			
			
			// button group
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout());
			buttonPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
			buttonPane.setBorder(new EmptyBorder(10, 10, 10, 10));
			
			Dimension buttonSize = new Dimension(100, 25);
			
			JButton btnSet = new JButton(" Save ");
			btnSet.setMinimumSize(buttonSize);
			btnSet.setPreferredSize(buttonSize);
			btnSet.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						double budget = Double.parseDouble(txtBudget.getText());
						double maxPrice = Double.parseDouble(txtMaximumPrice.getText());
						myAgent.setBudgetLimit(budget);
						myAgent.setMaximumPrice(maxPrice);
					}catch( NumberFormatException nfe ) {
						return;
					}
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
			pane.add(inputPane, BorderLayout.WEST);
			pane.add(buttonPane, BorderLayout.SOUTH);
			
			pack();
			
			// center
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
			
		}
	}

}
