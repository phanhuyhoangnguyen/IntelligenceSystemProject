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



public class appliantAgents extends JFrame  implements ActionListener {

	private String[] messageStrings = {"Agent 1","Agent 2", "Agent 3"};
	private JComboBox modifyAgentBox = new JComboBox(messageStrings);
	JLabel ltext = new JLabel();
	private JTextField amount_predict_set;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		appliantAgents fr = new appliantAgents();
		
		fr.setVisible(true);
	}

	/**
	 * Create the frame.
	 */
	public appliantAgents() {
		
		setBounds(100, 100, 426, 282);
		ltext.setText("Selected Agent Tola");
		modifyAgentBox.setSelectedIndex(0);
		modifyAgentBox.addActionListener(this);
		
		amount_predict_set = new JTextField();
		amount_predict_set.setColumns(10);
		
		JTextArea amount_predict = new JTextArea();
		amount_predict.setEditable(false);
		amount_predict.setText("Price prediction");
		
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
					.addGap(122)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(ltext, GroupLayout.PREFERRED_SIZE, 167, GroupLayout.PREFERRED_SIZE)
						.addComponent(modifyAgentBox, GroupLayout.PREFERRED_SIZE, 125, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(119, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(7)
					.addComponent(modifyAgentBox, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(ltext, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
					.addGap(26)
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
	
	public String getAgent()
	{
		return ltext.getText();
	}
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == modifyAgentBox)
		{
			JComboBox cb = (JComboBox) e.getSource();
			String msg = (String) cb.getSelectedItem() ;
			switch(msg) 
			{
					case "Agent 1": ltext.setText("Selected Agent Tola");
									break;
					case "Agent 2": ltext.setText("Selected Agent Dave");
									break;
					default: ltext.setText("Selected Agent Stupid");
							
			}
		}
			
	}
}