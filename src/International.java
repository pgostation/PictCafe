import java.util.HashMap;


public class International {
	static HashMap<String,String> menuHash = new HashMap<String,String>(); //�������̂��߂̃c���[
	static HashMap<String,String> menuRHash = new HashMap<String,String>(); //�t����
	static HashMap<String,String> dialogHash = new HashMap<String,String>(); //�������̂��߂̃c���[
	static HashMap<String,String> dialogRHash = new HashMap<String,String>(); //�t����

	public static void init() throws Exception{
		if(System.getProperty("user.language").equals("ja")){
			init("Japanese");
		}
		else{
			init("English");
		}
	}
	
	public static void init(String lang) throws Exception{
		if(lang.equals("Japanese") ){
			//���j���[
			putHash(menuHash, menuRHash,"File","�t�@�C��");
			putHash(menuHash, menuRHash,"New Picture from Clipboard","�N���b�v�{�[�h����V�K�쐬");
			putHash(menuHash, menuRHash,"Open�c","�J���c");
			putHash(menuHash, menuRHash,"Close","����");
			putHash(menuHash, menuRHash,"Save as PNG�c","PNG�`���ŕۑ��c");
			putHash(menuHash, menuRHash,"Save as GIF�c","GIF�`���ŕۑ��c");
			putHash(menuHash, menuRHash,"Quit","�I��");

			putHash(menuHash, menuRHash,"Edit","�ҏW");
			putHash(menuHash, menuRHash,"Index Color�c","���F�c");
			putHash(menuHash, menuRHash,"Cut","�J�b�g");
			putHash(menuHash, menuRHash,"Copy","�R�s�[");
			putHash(menuHash, menuRHash,"Paste","�y�[�X�g");
			putHash(menuHash, menuRHash,"Clear","����");
			putHash(menuHash, menuRHash,"Select All","���ׂĂ�I��");
			putHash(menuHash, menuRHash,"Version Info�c","�o�[�W�������c");
			
			//
			putHash(dialogHash, dialogRHash,"Drop picture files here","�摜�t�@�C���������Ƀh���b�v");
			putHash(dialogHash, dialogRHash,"Open Picture File","�摜�t�@�C�����J��");
			putHash(dialogHash, dialogRHash,"Save Picture File","�摜�t�@�C����ۑ�");
			putHash(dialogHash, dialogRHash,"Index Color Settings","���F�ݒ�");
			
			putHash(dialogHash, dialogRHash,"Colors and Dithering","�F���ƃf�B�U�����O");
			putHash(dialogHash, dialogRHash,"Save Format","�ۑ��`��");
			putHash(dialogHash, dialogRHash,"Protected Color","�ی�J���[");
			putHash(dialogHash, dialogRHash,"Auto","����");
			putHash(dialogHash, dialogRHash,"None","�Ȃ�");
			
			putHash(dialogHash, dialogRHash,"Cancel","�L�����Z��");
			putHash(dialogHash, dialogRHash,"Preview","�v���r���[");
			putHash(dialogHash, dialogRHash,"Save","�ۑ�");
			putHash(dialogHash, dialogRHash,"Save All","���ׂĕۑ�");

			putHash(dialogHash, dialogRHash,"Converting...","������...");
			putHash(dialogHash, dialogRHash," Colors:"," �F��:");
			putHash(dialogHash, dialogRHash,"Choose from Screen","��ʂ̐F���擾");

			//�G���[
			putHash(dialogHash, dialogRHash,"Could't open the file.","�t�@�C�����J���܂���ł���");
			putHash(dialogHash, dialogRHash,"Can't save full color Image.","�t���J���[�̉摜�ł��B���F���ĕۑ����Ă��������B");
			putHash(dialogHash, dialogRHash,"Can't save over 256 colors Image.","256�F�𒴂���摜�͂��̃t�H�[�}�b�g�ŕۑ��ł��܂���B");
			putHash(dialogHash, dialogRHash,"Save failure.","�ۑ��Ɏ��s���܂���");
			putHash(dialogHash, dialogRHash,"Image is empty.","�摜������܂���");
			putHash(dialogHash, dialogRHash,"Memory overflow.","���蓖�ă�����������܂���");
			putHash(dialogHash, dialogRHash,"Save complete.","�������������܂���");
			
		}
		else if(lang.equals("English") ){
			//�������Ȃ�
		}
		else {
			throw new Exception("Unsupported Language \""+lang+"\"");
		}
	}
	
	private static void putHash(HashMap<String,String> nh, HashMap<String,String> rh, String eng, String other){
		nh.put(eng, other);
		if(rh!=null){
			rh.put(other, eng);
		}
	}
	
	public static String getText(String name){
		String ret = menuHash.get(name);
		if(ret==null) return name;
		else return ret;
	}
	
	public static String getEngText(String name){
		String ret = menuRHash.get(name);
		if(ret==null) return name;
		else return ret;
	}
	
	public static String getDialogText(String name){
		String ret = dialogHash.get(name);
		if(ret==null) return name;
		else return ret;
	}
	
	public static String getDialogEngText(String name){
		String ret = dialogRHash.get(name);
		if(ret==null) return name;
		else return ret;
	}
}
