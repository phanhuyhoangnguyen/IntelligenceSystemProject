package GUI;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;

import EnergyAgents.PrintAgent;
import GUI.RetailerGUIDetails.RetailerFrame;

public class PrintGUI {
	
	private PrintAgent myAgent;
	private StringBuilder printText;
	
	private JEditorPane editorPane;
	
	public PrintGUI ( PrintAgent printAgent ) {
		myAgent = printAgent;
		printText = new StringBuilder();
		editorPane = new JEditorPane();
	}
	
	public void clearText() {
		printText.setLength(0);
		printText = new StringBuilder();
		editorPane.setText("");
	}
	
	public void append(String text) {
		printText.append(text + "<br>\n\r ");
		editorPane.setText(printText.toString());
	}
	
	public void showGUI() {
		EventQueue.invokeLater(new Runnable() {
	         public void run() {
	        	 PrintFrame frame = new PrintFrame();
	        	 frame.setVisible(true);
	         }
	      });
	}
	
	private class PrintFrame extends JFrame {
		private String title = "Print Agent: " + myAgent.getLocalName();
		
		public PrintFrame() {
			
			setTitle(title);
			setResizable(false);
			
			JLabel lblTitle = new JLabel(title);
			
			
			editorPane = new JEditorPane();
			editorPane.setEditable(false);
			editorPane.setContentType("text/html");
			editorPane.setEditorKit(new HTMLEditorKit());
			
			editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			Font font = new Font(editorPane.getFont().getFontName(), Font.PLAIN, editorPane.getFont().getSize()+10);
			editorPane.setFont(font);
			
			editorPane.setText(printText.toString());
			
			JScrollPane scrollPan = new JScrollPane(editorPane);
			
			
			// add to frame
			Container pane = getContentPane();
			pane.add(lblTitle, BorderLayout.NORTH);
			pane.add(scrollPan, BorderLayout.CENTER);
			
			setLocationRelativeTo(null);
			setLocation(1, 1);
			
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int w = (int)screenSize.getWidth() / 2;
			int h = (int)screenSize.getHeight();
			setSize(w, h);
			
			// remove the agent when close	
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					myAgent.doDelete();
				}
			} );
			
			setResizable(true);
			
		}
		
	}
	
}
