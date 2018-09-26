package EnergyAgentGUI.views;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent; 
import java.awt.event.ItemListener;
import javax.swing.Icon; 
import javax.swing.ImageIcon; 
import javax.swing.JComboBox; 
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSpinner;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import java.awt.Color;
import javax.swing.JSeparator;
import java.awt.SystemColor;
import java.awt.Font;

public class homeActivityGUI extends JFrame {

	private JPanel contentPane;
	JLabel appliant_agent_list = new JLabel("");

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					homeActivityGUI frame = new homeActivityGUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public homeActivityGUI() {
		setTitle("Agent Controller GUI");
		initializeComponents();
		createEvents();
		
	}
	/////////////// This is initialize all the components 
	private void initializeComponents() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 766, 656);
		contentPane = new JPanel();
		contentPane.setBackground(Color.LIGHT_GRAY);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		appliantAgents ALAgents = new appliantAgents();
		
		JButton modifyAgentBtbn = new JButton("Appliants");
		modifyAgentBtbn.setFont(new Font("SansSerif", Font.PLAIN, 25));
		//modifyAgentBtbn.setIcon(new ImageIcon("C:\\Users\\TuanAnh\\eclipse-workspace\\WindowBuilderAgentsGUI\\bin\\windowBuilder\\resources\\edit-512.png"));
		initializeAppliantAgents();
		modifyAgentBtbn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ALAgents.main(null); ///// Appliant Agent.
				initializeAppliantAgents();
			}
		});
		
		modifyAgentBtbn.setBorder(null);
		modifyAgentBtbn.setBackground(null);
		JLabel lblFinalPrice = new JLabel("Final amount predict: 600");
		lblFinalPrice.setFont(new Font("SansSerif", Font.PLAIN, 15));
		
		JPanel panel = new JPanel();
		panel.setBackground(new Color(176, 224, 230));
		
		JSeparator separator = new JSeparator();
		separator.setBackground(Color.RED);
		separator.setOrientation(SwingConstants.VERTICAL);
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setOrientation(SwingConstants.VERTICAL);
		separator_1.setBackground(Color.RED);
		
		JLabel home_limitation = new JLabel("Limitation: 600");
		home_limitation.setFont(new Font("SansSerif", Font.PLAIN, 15));
		
		homeAgent home_agent = new homeAgent();
		JButton home_agents_modify = new JButton("Homes");
		home_agents_modify.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				home_agent.main(null);
			}
		});
		home_agents_modify.setIcon(new ImageIcon("C:\\Users\\TuanAnh\\eclipse-workspace\\WindowBuilderAgentsGUI\\bin\\windowBuilder\\resources\\edit-512.png"));
		home_agents_modify.setFont(new Font("SansSerif", Font.PLAIN, 25));
		home_agents_modify.setBorder(null);
		home_agents_modify.setBackground((Color) null);
		
		JButton button = new JButton("");
		button.setIcon(new ImageIcon("C:\\Users\\TuanAnh\\eclipse-workspace\\WindowBuilderAgentsGUI\\bin\\windowBuilder\\resources\\green-start-button-clip-art-0.png"));
		button.setFont(new Font("SansSerif", Font.PLAIN, 25));
		button.setBorder(null);
		button.setBackground((Color) null);
		
		
		
		
		
		
		
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, 720, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(18, Short.MAX_VALUE))
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addComponent(appliant_agent_list, GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE)
						.addComponent(modifyAgentBtbn, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(30)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addComponent(lblFinalPrice)
						.addComponent(home_agents_modify, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
						.addComponent(home_limitation, GroupLayout.PREFERRED_SIZE, 177, GroupLayout.PREFERRED_SIZE))
					.addGap(41)
					.addComponent(separator_1, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(button, GroupLayout.PREFERRED_SIZE, 214, GroupLayout.PREFERRED_SIZE)
					.addGap(34))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, 152, GroupLayout.PREFERRED_SIZE)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(18)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(separator_1, GroupLayout.PREFERRED_SIZE, 448, GroupLayout.PREFERRED_SIZE)
								.addComponent(separator, GroupLayout.PREFERRED_SIZE, 449, GroupLayout.PREFERRED_SIZE)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(modifyAgentBtbn, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
									.addGap(35)
									.addComponent(appliant_agent_list, GroupLayout.PREFERRED_SIZE, 206, GroupLayout.PREFERRED_SIZE))
								.addGroup(gl_contentPane.createSequentialGroup()
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(home_agents_modify, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
									.addGap(18)
									.addComponent(lblFinalPrice, GroupLayout.PREFERRED_SIZE, 47, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(home_limitation, GroupLayout.PREFERRED_SIZE, 47, GroupLayout.PREFERRED_SIZE))))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(128)
							.addComponent(button)))
					.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);
		appliant_agent_list.setVerticalAlignment(SwingConstants.TOP);
		appliant_agent_list.setFont(new Font("SansSerif", Font.PLAIN, 15));
		
		JLabel label = new JLabel("");
		//label.setIcon(new ImageIcon("C:\\Users\\TuanAnh\\eclipse-workspace\\WindowBuilderAgentsGUI\\bin\\windowBuilder\\resources\\maxresdefault.jpg"));
		panel.add(label);
		
		
		contentPane.setLayout(gl_contentPane);
		
	}
	

	//////////// this is behaviours events
	private void createEvents() {
		
	}
	
	private void initializeAppliantAgents()
	{
		appliant_agent_list.setBackground(Color.LIGHT_GRAY);
		appliant_agent_list.setText("<html>Agent 1 predict : 100<br><br> Agent 2 predict : 200 <br> <br>Agent 3 predict : 300</html>");
	}
}
