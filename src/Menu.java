import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;


public class Menu {
	
	public Menu(JMenuBar mb, boolean isDropWindow){
		ActionListener listener = new MenuAction();
		
		JMenu m;
		JMenuItem mi;
		int s=InputEvent.CTRL_DOWN_MASK;
		if(isMacOSX()){
			s = InputEvent.META_DOWN_MASK;
		}

	    {
		    // Fileメニュー
		    m=new JMenu(International.getText("File"));
		    mb.add(m);
		    m.add(mi = new JMenuItem(International.getText("New Picture from Clipboard")));
		    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, s));
		    mi.addActionListener(listener);
		    
		    m.add(mi = new JMenuItem(International.getText("Open…")));
		    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, s));
		    mi.addActionListener(listener);
		    
			m.addSeparator();
		    
		    m.add(mi = new JMenuItem(International.getText("Close")));
		    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, s));
		    mi.addActionListener(listener);
		    
		    if(!isDropWindow){
		    	m.add(mi = new JMenuItem(International.getText("Save as PNG…")));
			    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, s));
			    mi.addActionListener(listener);
			    
			    m.add(mi = new JMenuItem(International.getText("Save as GIF…")));
			    mi.addActionListener(listener);
		    }
		    
			if(!isMacOSX()){
				m.addSeparator();
			    m.add(mi = new JMenuItem(International.getText("Quit")));
			    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, s));
			    mi.addActionListener(listener);
			}
	    }
	    
	    {
		    // Editメニュー
		    m=new JMenu(International.getText("Edit"));
		    mb.add(m);

		    if(!isDropWindow){
			    m.add(mi = new JMenuItem(International.getText("Index Color…")));
			    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, s));
			    mi.addActionListener(listener);
				m.addSeparator();
		    }

		    
		    m.add(mi = new JMenuItem(International.getText("Cut")));
		    mi.setEnabled(false);
		    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, s));
		    mi.addActionListener(listener);

		    m.add(mi = new JMenuItem(International.getText("Copy")));
		    mi.setEnabled(!isDropWindow);
		    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, s));
		    mi.addActionListener(listener);
		    
		    m.add(mi = new JMenuItem(International.getText("Paste")));
		    mi.setEnabled(false);
		    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, s));
		    mi.addActionListener(listener);

			m.addSeparator();
			
		    m.add(mi = new JMenuItem(International.getText("Clear")));
		    mi.setEnabled(false);
		    mi.addActionListener(listener);
		    
		    m.add(mi = new JMenuItem(International.getText("Select All")));
		    mi.setEnabled(false);
		    mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, s));
		    mi.addActionListener(listener);

			m.addSeparator();
			
		    m.add(mi = new JMenuItem(International.getText("Version Info…")));
		    mi.addActionListener(listener);
	    }
	}
	
	boolean isMacOSX(){
		return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
	}
  			
	/*public static boolean changeMenuName(String menu, String item, String name){
		JMenuItem mi = searchMenuItem(DropWindow.dropWindow.getJMenuBar(), menu, item);
		if(mi==null) return false;
		
		mi.setText(International.getText(name));
		return true;
	}
	
	public static boolean changeEnabled(String menu, String item, boolean enabled){
		JMenuItem mi = searchMenuItem(DropWindow.dropWindow.getJMenuBar(), menu, item);
		if(mi==null) return false;
		
		mi.setEnabled(enabled);
		return true;
	}
		
	public static boolean changeSelected(String menu, String item, boolean selected){
		JCheckBoxMenuItem mi = (JCheckBoxMenuItem)searchMenuItem(DropWindow.dropWindow.getJMenuBar(), menu, item);
		if(mi==null) return false;
		
		mi.setSelected(selected);
		return true;
	}
	
	public static JMenuItem searchMenuItem(JMenuBar mb, String menu, String item){
		if(mb==null) return null;
		int count = mb.getMenuCount();
		//そのまま探す
		for(int i=0; i<count; i++){
			JMenu m = mb.getMenu(i);
			if(m.getText().equals(menu)){
				int itemcount = m.getItemCount();
				for(int j=0; j<itemcount; j++){
					JMenuItem mi = m.getItem(j);
					if(mi.getText().equals(item)){
						return mi;
					}
				}
			}
		}
		//英語メニューで探す
		for(int i=0; i<count; i++){
			JMenu m = mb.getMenu(i);
			if(International.getEngText(m.getText()).equals(menu)){
				int itemcount = m.getItemCount();
				for(int j=0; j<itemcount; j++){
					JMenuItem mi = m.getItem(j);
					if(mi!=null && International.getEngText(mi.getText()).equals(item)){
						return mi;
					}
				}
			}
		}
		
		return null;
	}*/
}


//メニュー動作
class MenuAction implements ActionListener {
	public void actionPerformed (ActionEvent e) {
		String cmd = e.getActionCommand();

		Frame[] frame = Frame.getFrames();
		for(int i=0; i<frame.length; i++){
			if(frame[i].isActive()) {
				doMenu(cmd, (ImageWindow)frame[i]);
				return;
			}
		}

		if(IndexColorDialog.dialog!=null && IndexColorDialog.dialog.isActive()) {
			doMenu(cmd, IndexColorDialog.dialog.owner);
			return;
		}
		
		//アクティブなフレームが見つからなかった
		new AnswerDialog(DropWindow.dropWindow, International.getDialogText("Error: No frame to do menu."), null, "OK", null, null);
	}
	
