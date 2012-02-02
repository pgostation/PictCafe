import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

//  PictCafe
//
//  Mac用減色ツールをJavaで作る
//
//  programmed by pgo
//  2011.2.6 -

//0.10 2010.02.16 first release


//入力方法
//  ウィンドウへのドロップ
//  コマンドラインから
//  ファイル選択ダイアログ

//読み込める形式
//  JavaのImageIOが読める形式
//  PICT

//保存形式
//  GIF
//  PNG8(256色以下のとき)
//  PNG24(512色以上のとき)

//アルファ未対応(50%をしきい値で透明色にする)
//透明色対応(#00FF00固定)


public class PictCafe {
	static String AppName = "PictCafe";
	static String AppVersion = "0.10";
	static String AuthorName = "pgo";
	//static String CurrentDir;

	static String usageStr = AppName+" "+AppVersion+"\n"+
		"Usage:\n"+
		"  -i     : Input file path\n"+
		/*"  -o     : Output file path (Default:[input filename]+pcafe)\n"+
		"  -f     : Output file format (Default: PNG)\n"+
		"\n"+
		"  -num   : Number of colors [2-1024, auto] (Default: auto)\n"+
		"  -dither: Dither mode [\"none\", \"floyd-steinburg\", \"atkinson\"] (Default: none)\n"+
		"  -mono  : Use monochrome mode\n"+
		"  -set   : Select colors (Example: 255,0,0)\n"+
		"  -red   : Red depth [2-8] (Default:6)\n"+
		"  -green : Green depth [2-8] (Default:8)\n"+
		"  -blue  : Blue depth [2-8] (Default:6)\n"+ */
		"";
	
	
    public static void main(final String[] args) {
		System.setProperty("com.apple.mrj.application.apple.menu.about.name",
				AppName);//about box
		
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.macos.useScreenMenuBar", "true");

		System.setProperty("apple.awt.rendering","speed");
		System.setProperty("apple.awt.graphics.UseQuartz","false");
		
		//初期化
		try {
			International.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//
		String filename = null;
		
    	if(args.length>0) {
    		//コマンドライン引数指定
    		for (int i=0; i<args.length; ++i) {
	            if ("-i".equals(args[i])) {
	                filename = args[++i];
	            } else {
	            	System.err.println("Error: Unknown Parameter String\n"+
	            			usageStr);
	            }
	        }
    	}
		
    	if(filename != null){
    		String[] fnames = filename.split("\n");
			//ArrayList<File> fileList = new ArrayList<File>();
    		for(int i=0; i<fnames.length; i++){
				//fileList.add(new File(fnames[i]));
				new ImageWindow().setImage(null, fnames[i]);
    		}
			//DropOpenThread.openDropOpenThread(fileList);
    		//IndexColorDialog.isSaveAll = false;
    	}else{
        	//ドロップウィンドウを作る
    		DropWindow.newDropWindow();
    	}
    }
}


class ImageWindow extends JFrame {
	private static final long serialVersionUID = 6229111224697328473L;
	
	JPanel mainPane;
	JScrollPane scrollPane;
	String filepath;
	String orgfilepath;
	private BufferedImage newbi;
	int colors;
	
	
	ImageWindow(){
		//ウィンドウ内部を作成
		scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);
		
		mainPane = new JPanel();
		mainPane.setLayout(new BorderLayout());
		scrollPane.add(mainPane);
		scrollPane.setViewportView(mainPane);
		
		//メニューを作成
		JMenuBar menubar = new JMenuBar();
		if(this instanceof DropWindow){
			new Menu(menubar, true);
		}
		else{
			new Menu(menubar, false);
		}
		setJMenuBar(menubar);
		
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
		    	dispose();
				
