package GUI;

/**
 * Retailer GUI
 * 
 * @author Anh
 *
 */
import java.awt.event.*;
import java.util.List;
import java.awt.*;
import javax.swing.*;
import javax.swing.GroupLayout.*;
import javax.swing.LayoutStyle.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import EnergyAgents.JadeController;
import jade.wrapper.AgentController;
import jade.wrapper.AgentState;
import jade.wrapper.StaleProxyException;

public class RetailerGUI extends JFrame {

	private List<AgentController> retailerAgents;
	private JTable table;

	public RetailerGUI(List<AgentController> retailerAgents)
	{
		this.retailerAgents = retailerAgents;
		setTitle("Retailers Interface");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		
        
        table = new JTable(new AgentTableModel());
        
        JScrollPane tablePane = new JScrollPane(table);
        //table.setPreferredScrollableViewportSize(new Dimension(200,15* numberOfAgents));
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(400);
		table.setRowSelectionAllowed(true);
		table.setRowSelectionInterval(0, 0);	// select first row
		
		
		/*
		 * This method Change the state of Agents.
		 */
		JButton startBtn = new JButton("Start/Stop");
		startBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.getSelectedRow();
				if ( row < 0 ) {
					return;
				}
				try {
					int state = retailerAgents.get(row).getState().getCode();
					
					if ( state == AgentState.cAGENT_STATE_SUSPENDED ) {
						table.getModel().setValueAt("start", row, 1);
					}else {
						table.getModel().setValueAt("stop", row, 1);
					}
					
				} catch (StaleProxyException e1) {
					e1.printStackTrace();
				}
				
				
			}
		});
		
		/*
		 * This method start all the Agents
		 */
		JButton startAllbtn = new JButton("Start All");
		startAllbtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				for(int i = 0; i < retailerAgents.size(); i++) {
					try {
						int state = retailerAgents.get(i).getState().getCode();
						if ( state == AgentState.cAGENT_STATE_SUSPENDED ) {
							table.getModel().setValueAt("start", i, 1);
						}
					} catch (StaleProxyException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		/*
		 * This method kill all the Agents
		 */
		JButton stopAllbtn = new JButton("Stop All");
		stopAllbtn.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				for(int i = 0; i < retailerAgents.size(); i++) {
					try {
						int state = retailerAgents.get(i).getState().getCode();
						if ( state != AgentState.cAGENT_STATE_SUSPENDED ) {
							table.getModel().setValueAt("stop", i, 1);
						}
					} catch (StaleProxyException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		
		///Need Passing Value
		JButton setSelectedBtn = new JButton("Set Property");
		setSelectedBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.getSelectedRow();
				if ( row > -1 ) {
					JadeController.showAgentGUI(retailerAgents.get(row));
				}
			}
		});
		
		
		/* add layout */
		Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		
		JPanel listPane = new JPanel();
		listPane.setBorder(padding);
		listPane.add(tablePane);
		
		JPanel btnStartStopPane = new JPanel();
		btnStartStopPane.setLayout(new BoxLayout(btnStartStopPane, BoxLayout.X_AXIS));
		btnStartStopPane.add(startAllbtn);
		btnStartStopPane.add(Box.createRigidArea(new Dimension(10, 0)));
		btnStartStopPane.add(stopAllbtn);
		btnStartStopPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		btnStartStopPane.setAlignmentY(Component.TOP_ALIGNMENT);
		btnStartStopPane.setPreferredSize(btnStartStopPane.getPreferredSize());
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.Y_AXIS));
		buttonPane.setBorder(padding);
		buttonPane.add(startBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(0, 10)));
		buttonPane.add(btnStartStopPane);
		buttonPane.add(Box.createRigidArea(new Dimension(0, 10)));
		buttonPane.add(setSelectedBtn);
		buttonPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPane.setAlignmentY(Component.TOP_ALIGNMENT);
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(listPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.EAST);
		
		// set frame size
		//setSize(600, 600);
		setPreferredSize(getPreferredSize());
		pack();
		
		// center
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
	}
	
	/**
	 * Inner class to generate agents table
	 */
	private class AgentTableModel extends AbstractTableModel {
		private String[] columnNames = {"Agent Name", "Status" };
	
		public int getColumnCount() {
            return columnNames.length;
        }
	
		public int getRowCount() {
            return retailerAgents.size();
        }
	
		public String getColumnName(int col) {
            return columnNames[col];
        }
 
        public Object getValueAt(int row, int col) {
            try {
            	if ( col == 0 ) {
            		String name = retailerAgents.get(row).getName();
            		return name.substring(0, name.indexOf("@"));
            	}else { // col = 1
            		if ( retailerAgents.get(row).getState().getCode() == AgentState.cAGENT_STATE_SUSPENDED ) {
            			return "Stopped";
            		}else {
            			return "Started";
            		}
            	}
			} catch (StaleProxyException e) {
				e.printStackTrace();
				return "NAN";
			}
        }
        
        public void setValueAt(Object value, int row, int col) {
        	String status = (String) value;
        	if ( status.toLowerCase() == "start" ) {
        		try {
					retailerAgents.get(row).activate();;
				} catch (StaleProxyException e) {
					e.printStackTrace();
				}
        	}else {
        		try {
					retailerAgents.get(row).suspend();
				} catch (StaleProxyException e) {
					e.printStackTrace();
				}
        	}
        	
        	// update table
        	fireTableCellUpdated(row, col);
        }
        
	}
}
