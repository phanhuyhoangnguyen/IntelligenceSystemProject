package EnergyAgentGUI.views;


import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.GroupLayout.*;
import javax.swing.LayoutStyle.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

public class AppliantsGUI extends JFrame {

	JTable table;
	int numberOfAgents = 4;
	String On = "On";
	String Off = "Off";
	
	public void applyRetailersAgent()
	{
		
	}
	public AppliantsGUI()
	{
		setTitle("Retailers Interface");
		String[] columnNames ={"Retailers","Status"};
		
		Object[][] data =
			{
				{"DF",On},
				{"BF",On},
				{"Solar",On},
				{"SomeThing",On}
			};
		DefaultTableModel model = new DefaultTableModel(data, columnNames)
        {
            
            public Class getColumnClass(int column)
            {
                return getValueAt(0, column).getClass();
            }
        };
        
        table = new JTable(model) {
        	public boolean isCellEditable(int row, int column) {                
                return false;  
        };
        };
		table.setPreferredScrollableViewportSize(new Dimension(200,15* numberOfAgents));
		table.setFillsViewportHeight(true);
		JScrollPane scrollPane = new JScrollPane(table);
		table.getColumnModel().getColumn(0).setPreferredWidth(400);
		
		
		
		
		/*
		 * This method Change the state of Agents.
		 */
		JButton startBtn = new JButton("Turn On/Off");
		startBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = 1;
				int row = table.getSelectedRow();
				
				if(table.getModel().getValueAt(row, column) == On)
					table.getModel().setValueAt(Off, row, column);
				else
					table.getModel().setValueAt(On, row, column);
			}
		});
		
		/*
		 * This method start all the Agents
		 */
		JButton startAllbtn = new JButton("Start All");
		startAllbtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = 1;
				int row = 0;
				
				for(int i = 0; i < numberOfAgents; i++)
				{
					if(table.getModel().getValueAt(row, column) == Off)
						table.getModel().setValueAt(On, row, column);
					row ++;
				}

			}
		});
		/*
		 * This method kill all the Agents
		 */
		JButton stopAllbtn = new JButton("Stop All");
		stopAllbtn.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				int column = 1;
				int row = 0;
			
				for(int i = 0; i < numberOfAgents; i++)
				{
					if(table.getModel().getValueAt(row, column) == On)
						table.getModel().setValueAt(Off, row, column);
					row ++;
				}
			}
		});
		
		
		
		
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(startAllbtn, GroupLayout.PREFERRED_SIZE, 101, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(stopAllbtn, GroupLayout.PREFERRED_SIZE, 101, GroupLayout.PREFERRED_SIZE))
						.addComponent(startBtn, GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(startBtn, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(32)
							.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(stopAllbtn)
								.addComponent(startAllbtn))))
					.addGap(257))
		);
		getContentPane().setLayout(groupLayout);
	}
	public static void main(String args[])
	{
		AppliantsGUI gui = new AppliantsGUI();
		gui.setSize(500,250);
		gui.setVisible(true	);
		gui.setTitle("Retailers Interface");
	}
}