				Frame[] frame = Frame.getFrames();
				for(int i=0; i<frame.length; i++){
					if(frame[i].isVisible() /*&& frame[i].isValid()*/) {
						return;
					}
				}
				//見えているウィンドウがないので終了
				System.exit(0);
			}
		});
		
		ImgWinListener listener = new ImgWinListener(this);
		addWindowListener(listener);
	}
	

	class ImgWinListener implements WindowListener
	{
		ImageWindow owner;
		
		ImgWinListener(ImageWindow owner){
			this.owner = owner;
		}
		
		//@Override
		public void windowActivated(WindowEvent arg0) {
		}
		//@Override
		public void windowClosed(WindowEvent arg0) {
		}
		//@Override
		public void windowClosing(WindowEvent arg0) {
			if(IndexColorDialog.dialog!=null && IndexColorDialog.dialog.owner==owner){
				IndexColorDialog.dialog.dispose();
				IndexColorDialog.dialog.owner = null;
			}
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
	
	
	JDialog barDialog;
	JProgressBar bar;
	
	void progress(int percent){
		if(percent==0){
	    	//進捗表示を開始
			barDialog = new JDialog(this);
			barDialog.setUndecorated(true);
			bar = new JProgressBar();
			bar.setStringPainted(true);
			bar.setValue(0);
			barDialog.add(bar);
			barDialog.setBounds(this.getBounds().x,
					this.getBounds().y+this.getInsets().top,
					this.getBounds().width, 24);
			barDialog.setVisible(true);
			bar.setString(International.getDialogText("Converting..."));
		}
		else if(percent==100 && barDialog!=null){
			barDialog.setVisible(false);
			barDialog.dispose();
			barDialog = null;
			bar = null;
		}
		else if(bar!=null){
			bar.setValue(percent);
			bar.paintImmediately(bar.getBounds());
		}
	}
	
	
	public void saveImage(String path, String ext)
	{
		BufferedImage bi = getImage();
		
		if(bi==null){
			new AnswerDialog(this, International.getDialogText("Image is empty."), null, "OK", null, null);
			return;
		}
		
		if(colors>256){
			try {
				ImageIO.write(bi, ext, new File(path));
				setTitle(new File(path).getName());
			} catch (IOException e) {
				e.printStackTrace();
				new AnswerDialog(this, International.getDialogText("Save failure."), null, "OK", null, null);
			}
			return;
		}
	
		//使われているカラーを列挙
		byte[] a = new byte[256];
		boolean useAlpha = false;
		int alphaIndex = -1;
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		int colors = 0;
		DataBuffer db = bi.getRaster().getDataBuffer();
		int width = bi.getWidth();
		int height = bi.getHeight();
		for(int v=0; v<height; v++){
			for(int h=0; h<width; h++){
				int c = db.getElem(h+v*width);
				byte alpha = (byte)((c>>24)&0xFF);
				byte red = (byte)((c>>16)&0xFF);
				byte green = (byte)((c>>8)&0xFF);
				byte blue = (byte)((c>>0)&0xFF);
				if(/*!useAlpha &&*/ (alpha!=(byte)0xFF) ){
					if(!useAlpha) alphaIndex = colors;
					useAlpha = true;
					/*if(ext.equals("gif"))*/{
						alpha = 0;
						red = 0;
						green = (byte)0xFF;
						blue = 0;
					}
				}
				for(int i=0; ; i++){
					if(i==colors){
						if(colors>=256){
							new AnswerDialog(this, International.getDialogText("Picture contains over 256 colors."), null, "OK", null, null);
							return;
						}
						//　カラーを登録
						a[colors] = alpha;
						r[colors] = red;
						g[colors] = green;
						b[colors] = blue;
						colors++;
						break;
					}
					if((!useAlpha || alpha==a[i]) && red==r[i] && green==g[i] && blue==b[i]){
						break;
					}
				}
			}
		}
		
		//インデックスカラーイメージを作成
		IndexColorModel colormodel;
		if(useAlpha && ext.equals("png")){
			colormodel = new IndexColorModel(8 , colors , r , g , b, a);
		}
		else if(useAlpha && ext.equals("gif")){
			colormodel = new IndexColorModel(8 , colors , r , g , b, alphaIndex);
		}
		else{
			colormodel = new IndexColorModel(8 , colors , r , g , b);
		}
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colormodel);

		//これをやると自動でディザパターンになる
		//image.createGraphics().drawImage(DropWindow.dropWindow.newbi, 0, 0, null);

		DataBuffer imgdb = image.getRaster().getDataBuffer();
		for(int v=0; v<height; v++){
			for(int h=0; h<width; h++){
				int c = db.getElem(h+v*width);
				byte alpha = (byte)((c>>24)&0xFF);
				byte red = (byte)((c>>16)&0xFF);
				byte green = (byte)((c>>8)&0xFF);
				byte blue = (byte)((c>>0)&0xFF);
				if(alpha<=(byte)0x7F &&alpha>=0){
					imgdb.setElem(h+v*width, (byte)alphaIndex);
				}
				else{
					alpha=(byte) 0xFF;//暫定
				}
				for(int i=0; i<colors; i++){
					if((!useAlpha || alpha==a[i]) && red==r[i] && green==g[i] && blue==b[i]){
						imgdb.setElem(h+v*width, (byte)i);
						break;
					}
				}
			}
		}
		
		//保存
		try {
			ImageIO.write(image, ext, new File(path));
			setTitle(new File(path).getName()+International.getDialogText(" Colors:")+colors);
		} catch (IOException e) {
			e.printStackTrace();
			new AnswerDialog(this, International.getDialogText("Save failure."), null, "OK", null, null);
		}
	}
	
	public void setImage(BufferedImage bi, String filepath){
		newbi = bi;
		this.filepath = filepath;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mainPane.removeAll();
				
				ImageIcon icon = null;
				JLabel label = new JLabel();
				
				if(newbi!=null){
					icon = new ImageIcon(newbi);
					mainPane.setBounds(0,0,icon.getIconWidth(),icon.getIconHeight());
					
					label.setIcon(icon);
					mainPane.add("Center", label);
					
					int width = newbi.getWidth();
					int height = newbi.getHeight();
		
					Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
					if(d!=null){
						width = Math.min(d.width, width);
						height = Math.min(d.height-getInsets().top, height);
					}
					
					///owner.setResizable(true);
				}
				else if(ImageWindow.this.filepath!=null){
					ImageWindow.this.orgfilepath = ImageWindow.this.filepath;
					
					icon = new ImageIcon(ImageWindow.this.filepath);
					if(icon.getIconWidth()<=0 || icon.getIconHeight()<=0){
						new AnswerDialog(ImageWindow.this, International.getDialogText("Could't open the file."), null, "OK", null, null);
						ImageWindow.this.dispose();
						return;
					}
					if(Runtime.getRuntime().maxMemory()-(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())
							<1*icon.getIconWidth()*icon.getIconHeight()){
						return;
					}
					else{
						mainPane.setBounds(0,0,icon.getIconWidth(),icon.getIconHeight());
						
						label.setIcon(icon);
					}
					mainPane.add("Center", label);
					
					ImageWindow.this.setTitle(new File(ImageWindow.this.filepath).getName());
					
					colors = 16777216;
				}
		
				if(icon!=null){
					int width = icon.getIconWidth();
					int height = icon.getIconHeight();
			
					Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
					if(d!=null){
						width = Math.min(d.width, width);
						height = Math.min(d.height-getInsets().top, height);
					}
					
					ImageWindow.this.pack();
					ImageWindow.this.setLocationRelativeTo(null);
					Rectangle r = ImageWindow.this.getBounds();
				
					//なぜかこんなことをしないとスクロールで表示されていない部分がうまく描画できない
					ImageWindow.this.setBounds(r.x, r.y, width+getInsets().left+getInsets().right+5, height+getInsets().top+getInsets().bottom+5);
					ImageWindow.this.setBounds(r.x, r.y, width+getInsets().left+getInsets().right+4, height+getInsets().top+getInsets().bottom+4);
					
					
					///owner.setResizable(true);
				}
			}
		});
	}
	
	
	public BufferedImage getImage(){
		if(newbi != null){
			return newbi;
		}
		else if(filepath != null){
			BufferedImage bi = null;
			try{
				bi = javax.imageio.ImageIO.read(new File(filepath));
				if(bi==null){
					bi = PictFile.loadPICT(filepath);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return bi;
		}
		
		return null;
	}
}


class DropWindow extends ImageWindow {
	private static final long serialVersionUID = -3278876000138350669L;
	
	static DropWindow dropWindow;
	DropTarget drop;
	MyDropTargetListener droplistener;
	
	
	static public void newDropWindow(){
		if(dropWindow==null){
			//ウィンドウ作成
			dropWindow = new DropWindow();
		}
		dropWindow.setVisible(true);
	}
	
	
	DropWindow(){
		super();

		//
		resetMe();
		
		//ファイルのドロップに対応
		droplistener = new MyDropTargetListener();
		drop = new DropTarget(mainPane, droplistener);
	}

	
	void resetMe(){
		setImage(null, null);
		
		mainPane.removeAll();
		
		setTitle(International.getDialogText("Drop picture files here"));
		
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		if(d!=null){
			setBounds(d.width/2-400/2, d.height/2-320/2-20, 400, 320+20);
		}else{
			setBounds(0, 0-320-20, 400, 320+20);
		}
		setVisible(true);
	}
	
	
}
		

class MyDropTargetListener extends DropTargetAdapter {
	//@Override
	public void drop(DropTargetDropEvent e) {
		try {
			Transferable transfer = e.getTransferable();
			if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			{
				e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				@SuppressWarnings("unchecked")
				List<File> fileList = 
					(List<File>) (transfer.getTransferData(DataFlavor.javaFileListFlavor));
				DropOpenThread.openDropOpenThread(fileList);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}


//ドロップで同時に開くウィンドウを8枚に制限
//サムネイル表示ウィンドウを作りたいところ
class DropOpenThread extends Thread{
	static ArrayList<File> fileList;
	
	static void openDropOpenThread(List<File> infileList){
		if(fileList==null){
			fileList = new ArrayList<File>();
			synchronized(fileList)
			{
				for(int i=0; i<infileList.size(); i++){
					fileList.add(infileList.get(i));
				}
			}
			new DropOpenThread().start();
		}
		else{
			synchronized(fileList)
			{
				for(int i=0; i<infileList.size(); i++){
					fileList.add(infileList.get(i));
				}
			}
		}
	}
	
	public void run(){
		while(fileList.size()>0){
			if(countDocuments()<=8){
				ImageWindow imgWin = new ImageWindow();
				synchronized(fileList)
				{
					imgWin.setImage(null, fileList.get(0).toString());
					fileList.remove(0);
				}
				imgWin.setLocationRelativeTo(DropWindow.dropWindow);
			}
			
			try {
				sleep(500);
			} catch (InterruptedException e) {
			}
		}
		
		fileList = null;
	}
	
	//書類を持っているウィンドウの数を数える
	int countDocuments(){
		int count = 0;
		Frame[] frame = Frame.getFrames();
		for(int i=0; i<frame.length; i++){
			if(frame[i].isVisible() && frame[i] instanceof ImageWindow) {
				ImageWindow imgwin = (ImageWindow)frame[i];
				if(imgwin.filepath!=null) count++;
			}
		}
		
		return count;
	}
}
