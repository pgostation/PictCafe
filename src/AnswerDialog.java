import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class AnswerDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	static String clicked="";
	static String inputText="";
	static JTextField inputArea;
	static JButton defaultButton;
	
	AnswerDialog(Frame owner, String text, String input, String b1, String b2, String b3) {
		super(owner,true);
		make(owner.getBounds(), text, input, b1, b2, b3);
	}
	
	AnswerDialog(Dialog owner, String text, String input, String b1, String b2, String b3) {
		super(owner,true);
		make(owner.getBounds(), text, input, b1, b2, b3);
	}
	
	void make(Rectangle ownerBounds, String text, String input, String b1, String b2, String b3) {
		getContentPane().setLayout(new BorderLayout());

		//パネルを追加する
		JPanel btmPanel = new JPanel();
		getContentPane().add("South",btmPanel);
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		getContentPane().add("North",topPanel);

		String[] texts2 = text.split("\r");
		String text2 = "";
		for(int i=0; i<texts2.length; i++){
			text2 += texts2[i]+"\n";
		}
		JTextArea area = new JTextArea(text2);
		String[] texts = text2.split("\n");
		area.setSize(380, 18+18*texts.length);
		//area.setPreferredSize(new Dimension(380, 18+18*texts.length));
		area.setMargin(new Insets(16,16,2,16));
		area.setLineWrap(true);
		area.setOpaque(false);
		area.setEditable(false);
		area.setFocusable(false);
		topPanel.add(area);

		if(b1!=null){
			JButton btn1 = new JButton(b1);
			btn1.addActionListener(this);
			btmPanel.add(btn1);
			defaultButton = btn1;
		}
		if(b2!=null){
			JButton btn2 = new JButton(b2);
			btn2.addActionListener(this);
			btmPanel.add(btn2);
			defaultButton = btn2;
		}
		if(b3!=null){
			JButton btn3 = new JButton(b3);
			btn3.addActionListener(this);
			btmPanel.add(btn3);
			getRootPane().setDefaultButton(btn3);
			defaultButton = btn3;
		}
		
		if(input != null){
			JTextField area2 = new JTextField(input);
			area2.setSize(350, 24);
			area2.setMargin(new Insets(16,16,16,16));
			topPanel.add(area2);
			inputArea = area2;
		}

		int h = area.getPreferredSize().height;
		if(h>500)h=500;
		if(inputArea!=null) h+= inputArea.getPreferredSize().height;
		setBounds(ownerBounds.x+ownerBounds.width/2-200,ownerBounds.y+ownerBounds.height/2-h/2-40,400,h+48);
		setUndecorated(true);//タイトルバー非表示
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(defaultButton!=null) defaultButton.requestFocus();
			}
		});
		
		setVisible(true);
		
	}
	
	public void actionPerformed(ActionEvent e) {
		clicked = e.getActionCommand();
		if(inputArea != null) inputText = inputArea.getText();
		inputArea = null;
		this.dispose();
	}
}
