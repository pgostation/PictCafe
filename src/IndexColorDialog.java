import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


class IndexColorDialog extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	ImageWindow owner;
	static IndexColorDialog dialog;
	static private JComboBox colorcombo;
	static private JComboBox dithercombo;
	static private JComboBox selcombo;
	static private JComboBox formatcombo;
	static private CPButton cpbtn[] = new CPButton[8];
	
	
	private IndexColorDialog(ImageWindow owner2) {
		super();
		owner = owner2;
		ICDialogListener listener = new ICDialogListener();
		addWindowListener(listener);
	}


	static IndexColorDialog openIndexColorDialog(ImageWindow owner, boolean isSaveAll) {
		if(owner instanceof DropWindow){
			return null;
		}
		
		if(dialog==null){
			dialog = new IndexColorDialog(owner);
		}
		dialog.owner = owner;

		if(!isSaveAll){
			dialog.getContentPane().setLayout(new GridLayout(4,1));
			dialog.setTitle(International.getDialogText("Index Color Settings"));
			
			//ダイアログ内を作る
			dialog.makeDialog();
			
			dialog.pack();
			dialog.setBounds(owner.getX()+owner.getWidth()/2-dialog.getWidth()/2,Math.max(0,owner.getY()-dialog.getHeight()),dialog.getWidth(),dialog.getHeight());
			
			dialog.setResizable(false);
			dialog.setVisible(true);
		}
		
		if(isSaveAll){
			dialog.doAction("AutoSave");
		}
		
		return dialog;
	}
	

	class ICDialogListener implements WindowListener
	{
		//@Override
		public void windowActivated(WindowEvent arg0) {
			if(dialog!=null && dialog.owner!=null && (!dialog.owner.isVisible() || !dialog.owner.isEnabled())){
				dialog.dispose();
				dialog.setVisible(false);
				dialog.owner = null;
				//dialog = null;
			}
			else if(dialog!=null && dialog.owner!=null && Frame.getFrames().length>=2 && Frame.getFrames()[1]!=dialog.owner){
				dialog.owner.setAlwaysOnTop(true);
				dialog.owner.setAlwaysOnTop(false);
				dialog.setAlwaysOnTop(true);
				dialog.setAlwaysOnTop(false);
			}
		}
		//@Override
		public void windowClosed(WindowEvent arg0) {
		}
		//@Override
		public void windowClosing(WindowEvent arg0) {
			if(dialog!=null){
				dialog.owner = null;
			}
			//dialog = null;
		}
		//@Override
		public void windowDeactivated(WindowEvent arg0) {
		}
		//@Override
		public void windowDeiconified(WindowEvent arg0) {
		}
		//@Override
		public void windowIconified(WindowEvent arg0) {
		}
		//@Override
		public void windowOpened(WindowEvent arg0) {
		}
	}
	
	
	private void makeDialog()
	{
		this.getContentPane().removeAll();
		
		{
			JPanel panel = new JPanel();
			Border aquaBorder = UIManager.getBorder("TitledBorder.aquaVariant");
			if(aquaBorder==null){
				aquaBorder = new EtchedBorder();
			}
			panel.setBorder( new TitledBorder( aquaBorder, International.getDialogText("Colors and Dithering") ) );
			getContentPane().add(panel);
			
			if(colorcombo==null){
				String[] value = new String[]{
						/*"Auto",*/"512","256","128","64","32","16","12","8","6","4","3","2"
						};
				colorcombo = new JComboBox(value);
				colorcombo.setName("Colors");
				colorcombo.setSelectedIndex(1);
				colorcombo.setMaximumRowCount(32);
				colorcombo.addActionListener(this);
			}
			panel.add(colorcombo);

			if(dithercombo==null){
				String[] value = new String[]{
						International.getDialogText("None"),
						"Floyd-Steinburg",
						"Bill Atkinson"
						/*,"Pattern"*/
						};
				dithercombo = new JComboBox(value);
				dithercombo.setName("Dither");
				//dithercombo.setSelectedIndex(0);
				dithercombo.setMaximumRowCount(32);
				dithercombo.addActionListener(this);
			}
			panel.add(dithercombo);

			{
				panel = new JPanel();
				panel.setBorder( new TitledBorder( aquaBorder, International.getDialogText("Protected Color") ) );
				getContentPane().add(panel);
				
				//色選択
				if(selcombo==null){
					String[] value = new String[]{
							International.getDialogText("None"),
							"1",
							"2",
							"3",
							"4",
							"5",
							"6",
							"7",
							"8"
							};
					selcombo = new JComboBox(value);
					selcombo.setName("Protect");
					//dithercombo.setSelectedIndex(0);
					selcombo.setMaximumRowCount(32);
					selcombo.addActionListener(this);
				}
				panel.add(selcombo);
				
				for(int i=0; i<selcombo.getSelectedIndex(); i++){
					if(cpbtn[i] == null){
						cpbtn[i] = new CPButton(Color.WHITE);
						cpbtn[i].setFocusable(false);
					}
					panel.add(cpbtn[i]);
				}
			}
			
			panel = new JPanel();
			panel.setBorder( new TitledBorder( aquaBorder, International.getDialogText("Save Format") ) );
			getContentPane().add(panel);

			if(formatcombo==null){
				String[] value = new String[]{
						"PNG","GIF"
						};
				formatcombo = new JComboBox(value);
				formatcombo.setName("Format");
				//formatcombo.setSelectedIndex(0);
				formatcombo.setMaximumRowCount(32);
				formatcombo.addActionListener(this);
			}
			panel.add(formatcombo);
		}
		
		//ok, cancel
		{
			JPanel panel = new JPanel();
			getContentPane().add(panel);
			
			JButton button = new JButton(International.getDialogText("Cancel"));
			button.setName("Cancel");
			button.addActionListener(this);
			panel.add(button);
			
			button = new JButton(International.getDialogText("Preview"));
			button.setName("Preview");
			button.addActionListener(this);
			panel.add(button);
			
			button = new JButton(International.getDialogText("Save"));
			button.setName("Save");
			button.addActionListener(this);
			panel.add(button);

			//ファイルを複数開いているかどうか
			int filenum = 0;
			Frame[] frame = Frame.getFrames();
			for(int i=0; i<frame.length; i++){
				if(frame[i] instanceof ImageWindow) {
					ImageWindow imgWin = (ImageWindow)frame[i];
					if(imgWin.isVisible() && imgWin.filepath != null){
						filenum++;
					}
				}
			}
			
			{
				button = new JButton(International.getDialogText("Save All"));
				button.setName("Save All");
				if(filenum<=1) {
					button.setEnabled(false);
				}
				button.addActionListener(this);
				panel.add(button);
			}
		}
		setVisible(true);		
		//this.getContentPane().repaint();
	}

	
	public void actionPerformed(ActionEvent e) {
		String name = ((JComponent)e.getSource()).getName();
		String cmd = International.getDialogEngText(name);
		
		doAction(cmd);
	}
	
	private void doAction(String cmd){
		if(dialog.owner==null){
			return;
		}
		
		if(cmd!=null && cmd.equals("Cancel")){
			//this.setEndFlag(true);
			this.dispose();
			return;
		}
		
		String colorstr = (String)colorcombo.getSelectedItem();
		int color = 0;
		if(International.getDialogEngText(colorstr).equals("Auto")){
			color = -1;
		}
		else{
			color = Integer.valueOf(colorstr);
		}
		int dithermode = dithercombo.getSelectedIndex()-1;
		
		Color[] protects = null;
		if(selcombo.getSelectedIndex()>0){
			protects = new Color[selcombo.getSelectedIndex()];
			for(int i=0; i<selcombo.getSelectedIndex(); i++){
				if(cpbtn[i]!=null && cpbtn[i].color!=null){
					protects[i] = cpbtn[i].color;
				}
			}
		}
		
		
		if(cmd!=null && cmd.equals("Protect")){
			makeDialog();
		}
		else if(cmd!=null && (cmd.equals("Save") || cmd.equals("Preview") || cmd.equals("AutoSave"))){
			BufferedImage bi;
			bi = IndexColor.makeIndexColor(this, color, protects, dithermode);

			if(bi==null){
				owner.progress(100);
				return;
			}
			
			ImageWindow imgWin = new ImageWindow();
			imgWin.setImage(bi, null);
			imgWin.colors = color;
			imgWin.orgfilepath = owner.orgfilepath;
			imgWin.setTitle(International.getDialogText(" Colors:")+color);

			if(cmd.equals("Save") || cmd.equals("AutoSave")){
				String ext = formatcombo.getSelectedIndex()==0?"png":"gif";

				//連番で被らない番号を付ける
				String filePath = "";
				if(owner.orgfilepath!=null){
					filePath = owner.orgfilepath;
				}
				if(filePath.lastIndexOf(".")>=0){
					filePath = filePath.substring(0,filePath.lastIndexOf("."))+"."+ext;
				}
				else{
					filePath = filePath+"."+ext;
				}
				if(new File(filePath).exists()){
					//上書き防止
					for(int i=1; i<1000; i++){
						filePath = "";
						if(owner.orgfilepath!=null){
							filePath = owner.orgfilepath;
						}
						String numStr = Integer.toString(i);
						//if(i==0) numStr = "";
						
						if(filePath.lastIndexOf(".")>=0){
							filePath = filePath.substring(0,filePath.lastIndexOf("."))+"_cnv"+numStr+"."+ext;
						}
						else{
							filePath = filePath+"_"+numStr+"."+ext;
						}
						if(!new File(filePath).exists()){
							break;
						}
					}
				}
				
				//保存処理
				imgWin.saveImage(filePath, ext);
				
				if(cmd.equals("AutoSave")){
					imgWin.setImage(null, null);
					imgWin.dispose();
					this.dispose();
				}
				
				System.gc();
				
				return;
			}
		}
		else if(cmd!=null && cmd.equals("Save All")){
			new saveAllThread().start();
		}
	}
	
	
	class saveAllThread extends Thread{
		public void run(){
			while(true){
				boolean flag = false;
				Frame[] frame = Frame.getFrames();
				for(int i=frame.length-1; i>=0; i--){
					if(frame[i] instanceof ImageWindow) {
						ImageWindow imgWin = (ImageWindow)frame[i];
						if(imgWin.isVisible() && imgWin.filepath != null){
							flag = true;
							IndexColorDialog dlog = openIndexColorDialog(imgWin, true);
	
							while(dlog!=null && dlog.isValid() && dlog.owner.isVisible()){
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
								}
							}
							
							if(dlog.owner!=null){
								dlog.owner.setImage(null,null);
								dlog.owner.dispose();
							}
						}
					}
				}
				if(flag==false) break;
			}
			
			new AnswerDialog(dialog, International.getDialogText("Save complete."), null, "OK", null, null);
		}
	}
	
	
	class CPButton extends JButton {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	    CPButtonListener listener = new CPButtonListener();
	    Color color;
		
		CPButton(Color in_color){
			super("");
	        this.setFocusable(false);
			BufferedImage bi = new BufferedImage(16,12,BufferedImage.TYPE_INT_RGB);
			Graphics g = bi.createGraphics();
			g.setColor(in_color);
			g.fillRect(0,0,16,12);
			this.setIcon(new ImageIcon(bi));
			this.color = in_color;
			this.setBounds(0,0,24,20);
	        setMargin(new Insets(0,0,0,0));
	        this.addActionListener(listener);
		}
		
		public void makeIcon(Color col){
			this.color = col;
			BufferedImage bi = new BufferedImage(16,12,BufferedImage.TYPE_INT_RGB);
			Graphics g = bi.createGraphics();
			g.setColor(col);
			g.fillRect(0,0,16,12);
			this.setIcon(new ImageIcon(bi));
		}
	}


	class CPButtonListener implements ActionListener {
		//@Override
		public void actionPerformed (ActionEvent e) {
			CPButton btn = (CPButton)e.getSource();
			new CPButtonThread(btn, e).start();
		}

		class CPButtonThread extends Thread {
			CPButton btn;
			ActionEvent e;
			
			CPButtonThread(CPButton btn, ActionEvent e){
				super();
				this.btn = btn;
				this.e = e;
			}
			
			public void run(){
				Color col = ColorDialog.getColor(owner, btn.color, true);

				if(col != null){
					btn.makeIcon(col);
				}
			}
		}
	}

}
