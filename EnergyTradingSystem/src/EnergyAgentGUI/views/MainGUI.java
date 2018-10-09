package EnergyAgentGUI.views;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.GroupLayout.*;
import javax.swing.LayoutStyle.*;
import javax.swing.border.*;

public class MainGUI extends JFrame {

	private JPanel contentPane;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainGUI frame = new MainGUI();
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
	public MainGUI() {
		setTitle("Energy System");
		initComponent();
	}
	
	/*
	 * This event called for Appliant 
	 */
	public void setMouseListenerAppliant(JLabel jlabel)
	{
		jlabel.addMouseListener(new MouseAdapter()  
		{  
		    public void mouseClicked(MouseEvent e)  
		    {  
		    	AppliantsGUI appliantGUI = new AppliantsGUI();
		    	appliantGUI.main(null);
		    }  
		}); 
	}
	/*
	 * This event called for Home 
	 */
	public void setMouseListenerHome(JLabel jlabel)
	{
		jlabel.addMouseListener(new MouseAdapter()  
		{  
		    public void mouseClicked(MouseEvent e)  
		    {  
		    	HomeGUI homeGUI = new HomeGUI();
		    	homeGUI.main(null);

		    }  
		}); 
	}
	/*
	 * This event called for Retailer 
	 */
	public void setMouseListenerRetailer(JLabel jlabel)
	{
		jlabel.addMouseListener(new MouseAdapter()  
		{  
		    public void mouseClicked(MouseEvent e)  
		    {  
		    	RetailersGUI retailersGUI = new RetailersGUI();
		    	retailersGUI.main(null);

		    }  
		}); 
	}
	public void initComponent()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 520, 397);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		JLabel mAppliant = new JLabel("");
		ImageIcon mAppliantIcon = new ImageIcon("resources/Tools-icon.png");
		mAppliant.setIcon(new ImageIcon("C:\\Users\\TuanAnh\\eclipse-workspace\\WindowBuilderAgentsGUI\\src\\windowBuilder\\resources\\Tools-icon.png"));
		setMouseListenerAppliant(mAppliant);
		
		JLabel mHome = new JLabel("");
		mHome.setIcon(new ImageIcon("C:\\Users\\TuanAnh\\eclipse-workspace\\WindowBuilderAgentsGUI\\src\\windowBuilder\\resources\\Home-icon.png"));
		setMouseListenerHome(mHome);
		
		JLabel mRetailers = new JLabel("");
		mRetailers.setIcon(new ImageIcon("C:\\Users\\TuanAnh\\eclipse-workspace\\WindowBuilderAgentsGUI\\src\\windowBuilder\\resources\\dollar-icon.png"));
		setMouseListenerRetailer(mRetailers);
		
		
		JLabel mAppliantText = new JLabel("Appliant");
		mAppliantText.setFont(new Font("Tahoma", Font.BOLD, 19));
		
		JLabel mHomeText = new JLabel("Home");
		mHomeText.setFont(new Font("Tahoma", Font.BOLD, 19));
		
		JLabel mRetailerText = new JLabel("Retailer");
		mRetailerText.setFont(new Font("Tahoma", Font.BOLD, 19));
		
		
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGap(22)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(mAppliant, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE)
							.addGap(88)
							.addComponent(mHome, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(10)
							.addComponent(mAppliantText)
							.addGap(115)
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
						.addComponent(mAppliant, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
						.addComponent(mHome, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
						.addComponent(mRetailers, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(mAppliantText)
						.addComponent(mHomeText)
						.addComponent(mRetailerText))
					.addContainerGap(127, Short.MAX_VALUE))
		);
		contentPane.setLayout(gl_contentPane);
	}
}
