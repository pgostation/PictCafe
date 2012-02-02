import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class PictFile {

	//-------------------------------
	//PICTファイル読み込み
	//-------------------------------
	public static BufferedImage loadPICT(String path){
		File file = new File(path);
		if(!file.exists()) return null;
		
		BufferedInputStream stream;
		try {
			stream = new BufferedInputStream(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		//サイズ取得
		Dimension size = readPICTv1Size(stream);
		int version = readPICTVer(stream);
		if(version==2) size = readPICTv2Size(stream,version);
		if(size==null) return null;
		if(size.width<=0 || size.height<=0){
			System.out.println("PICT size error");
			return null;
		}

		//各データ
		BufferedImage img = null;
		while(true){
			int opcode;
			try {
				opcode = readOpcode2(stream,version);
			} catch (IOException e1) {
				break;
			}
			if(opcode==0x0000){ //アライメントずれの暫定措置
				opcode = readOpcode(stream,1);
			}
			boolean bitmap_flag=false;
			boolean packbits_flag=false;
			boolean fullcolor_flag=false;
			boolean rgnmask_flag=false;
			//System.out.println("opcode:"+opcode);
			if(opcode==0x8200||opcode==0x8201){
				//JPEG
				try {
					img = javax.imageio.ImageIO.read(stream);
				} catch (IOException e) {
					e.printStackTrace();
				}
				continue;
			}
			else if(opcode==0x0090||opcode==0x0091){
				bitmap_flag = true;
			}
			else if(opcode==0x0098||opcode==0x0099){
				packbits_flag = true;
			}
			else if(opcode==0x009A||opcode==0x009B){
				packbits_flag = true;
				fullcolor_flag = true;
			}
			else if(opcode==0x00a1){
				//ロングコメント
				opcode = readOpcode(stream,2);
				int length = readOpcode(stream,2);
				for(int i=0;i<length;i++){readOpcode(stream,1);}
			}
			else if(opcode==0x00ff||opcode==0xffff){
				break; //終了コード
			}
			else if(opcode==0x001e){
				continue; //?
			}
			else if(opcode==0x0001){
				//領域
				int length = readOpcode(stream,2);
				for(int i=0;i<length-2;i++){readOpcode(stream,1);}
				continue;
			}
			else if(opcode==0x001f){
				//OpColor
				readOpcode(stream,2);//r
				readOpcode(stream,2);//g
				readOpcode(stream,2);//b
				continue;
			}
			else if(opcode==0x001e){
				//defHilite
				continue;
			}
			else if(opcode==0x00a0){
				//ショートコメント
				readOpcode(stream,2);
				continue;
			}
			else{
				//不明
				continue;
			}

			if(opcode==0x0091||opcode==0x0099||opcode==0x009B){
				rgnmask_flag = true;
			}
			
			if(img==null) {
				img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = img.createGraphics();
				g.setColor(new Color(255,255,255));
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
				g.fillRect(0,0, size.width, size.height);
			}
			DataBuffer db = img.getRaster().getDataBuffer();

			if(fullcolor_flag){
				readOpcode(stream,2);//ベースアドレス
				readOpcode(stream,2);
			}
			
			int rowBytes = 0x3fff & readOpcode(stream,2);
			
			int btop = readOpcode(stream,2);//左上Ｙ座標
			int bleft = readOpcode(stream,2);
			int bbottom = readOpcode(stream,2);
			int bright = readOpcode(stream,2);
			
			int bpp = 1;
			int[] palette = null;
			
			if(!bitmap_flag){
				readOpcode(stream,2);//バージョン
				readOpcode(stream,2);//圧縮タイプ
				readOpcode(stream,2);//圧縮サイズ
				readOpcode(stream,2);//圧縮サイズ
				readOpcode(stream,2);//水平解像度
				readOpcode(stream,2);//水平解像度
				readOpcode(stream,2);//垂直解像度
				readOpcode(stream,2);//垂直解像度
				readOpcode(stream,2);//ピクセルタイプ
				bpp = readOpcode(stream,2);//１ピクセルあたりのビット数
				/*int byteoff =*/ readOpcode(stream,2);//次のピクセルまでのバイトオフセット
				/*int pixelbytes =*/ readOpcode(stream,2);//コンポーネントサイズ
				readOpcode(stream,2);//次のカラープレーンまでのオフセット
				readOpcode(stream,2);//次のカラープレーンまでのオフセット
				readOpcode(stream,2);//反転
				readOpcode(stream,2);//反転
				readOpcode(stream,2);//カラーテーブル識別番号
				readOpcode(stream,2);//カラーテーブル識別番号
				if(!fullcolor_flag){
					readOpcode(stream,2);//カラーテーブルID
					readOpcode(stream,2);//カラーテーブルID
					readOpcode(stream,2);//カラーテーブルフラグ
					int palette_cnt = 1+readOpcode(stream,2);//登録されているパレット数
					if(palette_cnt > 256) return null;
					palette = new int[palette_cnt];
					for(int i=0; i<palette_cnt; i++){
						/*int pidx =*/ readOpcode(stream,2);//パレット番号
						int cr = readOpcode(stream,2)>>8;//パレット色データR
						int cg = readOpcode(stream,2)>>8;//パレット色データG
						int cb = readOpcode(stream,2)>>8;//パレット色データB
						palette[i] = 0xFF000000+(cr<<16)+(cg<<8)+cb;
					}
				}
			}

			if(rowBytes==0) rowBytes = (bright-bleft)*bpp/8;
			if(rowBytes<8) packbits_flag = false;
			
			int dtop = readOpcode(stream,2);//元解像度での左上Ｙ座標
			int dleft = readOpcode(stream,2);
			int dbottom = readOpcode(stream,2);
			int dright = readOpcode(stream,2);
			
			if(dright>size.width || dbottom>size.height){ //無理矢理対応・・・
				dright -= dleft;
				dleft = 0;
				dbottom -= dtop;
				dtop = 0;
				if(dright>size.width || dbottom>size.height){
					break; //無理
				}
			}
			
			readOpcode(stream,2);//72dpiでの左上Ｙ座標
			readOpcode(stream,2);
			readOpcode(stream,2);
			readOpcode(stream,2);
			
			int trans_mode = readOpcode(stream,2);//転送モード
			if(trans_mode!=0){
				//System.out.println("trans_mode:"+trans_mode);
			}
			
			if(rgnmask_flag){
				//System.out.println("rgnmask_flag:"+rgnmask_flag);
				int len = readOpcode(stream,2);
				readOpcode(stream,2);//top
				readOpcode(stream,2);//left
				readOpcode(stream,2);//bottom
				readOpcode(stream,2);//right
				//for(int i=0; i<len-10;i++){readOpcode(stream,1);}
				
				//リージョンフォーマット
				//
				//領域の輪郭の線のデータが入っている
				//上から見て行って、輪郭に含まれたら1、もう一度含まれたら0
				//ということをやればビットマップデータに出来る。
				//
				//最初のwordで上からの行数を示す (飛ばした行は上の行と同じ)
				//(繰り返し){
				//  次のwordで輪郭部分の開始位置
				//  次のwordで輪郭部分の終了位置
				//}
				//32767でライン終了
				int scanline = 0;
				for(int i=0; i<len-10;){
					int lastscanline = scanline;
					scanline = readOpcode(stream,2);
					i+=2;
					for(int yy=lastscanline+1; yy<scanline; yy++){
						if(yy<size.height){
							for(int xx=0; xx<size.width; xx++){
								db.setElem(yy*size.width+xx, db.getElem((yy-1)*size.width+xx));
							}
						}
					}
					int x=0;
					while(i<len-10){
						int xstart = readOpcode(stream,2);
						i+=2;
						if(xstart==32767) {
							if(scanline<size.height && scanline>0){
								for(; x<size.width; x++){
									db.setElem(scanline*size.width+x, db.getElem((scanline-1)*size.width+x));
								}
							}
							break;
						}
						int xend = readOpcode(stream,2);
						i+=2;
						if(scanline<size.height){
							for(; x<xstart; x++){
								if(scanline>0){
									db.setElem(scanline*size.width+x, db.getElem((scanline-1)*size.width+x));
								}
							}
						}
						if(scanline<size.height){
							for(; x<xend; x++){
								if(scanline==0){
									db.setElem(scanline*size.width+x, 0xFFFFFFFF);
								}else{
									if(db.getElem((scanline-1)*size.width+x)==0x00000000){
										db.setElem(scanline*size.width+x, 0xFFFFFFFF);
									}
								}
							}
						}
					}
				}
			}
			
			if(bitmap_flag&&!packbits_flag){
				//無圧縮BitMap
				for(int v=0; v<bbottom-btop; v++){
					byte[] data = new byte[rowBytes];
					for(int i=0; i<rowBytes; i++){
						try { data[i] = (byte)stream.read(); }
						catch (IOException e) {}
					}
					for(int h=0; h<bright-bleft; h++){
						int pix = (data[h/8]>>(7-(h%8)))&0x01;
						if(pix!=0) {
							if(trans_mode==1) continue; //#srcOr
							else pix=0xFFFFFFFF;
						}
						else if( pix==0 && trans_mode==3 ){ //#srcBic
							pix=0xFFFFFFFF;
						}
						if(rgnmask_flag){
							if(db.getElem(v*size.width+h)==0x00000000){
								continue;
							}
						}
						db.setElem((v/*+btop*/)*size.width+(h/*+bleft*/), pix);
					}
				}
			}
			else if(bitmap_flag&&packbits_flag){
				//圧縮BitMap
				int dlen = 1;
				if(rowBytes>=251) dlen=2;
				for(int v=0; v<bbottom-btop; v++){
					if(v+dtop>=size.height) break;
					
					byte[] data = new byte[rowBytes];
					int offset = 0;
					//packBitsを展開
					int packsize = readOpcode(stream,dlen);
					for(int i=0; i<packsize; i++){
						int dsize = readOpcode(stream,1);
						if(dsize>=128) {
							//同じデータが連続する場合
							dsize = 256-dsize+1;
							int src = readOpcode(stream,1);
							for(int j=0; j<dsize; j++){
								data[j+offset] = (byte)src;
							}
							offset += dsize;
						}
						else {
							//データそのまま
							dsize++;
							for(int j=0; j<dsize; j++){
								try { data[j+offset] = (byte)stream.read(); }
								catch (IOException e) {}
							}
							offset += dsize;
						}
					}
					for(int h=0; h<bright-bleft; h++){
						int pix = (data[h/8]>>(7-(h%8)))&0x01;
						if(pix!=0){
							if(trans_mode==1) continue; //#srcOr
							else pix=0xFFFFFFFF;
						}
						else if( pix==0 && trans_mode==3 ){ //#srcBic
							pix=0xFFFFFFFF;
						}
						if(rgnmask_flag){
							if(db.getElem(v*size.width+h)==0x00000000){
								continue;
							}
						}
						db.setElem((v+dtop)*size.width+(h+dleft), pix);
					}
				}
			}
			else if(!bitmap_flag&&packbits_flag){
				//圧縮PixMap
				int dlen = 1;
				if(rowBytes>=251) dlen=2;
				for(int v=0; v<bbottom-btop; v++){
					byte[] data = new byte[rowBytes];
					int offset = 0;
					//packBitsを展開
					int packsize = readOpcode(stream,dlen);
					for(int i=0; i<packsize; i++){
						int dsize = readOpcode(stream,1);
						if(dsize>=128) {
							//同じデータが連続する場合
							dsize = 256-dsize+1;
							int src = readOpcode(stream,1);
							i++;
							for(int j=0; j<dsize && j+offset<data.length; j++){ data[j+offset] = (byte)src; }
							offset += dsize;
						}
						else {
							//データそのまま
							dsize++;
							for(int j=0; j<dsize; j++){
								if(rowBytes<=j+offset){
									System.out.println("!");
								}
								try { data[j+offset] = (byte)stream.read();i++; }
								catch (IOException e) {}
							}
							offset += dsize;
						}
					}
					if(v+dtop>=size.height) break;
					for(int h=0; h<bright-bleft; h++){
						if(h+dleft>=size.width) break;
						int pix = 0;
						int idx = 0;
						if(bpp==1){
							idx = (data[h/8]>>(8-(h%8+1)))&0x01;
						}
						else if(bpp==2){
							idx = (data[h/4]>>(8-(h%4*2+2)))&0x03;
						}
						else if(bpp==4){
							idx = (data[h/2]>>(8-(h%2*4+4)))&0x07;
						}
						else if(bpp==8){
							idx = (data[h])&0xFF;
						}
						else if(bpp==16){
							int pix16 = (data[h*2]<<8)+data[h*2+1];
							int cr = (pix16>>11)&0x1F;
							int cg = (pix16>> 5)&0x3F;
							int cb = (pix16    )&0x1F;
							cr *= 0xFF/0x1F;
							cg *= 0xFF/0x3F;
							cb *= 0xFF/0x1F;
							pix = 0xFF000000+cr<<16+cg<<8+cb;
						}
						else if(bpp==32){
							pix = 0xFF000000+(data[h*3+1]<<16)+(data[h*3+2]<<8)+data[h*3+3];
						}
						if(fullcolor_flag){
							if(trans_mode==36 && pix==0xFFFFFFFF){
								continue;
							}
							else if( pix==0x00000000 && trans_mode==3 ){ //#srcBic
								pix=0xFFFFFFFF;
							}
						}else if(palette!=null){
							if(idx>=palette.length) idx = 0;
							pix = palette[idx];
							if(trans_mode==36 && pix==0xFFFFFFFF){
								continue;
							}
							else if( pix==0x00000000 && trans_mode==3 ){ //#srcBic
								pix=0xFFFFFFFF;
							}
						}else{
							if(pix!=0) {
								if(trans_mode==1) continue; //#srcOr
								else pix=0xFFFFFFFF;
							}
							else if( pix==0 && trans_mode==3 ){ //#srcBic
								pix=0xFFFFFFFF;
							}
						}
						if(rgnmask_flag){
							if(db.getElem((v+dtop)*size.width+(h+dleft))==0x00000000){
								continue;
							}
						}
						db.setElem((v+dtop)*size.width+(h+dleft), pix);
					}
				}
			}
		}
		
		return img;
	}
	
	//PICTヘッダ読み込み
	private static Dimension readPICTv1Size(BufferedInputStream stream){
		int width=0, height=0;
		try {
			//filler
			for(int i=0; i<512; i++){
				stream.read();
			}
			//v1 filesize
			for(int i=0; i<2; i++){
				stream.read();
			}
			//print size
			readOpcode(stream,2);
			readOpcode(stream,2);
			height = readOpcode(stream,2);
			width = readOpcode(stream,2);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return new Dimension(width,height);
	}
	
	//PICTヘッダ読み込み
	private static int readPICTVer(BufferedInputStream stream){
		int version = 0;

		//バージョン
		int opcode = readOpcode(stream,2);
		if(opcode==0x0011) {//バージョンオプコード2
			opcode = readOpcode(stream,2);
			if(opcode==0x02FF) version = 2;//バージョン2
		}
		else if(opcode==0x1101) version = 1;//バージョン1
		
		return version;
	}

	private static Dimension readPICTv2Size(BufferedInputStream stream, int version){
		int top=0,left=0,bottom=0,right=0;
		
		//バージョン2ヘッダ
		int opcode = readOpcode(stream,version);
		if(opcode==0x0C00);//バージョン2ヘッダーオプコード
		else return null;
		int zahyou = readOpcode(stream,version);//座標位置指定形式
		if(zahyou == 0xfffe){
			readOpcode(stream,version);//予約
			readOpcode(stream,version);//水平解像度
			readOpcode(stream,version);//水平解像度
			readOpcode(stream,version);//垂直解像度
			readOpcode(stream,version);//垂直解像度
			top = readOpcode(stream,version);//左上Ｙ座標
			left = readOpcode(stream,version);//左上Ｘ座標
			bottom = readOpcode(stream,version);//右下Ｙ座標
			right = readOpcode(stream,version);//右下Ｘ座標
			readOpcode(stream,version);//予約（0）
			readOpcode(stream,version);//予約（0）
		}
		else if(zahyou == 0xffff){ //固定小数点座標
			readOpcode(stream,version);//予約(ffff)
			left = readOpcode(stream,version);//左上Ｘ標
			readOpcode(stream,version);//左上Ｘ座標(小数点以下)
			top = readOpcode(stream,version);//左上Ｙ座標
			readOpcode(stream,version);//左上Ｙ座標(小数点以下)
			right = readOpcode(stream,version);//右下Ｘ座標
			readOpcode(stream,version);//右下Ｘ座標(小数点以下)
			bottom = readOpcode(stream,version);//右下Ｙ座標
			readOpcode(stream,version);//右下Ｙ座標(小数点以下)
			readOpcode(stream,version);//予約（0）
			readOpcode(stream,version);//予約（0）
		}
		
		return new Dimension(right-left,bottom-top);
	}

	private static final int readOpcode(BufferedInputStream stream, int version){
		byte[] opcode = new byte[2];
		for(int i=0; i<version; i++){
			try {
				opcode[i] = (byte)stream.read();
			} catch (IOException e) {
			}
		}
		int iop = 0;
		if(version==1) iop = (opcode[0])&0xff;
		else if(version==2) iop = ((opcode[0]&0xff)<<8)+(opcode[1]&0xff);
		//System.out.println(". "+iop);
		return iop;
	}
	
	private static final int readOpcode2(BufferedInputStream stream, int version) throws IOException{
		byte[] opcode = new byte[2];
		for(int i=0; i<version; i++){
			opcode[i] = (byte)stream.read();
		}
		int iop = 0;
		if(version==1) iop = (opcode[0])&0xff;
		else if(version==2) iop = ((opcode[0]&0xff)<<8)+(opcode[1]&0xff);
		return iop;
	}
}
