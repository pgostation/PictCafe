import java.util.HashMap;


public class International {
	static HashMap<String,String> menuHash = new HashMap<String,String>(); //高速化のためのツリー
	static HashMap<String,String> menuRHash = new HashMap<String,String>(); //逆引き
	static HashMap<String,String> dialogHash = new HashMap<String,String>(); //高速化のためのツリー
	static HashMap<String,String> dialogRHash = new HashMap<String,String>(); //逆引き

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
			//メニュー
			putHash(menuHash, menuRHash,"File","ファイル");
			putHash(menuHash, menuRHash,"New Picture from Clipboard","クリップボードから新規作成");
			putHash(menuHash, menuRHash,"Open…","開く…");
			putHash(menuHash, menuRHash,"Close","閉じる");
			putHash(menuHash, menuRHash,"Save as PNG…","PNG形式で保存…");
			putHash(menuHash, menuRHash,"Save as GIF…","GIF形式で保存…");
			putHash(menuHash, menuRHash,"Quit","終了");

			putHash(menuHash, menuRHash,"Edit","編集");
			putHash(menuHash, menuRHash,"Index Color…","減色…");
			putHash(menuHash, menuRHash,"Cut","カット");
			putHash(menuHash, menuRHash,"Copy","コピー");
			putHash(menuHash, menuRHash,"Paste","ペースト");
			putHash(menuHash, menuRHash,"Clear","消去");
			putHash(menuHash, menuRHash,"Select All","すべてを選択");
			putHash(menuHash, menuRHash,"Version Info…","バージョン情報…");
			
			//
			putHash(dialogHash, dialogRHash,"Drop picture files here","画像ファイルをここにドロップ");
			putHash(dialogHash, dialogRHash,"Open Picture File","画像ファイルを開く");
			putHash(dialogHash, dialogRHash,"Save Picture File","画像ファイルを保存");
			putHash(dialogHash, dialogRHash,"Index Color Settings","減色設定");
			
			putHash(dialogHash, dialogRHash,"Colors and Dithering","色数とディザリング");
			putHash(dialogHash, dialogRHash,"Save Format","保存形式");
			putHash(dialogHash, dialogRHash,"Protected Color","保護カラー");
			putHash(dialogHash, dialogRHash,"Auto","自動");
			putHash(dialogHash, dialogRHash,"None","なし");
			
			putHash(dialogHash, dialogRHash,"Cancel","キャンセル");
			putHash(dialogHash, dialogRHash,"Preview","プレビュー");
			putHash(dialogHash, dialogRHash,"Save","保存");
			putHash(dialogHash, dialogRHash,"Save All","すべて保存");

			putHash(dialogHash, dialogRHash,"Converting...","処理中...");
			putHash(dialogHash, dialogRHash," Colors:"," 色数:");
			putHash(dialogHash, dialogRHash,"Choose from Screen","画面の色を取得");

			//エラー
			putHash(dialogHash, dialogRHash,"Could't open the file.","ファイルを開けませんでした");
			putHash(dialogHash, dialogRHash,"Can't save full color Image.","フルカラーの画像です。減色して保存してください。");
			putHash(dialogHash, dialogRHash,"Can't save over 256 colors Image.","256色を超える画像はこのフォーマットで保存できません。");
			putHash(dialogHash, dialogRHash,"Save failure.","保存に失敗しました");
			putHash(dialogHash, dialogRHash,"Image is empty.","画像がありません");
			putHash(dialogHash, dialogRHash,"Memory overflow.","割り当てメモリが足りません");
			putHash(dialogHash, dialogRHash,"Save complete.","処理が完了しました");
			
		}
		else if(lang.equals("English") ){
			//何もしない
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
