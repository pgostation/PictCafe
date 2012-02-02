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
	//PICT�t�@�C���ǂݍ���
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
		
		//�T�C�Y�擾
		Dimension size = readPICTv1Size(stream);
		int version = readPICTVer(stream);
		if(version==2) size = readPICTv2Size(stream,version);
		if(size==null) return null;
		if(size.width<=0 || size.height<=0){
			System.out.println("PICT size error");
			return null;
		}

		//�e�f�[�^
		BufferedImage img = null;
		while(true){
			int opcode;
			try {
				opcode = readOpcode2(stream,version);
			} catch (IOException e1) {
				break;
			}
			if(opcode==0x0000){ //�A���C�����g����̎b��[�u
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
				//�����O�R�����g
				opcode = readOpcode(stream,2);
				int length = readOpcode(stream,2);
				for(int i=0;i<length;i++){readOpcode(stream,1);}
			}
			else if(opcode==0x00ff||opcode==0xffff){
				break; //�I���R�[�h
			}
			else if(opcode==0x001e){
				continue; //?
			}
			else if(opcode==0x0001){
				//�̈�
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
				//�V���[�g�R�����g
				readOpcode(stream,2);
				continue;
			}
			else{
				//�s��
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
				readOpcode(stream,2);//�x�[�X�A�h���X
				readOpcode(stream,2);
			}
			
			int rowBytes = 0x3fff & readOpcode(stream,2);
			
			int btop = readOpcode(stream,2);//����x���W
			int bleft = readOpcode(stream,2);
			int bbottom = readOpcode(stream,2);
			int bright = readOpcode(stream,2);
			
			int bpp = 1;
			int[] palette = null;
			
			if(!bitmap_flag){
				readOpcode(stream,2);//�o�[�W����
				readOpcode(stream,2);//���k�^�C�v
				readOpcode(stream,2);//���k�T�C�Y
				readOpcode(stream,2);//���k�T�C�Y
				readOpcode(stream,2);//�����𑜓x
				readOpcode(stream,2);//�����𑜓x
				readOpcode(stream,2);//�����𑜓x
				readOpcode(stream,2);//�����𑜓x
				readOpcode(stream,2);//�s�N�Z���^�C�v
				bpp = readOpcode(stream,2);//�P�s�N�Z��������̃r�b�g��
				/*int byteoff =*/ readOpcode(stream,2);//���̃s�N�Z���܂ł̃o�C�g�I�t�Z�b�g
				/*int pixelbytes =*/ readOpcode(stream,2);//�R���|�[�l���g�T�C�Y
				readOpcode(stream,2);//���̃J���[�v���[���܂ł̃I�t�Z�b�g
				readOpcode(stream,2);//���̃J���[�v���[���܂ł̃I�t�Z�b�g
				readOpcode(stream,2);//���]
				readOpcode(stream,2);//���]
				readOpcode(stream,2);//�J���[�e�[�u�����ʔԍ�
				readOpcode(stream,2);//�J���[�e�[�u�����ʔԍ�
				if(!fullcolor_flag){
					readOpcode(stream,2);//�J���[�e�[�u��ID
					readOpcode(stream,2);//�J���[�e�[�u��ID
					readOpcode(stream,2);//�J���[�e�[�u���t���O
					int palette_cnt = 1+readOpcode(stream,2);//�o�^����Ă���p���b�g��
					if(palette_cnt > 256) return null;
					palette = new int[palette_cnt];
					for(int i=0; i<palette_cnt; i++){
						/*int pidx =*/ readOpcode(stream,2);//�p���b�g�ԍ�
						int cr = readOpcode(stream,2)>>8;//�p���b�g�F�f�[�^R
						int cg = readOpcode(stream,2)>>8;//�p���b�g�F�f�[�^G
						int cb = readOpcode(stream,2)>>8;//�p���b�g�F�f�[�^B
						palette[i] = 0xFF000000+(cr<<16)+(cg<<8)+cb;
					}
				}
			}

			if(rowBytes==0) rowBytes = (bright-bleft)*bpp/8;
			if(rowBytes<8) packbits_flag = false;
			
			int dtop = readOpcode(stream,2);//���𑜓x�ł̍���x���W
			int dleft = readOpcode(stream,2);
			int dbottom = readOpcode(stream,2);
			int dright = readOpcode(stream,2);
			
			if(dright>size.width || dbottom>size.height){ //������Ή��E�E�E
				dright -= dleft;
				dleft = 0;
				dbottom -= dtop;
				dtop = 0;
				if(dright>size.width || dbottom>size.height){
					break; //����
				}
			}
			
			readOpcode(stream,2);//72dpi�ł̍���x���W
			readOpcode(stream,2);
			readOpcode(stream,2);
			readOpcode(stream,2);
			
			int trans_mode = readOpcode(stream,2);//�]�����[�h
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
				
				//���[�W�����t�H�[�}�b�g
				//
				//�̈�̗֊s�̐��̃f�[�^�������Ă���
				//�ォ�猩�čs���āA�֊s�Ɋ܂܂ꂽ��1�A������x�܂܂ꂽ��0
				//�Ƃ������Ƃ����΃r�b�g�}�b�v�f�[�^�ɏo����B
				//
				//�ŏ���word�ŏォ��̍s�������� (��΂����s�͏�̍s�Ɠ���)
				//(�J��Ԃ�){
				//  ����word�ŗ֊s�����̊J�n�ʒu
				//  ����word�ŗ֊s�����̏I���ʒu
				//}
				//32767�Ń��C���I��
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
				//�����kBitMap
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
				//���kBitMap
				int dlen = 1;
				if(rowBytes>=251) dlen=2;
				for(int v=0; v<bbottom-btop; v++){
					if(v+dtop>=size.height) break;
					
					byte[] data = new byte[rowBytes];
					int offset = 0;
					//packBits��W�J
					int packsize = readOpcode(stream,dlen);
					for(int i=0; i<packsize; i++){
						int dsize = readOpcode(stream,1);
						if(dsize>=128) {
							//�����f�[�^���A������ꍇ
							dsize = 256-dsize+1;
							int src = readOpcode(stream,1);
							for(int j=0; j<dsize; j++){
								data[j+offset] = (byte)src;
							}
							offset += dsize;
						}
						else {
							//�f�[�^���̂܂�
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
				//���kPixMap
				int dlen = 1;
				if(rowBytes>=251) dlen=2;
				for(int v=0; v<bbottom-btop; v++){
					byte[] data = new byte[rowBytes];
					int offset = 0;
					//packBits��W�J
					int packsize = readOpcode(stream,dlen);
					for(int i=0; i<packsize; i++){
						int dsize = readOpcode(stream,1);
						if(dsize>=128) {
							//�����f�[�^���A������ꍇ
							dsize = 256-dsize+1;
							int src = readOpcode(stream,1);
							i++;
							for(int j=0; j<dsize && j+offset<data.length; j++){ data[j+offset] = (byte)src; }
							offset += dsize;
						}
						else {
							//�f�[�^���̂܂�
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
	
	//PICT�w�b�_�ǂݍ���
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
	
	//PICT�w�b�_�ǂݍ���
	private static int readPICTVer(BufferedInputStream stream){
		int version = 0;

		//�o�[�W����
		int opcode = readOpcode(stream,2);
		if(opcode==0x0011) {//�o�[�W�����I�v�R�[�h2
			opcode = readOpcode(stream,2);
			if(opcode==0x02FF) version = 2;//�o�[�W����2
		}
		else if(opcode==0x1101) version = 1;//�o�[�W����1
		
		return version;
	}

	private static Dimension readPICTv2Size(BufferedInputStream stream, int version){
		int top=0,left=0,bottom=0,right=0;
		
		//�o�[�W����2�w�b�_
		int opcode = readOpcode(stream,version);
		if(opcode==0x0C00);//�o�[�W����2�w�b�_�[�I�v�R�[�h
		else return null;
		int zahyou = readOpcode(stream,version);//���W�ʒu�w��`��
		if(zahyou == 0xfffe){
			readOpcode(stream,version);//�\��
			readOpcode(stream,version);//�����𑜓x
			readOpcode(stream,version);//�����𑜓x
			readOpcode(stream,version);//�����𑜓x
			readOpcode(stream,version);//�����𑜓x
			top = readOpcode(stream,version);//����x���W
			left = readOpcode(stream,version);//����w���W
			bottom = readOpcode(stream,version);//�E���x���W
			right = readOpcode(stream,version);//�E���w���W
			readOpcode(stream,version);//�\��i0�j
			readOpcode(stream,version);//�\��i0�j
		}
		else if(zahyou == 0xffff){ //�Œ菬���_���W
			readOpcode(stream,version);//�\��(ffff)
			left = readOpcode(stream,version);//����w�W
			readOpcode(stream,version);//����w���W(�����_�ȉ�)
			top = readOpcode(stream,version);//����x���W
			readOpcode(stream,version);//����x���W(�����_�ȉ�)
			right = readOpcode(stream,version);//�E���w���W
			readOpcode(stream,version);//�E���w���W(�����_�ȉ�)
			bottom = readOpcode(stream,version);//�E���x���W
			readOpcode(stream,version);//�E���x���W(�����_�ȉ�)
			readOpcode(stream,version);//�\��i0�j
			readOpcode(stream,version);//�\��i0�j
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