	public void doMenu(String in_cmd, ImageWindow window)
	{
		String cmd = International.getEngText(in_cmd);

		if(cmd.equalsIgnoreCase("Quit")){
			System.exit(0);
		}
		else if(cmd.equalsIgnoreCase("Open…")){
			JFileChooser chooser = new JFileChooser();
			if(window.orgfilepath!=null){
				chooser.setCurrentDirectory(new File(new File(window.orgfilepath).getParent()));
			}
			chooser.setDialogTitle(International.getDialogText("Open Picture File"));
			chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			int selected = chooser.showOpenDialog(window);
			if (selected == JFileChooser.APPROVE_OPTION) {
				String path = chooser.getSelectedFile().getPath();

				ImageWindow imgWin = new ImageWindow();
				imgWin.setImage(null, path);
				imgWin.setLocationRelativeTo(window);
			}
		}
		else if(cmd.equalsIgnoreCase("Index Color…")){
			IndexColorDialog.openIndexColorDialog(window, false);
		}
		else if(cmd.equalsIgnoreCase("Close")){
			if(window instanceof DropWindow){
				window.dispose();
			}
			else if(window instanceof ImageWindow){
				window.dispose();
				if(IndexColorDialog.dialog!=null && IndexColorDialog.dialog.owner == window){
					IndexColorDialog.dialog.dispose();
					IndexColorDialog.dialog.owner = null;
				}
			}
		}
		else if(cmd.equalsIgnoreCase("Save as PNG…") ||
				cmd.equalsIgnoreCase("Save as GIF…"))
		{
			String ext = "";
			if(cmd.equalsIgnoreCase("Save as PNG…")) ext = "png";
			else ext = "gif";
			
			if(window.getImage() == null){
				new AnswerDialog(window, International.getDialogText("Image is empty."), null, "OK", null, null);
				return;
			}
			/*else if(window.colors>1024){
				new AnswerDialog(window, International.getDialogText("Can't save full color Image."), null, "OK", null, null);
				return;
			}*/
			else if(window.colors>256 && ext.equals("gif")){
				new AnswerDialog(window, International.getDialogText("Can't save over 256 colors Image."), null, "OK", null, null);
				return;
			}
			
			JFileChooser chooser;
			if(window.orgfilepath!=null){
				String filePath;
				if(window.orgfilepath.lastIndexOf(".")>=0){
					filePath = window.orgfilepath.substring(0,window.orgfilepath.lastIndexOf("."))+"."+ext;
				}
				else{
					filePath = window.orgfilepath+"."+ext;
				}
				chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File(new File(window.orgfilepath).getParent()));
				chooser.setSelectedFile(new File(filePath));
			}
			else{
				chooser = new JFileChooser();
			}
			chooser.setDialogTitle(International.getDialogText("Save Picture File"));
			int ret = chooser.showSaveDialog(window);
			if(ret != JFileChooser.APPROVE_OPTION){
				//保存しない
				return;
			}
			String path = chooser.getSelectedFile().getPath();
			
			window.saveImage(path, ext);
		}
		else if(cmd.equalsIgnoreCase("Copy"))
		{
			if(window.getImage()!=null){
				setClipboardImage(window.getImage());
			}
			else{
				new AnswerDialog(window, International.getDialogText("Image is empty."), null, "OK", null, null);
			}
		}
		else if(cmd.equalsIgnoreCase("New Picture from Clipboard"))
		{
			Image img = getClipboardImage();
			if(img!=null){
				int width = img.getWidth(window);
				int height = img.getHeight(window);
				BufferedImage newbi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				newbi.createGraphics().drawImage(img, 0, 0, window);

				ImageWindow imgWin = new ImageWindow();
				imgWin.setImage(newbi, null);
				imgWin.setTitle("Clipboard");
			}
			else{
				new AnswerDialog(window, International.getDialogText("Image is empty."), null, "OK", null, null);
			}
		}
		else if(cmd.equalsIgnoreCase("Version Info…"))
		{
			new AnswerDialog(window, International.getDialogText(PictCafe.AppName+" "+PictCafe.AppVersion + " by "+PictCafe.AuthorName), null, "OK", null, null);
		}
	}
	

	public static Image getClipboardImage() {
		Toolkit kit = Toolkit.getDefaultToolkit();
		Clipboard clip = kit.getSystemClipboard();

		try {
			return (Image) clip.getData(DataFlavor.imageFlavor);
		} catch (UnsupportedFlavorException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}
	
	
	public static void setClipboardImage(BufferedImage img) {
		Toolkit kit = Toolkit.getDefaultToolkit();
		Clipboard clip = kit.getSystemClipboard();

		ImageSelection is = new ImageSelection(img, img.getWidth(), img.getHeight());
		clip.setContents(is, is);
	}
}


class ImageSelection implements Transferable, ClipboardOwner {

	protected Image data;

	public ImageSelection(BufferedImage image, int width, int height) {
		this.data = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)this.data.getGraphics();
		g.setColor(Color.WHITE);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
		g.fillRect(0, 0, width, height);
		g = (Graphics2D)this.data.getGraphics();
		g.drawImage(image, 0, 0, null);
	}

	//@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.imageFlavor };
	}

	//@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return DataFlavor.imageFlavor.equals(flavor);
	}

	//@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (DataFlavor.imageFlavor.equals(flavor)) {
			return data;
		}
		throw new UnsupportedFlavorException(flavor);
	}

	//@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		this.data = null;
	}
}
