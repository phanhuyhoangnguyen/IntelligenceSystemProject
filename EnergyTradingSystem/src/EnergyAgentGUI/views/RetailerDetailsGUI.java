package EnergyAgentGUI.views;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextField;
import java.awt.Font;
import javax.swing.JButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RetailerDetailsGUI extends JFrame {

	private JPanel contentPane;
	String title = "Agent Selected : Solar";
	private JTextField nameInput;
	private JTextField priceInput;
	private JTextField offerInput;
	private JButton saveBtn;
	private JButton resetBtn;

	/**
	 * Launch the application.
	 */
	
	/*
	 * This method match the object of actual Agent.
	 */
	public void matchingAgent()
	{
		
	}
	public static void main(String[] args,String agentName) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RetailerDetailsGUI frame = new RetailerDetailsGUI(agentName);
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
	
	/*
	 * Reseting input/ Modify later
	 */
	public void reset()
	{
		nameInput.setText("Tola");
		priceInput.setText("Price");
		offerInput.setText("3");
	}
	public RetailerDetailsGUI(String agentName) {
		setTitle(agentName);
	
		setBounds(100, 100, 278, 181);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		JLabel nameText = new JLabel("Name");
		nameText.setFont(new Font("Tahoma", Font.BOLD, 15));
		
		JLabel lblPrice = new JLabel("Price");
		lblPrice.setFont(new Font("Tahoma", Font.BOLD, 15));
		
		JLabel offer = new JLabel("Offer Counter");
		offer.setFont(new Font("Tahoma", Font.BOLD, 15));
		
		nameInput = new JTextField();
		nameInput.setText("Tola");
		nameInput.setColumns(10);
		
		priceInput = new JTextField();
		priceInput.setText("Price");
		priceInput.setColumns(10);
		
		offerInput = new JTextField();
		offerInput.setText("3");
		offerInput.setColumns(10);
		
		
		
		
		/*
		 * This button Save the details of agents
		 */
		saveBtn = new JButton("SAVE");
		saveBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				///Add actions he
			}
		});
		saveBtn.setFont(new Font("Tahoma", Font.BOLD, 16));
		
		
		
		
		
		
		/*
		 * This button Save the details of agents
		 */
		resetBtn = new JButton("RESET");
		resetBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				reset();
				
			}
		});
		resetBtn.setFont(new Font("Tahoma", Font.BOLD, 16));
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(Alignment.TRAILING, gl_contentPane.createSequentialGroup()
							.addComponent(offer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addGap(1)
							.addComponent(offerInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(nameText)
								.addComponent(lblPrice, GroupLayout.PREFERRED_SIZE, 58, GroupLayout.PREFERRED_SIZE))
							.addGap(58)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(nameInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(priceInput, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE)))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(saveBtn, GroupLayout.PREFERRED_SIZE, 105, GroupLayout.PREFERRED_SIZE)
							.addGap(30)
							.addComponent(resetBtn)))
					.addContainerGap(18, GroupLayout.PREFERRED_SIZE))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(nameInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(nameText))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(priceInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPrice))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(offer)
						.addComponent(offerInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(saveBtn)
						.addComponent(resetBtn, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE))
					.addContainerGap())
		);
		contentPane.setLayout(gl_contentPane);
	}
}
