package EnergyAgentGUI.views;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;



import java.awt.Font;
public class homeAgent extends JFrame  implements ActionListener {


	private String[] messageStrings = {"Agent 1","Agent 2", "Agent 3"};
	private JTextField amount_predict_set;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		homeAgent fr = new homeAgent();
		
		fr.setVisible(true);
	}

	/**
	 * Create the frame.
	 */
	public homeAgent() {
		
		setBounds(100, 100, 426, 282);
		
		amount_predict_set = new JTextField();
		amount_predict_set.setColumns(10);
		
		JTextArea amount_predict = new JTextArea();
		amount_predict.setEditable(false);
		amount_predict.setText("Set Limitation");
		
		////Saving Button Here
		JButton modifyBtn = new JButton("Modify");
		modifyBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			}
		});
		
		////Saving Button Here
		JButton cancleBtn = new JButton("Cancle");
		cancleBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//setValue
			}
		});
		
		JLabel lblHomeAgent = new JLabel("Home Agent");
		lblHomeAgent.setFont(new Font("Tahoma", Font.PLAIN, 22));
		
		
		
		
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(modifyBtn, GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
						.addComponent(amount_predict, GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE))
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(cancleBtn, GroupLayout.PREFERRED_SIZE, 195, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(50)
							.addComponent(amount_predict_set, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
					.addGap(27))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(128)
					.addComponent(lblHomeAgent, GroupLayout.PREFERRED_SIZE, 126, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(154, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(lblHomeAgent, GroupLayout.PREFERRED_SIZE, 65, GroupLayout.PREFERRED_SIZE)
					.addGap(18)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(amount_predict, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(amount_predict_set, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(87)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(modifyBtn)
						.addComponent(cancleBtn))
					.addContainerGap())
		);
		getContentPane().setLayout(groupLayout);
	}
	
	public void actionPerformed(ActionEvent e)
	{
			
	}
}
