import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;


class colorCnt{
	public colorCnt(int redmin, int redmax, int greenmin, int greenmax, int bluemin, int bluemax, int cnt) {
		this.redmin = redmin;
		this.redmax = redmax;
		this.greenmin = greenmin;
		this.greenmax = greenmax;
		this.bluemin = bluemin;
		this.bluemax = bluemax;
		this.count = cnt;
	}
	int redmin; //色空間エリア
	int redmax;
	int greenmin;
	int greenmax;
	int bluemin;
	int bluemax;
	float redavr;
	float greenavr;
	float blueavr;
	float trueredavr;
	float truegreenavr;
	float trueblueavr;
	int reddist;
	int greendist;
	int bluedist;
	int count; //エリア内にあるピクセル数
	colorCnt child; //dietPaletteで統合した子供
	
	int[] initArea = new int[6];
	void setInitArea(colorCnt parentCnt){
		initArea[0] = parentCnt.initArea[0];
		initArea[1] = parentCnt.initArea[1];
		initArea[2] = parentCnt.initArea[2];
		initArea[3] = parentCnt.initArea[3];
		initArea[4] = parentCnt.initArea[4];
		initArea[5] = parentCnt.initArea[5];
	}
	
	
	void setDist(int[] countHist, int[] counts, int dithermode, int colorsize, byte[] gradarea, Color[] protects, Color[] saturations){
		int protectIndex = -1;
		if(protects!=null){
			for(int j=0; j<protects.length; j++){
				//保護カラー
				if(protects[j].getRed()>=this.redmin*4 && protects[j].getRed()<=this.redmax*4 &&
					protects[j].getGreen()>=this.greenmin && protects[j].getGreen()<=this.greenmax &&
					protects[j].getBlue()>=this.bluemin*4 && protects[j].getBlue()<=this.bluemax*4)
				{
					protectIndex = j;
					break;
				}
			}
		}

		int saturationIndex = -1;
		if(protectIndex==-1 && colorsize>2){
			for(int j=0; j<saturations.length; j++){
				//彩度最高のカラー優先
				if(saturations[j]==null) continue;
				if(saturations[j].getRed()>=this.redmin*4 && saturations[j].getRed()<=this.redmax*4 &&
					saturations[j].getGreen()>=this.greenmin && saturations[j].getGreen()<=this.greenmax &&
					saturations[j].getBlue()>=this.bluemin*4 && saturations[j].getBlue()<=this.bluemax*4)
				{
					int max,min,areamax,areamin;
					if(saturations[j].getRed()>saturations[j].getGreen()&&saturations[j].getRed()>saturations[j].getBlue()){
						max = saturations[j].getRed();
						areamax = (int)this.trueredavr*4;
					}else if(saturations[j].getGreen()>saturations[j].getRed()&&saturations[j].getGreen()>saturations[j].getBlue()){
						max = saturations[j].getGreen();
						areamax = (int)this.truegreenavr;
					}else{ 
						max = saturations[j].getBlue();
						areamax = (int)this.trueblueavr*4;
					}
					if(saturations[j].getRed()<saturations[j].getGreen()&&saturations[j].getRed()<saturations[j].getBlue()){
						min = saturations[j].getRed();
						areamin = (int)this.trueredavr*4;
					}else if(saturations[j].getGreen()<saturations[j].getRed()&&saturations[j].getGreen()<saturations[j].getBlue()){
						min = saturations[j].getGreen();
						areamin = (int)this.truegreenavr;
					}else{ 
						min = saturations[j].getBlue();
						areamin = (int)this.trueblueavr*4;
					}
					
					if((areamax-areamin)<(max-min)*1/2){ //エリア内の彩度が1/2以下
						if(colorsize<32 || (dithermode!=-1 && colorsize<64)){
							saturationIndex = j;
							break;
						}
					}
				}
			}
		}
		
		
		int iredmin = -1;
		int iredmax = 0;
		//int average = 0;
		this.redavr = 0;
		float pixtotal = 0;
		for(int i=this.redmin; i<this.redmax; i++){
			//ピクセル数を数える
			countHist[i] = 0;
			for(int g=this.greenmin; g<this.greenmax; g++){
				for(int b=this.bluemin; b<this.bluemax; b++){
					countHist[i] += counts[i*256*64+g*64+b];
				}
			}
			pixtotal += countHist[i];
			this.redavr += i*countHist[i];
			if(countHist[i]>0){
				//ピクセルがあるのでminとmaxに反映
				if(iredmin==-1){
					iredmin = i;
					this.redmin = i;
				}
				iredmax = i+1;
			}
		}
		this.redmax = iredmax;
		this.redavr /= (pixtotal);
		this.trueredavr = this.redavr;
		if(protectIndex>=0){
			this.redavr = (this.redavr+7*max(0,(protects[protectIndex].getRed()>>2)-1))/8;
		}
		if(saturationIndex>=0){
			this.redavr = min(63,(this.redavr+7*(saturations[saturationIndex].getRed()>>2))/8);
		}
		//平均値との乖離の合計を求める
		this.reddist = 0;
		for(int i=this.redmin; i<this.redmax; i++){
			this.reddist += countHist[i]*abs(i-this.redavr)/*/(this.redmax-this.redmin)*/;
		}
		if(this.redmax-this.redmin<=1){
			this.reddist = 0;
		}
		
		int igreenmin = -1;
		int igreenmax = 0;
		this.greenavr = 0;
		pixtotal = 0;
		for(int i=this.greenmin; i<this.greenmax; i++){
			//ピクセル数を数える
			countHist[i] = 0;
			for(int r=this.redmin; r<this.redmax; r++){
				for(int b=this.bluemin; b<this.bluemax; b++){
					countHist[i] += counts[r*256*64+i*64+b];
				}
			}
			pixtotal += countHist[i];
			this.greenavr += i*countHist[i];
			if(countHist[i]>0){
				//ピクセルがあるのでminとmaxに反映
				if(igreenmin==-1){
					igreenmin = i;
					this.greenmin = i;
				}
				igreenmax = i+1;
			}
		}
		this.greenmax = igreenmax;
		this.greenavr /= (pixtotal);
		this.truegreenavr = this.greenavr;
		if(protectIndex>=0){
			this.greenavr = (this.greenavr+7*max(0,protects[protectIndex].getGreen()-1))/8;
		}
		if(saturationIndex>=0){
			this.greenavr = min(255,(this.greenavr+7*saturations[saturationIndex].getGreen())/8);
		}
		//平均値との乖離の合計を求める
		this.greendist = 0;
		for(int i=this.greenmin; i<this.greenmax; i++){
			this.greendist += countHist[i]*abs(i-this.greenavr)/*/(this.greenmax-this.greenmin)*/;
		}
		if(this.greenmax-this.greenmin<=1){
			this.greendist = 0;
		}
		this.greendist/=4;
		
		int ibluemin = -1;
		int ibluemax = 0;
		this.blueavr = 0;
		pixtotal = 0;
		for(int i=this.bluemin; i<this.bluemax; i++){
			//ピクセル数を数える
			countHist[i] = 0;
			for(int r=this.redmin; r<this.redmax; r++){
				for(int g=this.greenmin; g<this.greenmax; g++){
					countHist[i] += counts[r*256*64+g*64+i];
				}
			}
			pixtotal += countHist[i];
			this.blueavr += i*countHist[i];
			if(countHist[i]>0){
				//ピクセルがあるのでminとmaxに反映
				if(ibluemin==-1){
					ibluemin = i;
					this.bluemin = i;
				}
				ibluemax = i+1;
			}
		}
		this.bluemax = ibluemax;
		this.blueavr /= (pixtotal);
		this.trueblueavr = this.blueavr;
		if(protectIndex>=0){
			this.blueavr = (this.blueavr+7*max(0,(protects[protectIndex].getBlue()>>2)-1))/8;
		}
		if(saturationIndex>=0){
			this.blueavr = min(63,(this.blueavr+7*max(0,(saturations[saturationIndex].getBlue()>>2)))/8);
		}
		//平均値との乖離の合計を求める
		this.bluedist = 0;
		for(int i=this.bluemin; i<this.bluemax; i++){
			this.bluedist += countHist[i]*abs(i-this.blueavr)/*/(this.bluemax-this.bluemin)*/;
		}
		if(this.bluemax-this.bluemin<=1){
			this.bluedist = 0;
		}

		//明るさの変化度合いを調べる
		int countBright = 0;
		if(reddist>this.bluedist){
			//両方とも明るい領域の数
			for(int r=((int)this.redavr+this.redmax)/2; r<this.redmax; r++){
				for(int g=((int)this.greenavr+this.greenmax)/2; g<this.greenmax; g++){
					for(int b=this.bluemin; b<this.bluemax; b++){
						countBright += counts[r*256*64+g*64+b];
					}
				}
			}
			//両方とも暗い領域の数
			for(int r=this.redmin; r<((int)this.redavr+this.redmin)/2; r++){
				for(int g=this.greenmin; g<((int)this.greenavr+this.greenmax)/2; g++){
					for(int b=this.bluemin; b<this.bluemax; b++){
						countBright += counts[r*256*64+g*64+b];
					}
				}
			}
		}
		else{
			//両方とも明るい領域の数
			for(int r=this.redmin; r<this.redmax; r++){
				for(int g=((int)this.greenavr+this.greenmax)/2; g<this.greenmax; g++){
					for(int b=((int)this.blueavr+this.bluemax)/2; b<this.bluemax; b++){
						countBright += counts[r*256*64+g*64+b];
					}
				}
			}
			//両方とも暗い領域の数
			for(int r=this.redmin; r<this.redmax; r++){
				for(int g=this.greenmin; g<((int)this.greenavr+this.greenmax)/2; g++){
					for(int b=this.bluemin; b<((int)this.blueavr+this.bluemin)/2; b++){
						countBright += counts[r*256*64+g*64+b];
					}
				}
			}
		}

		//全てのdistが大きければ明るさの違い
		//if(this.redavr<58 && (float)this.reddist/this.greendist/2<1.2f && (float)this.bluedist/this.greendist/2<1.2f){

		int c565 = (((int)this.trueredavr>>1)<<11)+(((int)this.truegreenavr>>2)<<5)+(((int)this.trueblueavr>>1));

		if(countBright>pixtotal*2/16 && (dithermode==-1 && colorsize>64))
		{
			float grad=1.0f;
			//System.out.println("gradarea[c565]:"+gradarea[c565]);
			if(colorsize>64){
				//グラデーションエリアでは明るさを優先
				if(gradarea[c565]>=32) grad = 2.0f;
				else if(gradarea[c565]>=2 || gradarea[c565]<0) grad = 1.6f;
				else if (gradarea[c565]>=1) grad = 1.2f;
				else grad = 1.1f;
			}
			
			//二つの領域がともに高いところにピクセルが集中していれば明るさの違い
			if(this.greenavr/4>1 && this.greendist>this.reddist/2 && this.greendist>this.bluedist/2 ){
				//緑を強化(緑が明るさに影響大)
				this.reddist *= 1.0/grad;
				if(dithermode!=-1 || colorsize<=16) this.greendist *= 1.2;
				else this.greendist *= (1.4f+colorsize/256.0f)*grad*grad;
				this.bluedist *= 1.0/grad;
			}
			else if(this.reddist>this.bluedist){
				//赤を強化
				if(dithermode!=-1 || colorsize<=16) this.reddist *= 1.2;
				else this.reddist *= (1.4f+colorsize/256.0f)*grad*grad;
				this.greendist *= 1.0/grad;
				this.bluedist *= 1.0/grad;
			}
			else{
				//青を強化
				this.reddist *= 1.0/grad;
				this.greendist *= 1.0/grad;
				if(dithermode!=-1 || colorsize<=16) this.bluedist *= 1.2;
				else this.bluedist *= (1.4f+colorsize/256.0f)*grad*grad;
			}
		}
		
		if(countBright<pixtotal*1/32 ){
			if(colorsize<32){
				//色数の少ない場合はむしろ色相を優先
				this.reddist *= 1.8;
				this.greendist *= 1.8;
				this.bluedist *= 1.8;
			}
			else if(this.redavr+this.greenavr/4+this.blueavr >= 185 ||
					this.redavr+this.greenavr/4+this.blueavr <= 10){
				//暗いエリア、明るいエリアでは色相を無視
				this.reddist *= 0.1;
				this.greendist *= 0.3;
				this.bluedist *= 0.1;
			}
		}
		
		if(gradarea[c565]>=2 || gradarea[c565]<0){
			humanicEye();
			humanicEye();
		}
		else{
			humanicEye();
		}
	}
	

	//人間の視覚に合わせる処理
	private void humanicEye(){
		if( this.blueavr+this.greenavr/4+this.redavr<60 && this.blueavr>this.greenavr/4*1.5 && this.blueavr>this.redavr*1.5 ){
			//暗くて青が大
			this.reddist *= 0.8;
			this.greendist *= 0.6;
		}
		
		if( this.redavr>this.blueavr*2 && this.redavr>this.greenavr/4*2*1.2 ){
			//赤が大で他が弱い場合は他の変化を無視
			this.reddist *= 1.0;
			this.greendist *= 0.7;
			this.bluedist *= 0.2;
		}
		else if( this.redavr>this.blueavr*1.6 && this.redavr>this.greenavr/4*1.5 ){
			//赤がやや大で他が弱い場合は他の変化を無視
			this.reddist *= 1.0;
			this.greendist *= 0.9;
			this.bluedist *= 0.5;
		}
		if( this.greenavr/4*1.2>this.blueavr*2 && this.greenavr/4*1.2>this.redavr*2 ){
			//緑が大で他が弱い場合は他の変化を無視
			if(this.greenmin>250) this.greendist *= 0.5;
			this.reddist *= 0.2;
			this.bluedist *= 0.2;
		}
		else if( this.greenavr/4*1.2>this.blueavr*1.4 && this.greenavr/4*1.2>this.redavr*1.6 ){
			//緑がやや大で他が弱い場合は他の変化を無視
			if(this.greenmin>245) this.greendist *= 0.8;
			this.reddist *= 0.4;
			this.bluedist *= 0.4;
		}
		if( this.blueavr>this.redavr*2 && this.blueavr>this.greenavr/4*1.2 ){
			//青が大で他が弱い場合は他の変化を無視、やっぱり青の変化を無視
			this.reddist *= 1;
			this.greendist *= 1;
			this.bluedist *= 0.8;
		}
	
		if( (this.greenmax/4>48 && this.redmax>54) && this.blueavr<32 ){
			//赤and緑が大で青が弱い場合は青の変化を無視
			this.bluedist *= 0.1;
			this.greendist *= 0.8;
			this.reddist *= 0.7;
		}
		else if( (this.redmax>54) && this.blueavr<32 ){
			//赤が大で青が弱い場合は青の変化を無視
			this.bluedist *= 0.3;
			if(this.greenmax/4>60) this.greendist *= 0.6;
			else this.greendist *= 1.0;
			this.reddist *= 0.9;
		}
		else if( (this.greenmax/4>48) && this.blueavr<32 ){
			//緑が大で青が弱い場合は青の変化を無視
			this.bluedist *= 0.3;
			this.greendist *= 0.8;
			this.reddist *= 0.9;
		}
		else if( (this.greenmax/4>36 && this.bluemax>54) && this.redavr<32 ){
			//緑and青が大で赤が弱い場合は赤の変化を無視
			this.reddist *= 0.3;
			this.greendist *= 1.0;
			this.bluedist *= 0.9;
		}
		else if( (this.redmax>48 && this.bluemax>48) && this.greenavr/4<12 ){
			//赤and青が大で緑が弱い場合は緑の変化を無視
			this.greendist *= 0.30;
			if(this.redmax>54) this.bluedist *= 0.50;
		}
		
		if(this.redavr+this.greenavr/4+this.blueavr >= 188){
			//白色エリア
			this.reddist = this.reddist*3/4;
			this.greendist = this.greendist*3/4;
			this.bluedist = this.bluedist*3/4;
		}
		else if( this.redavr>this.greenavr/4+5 && this.greenavr/4>this.blueavr+5 && 
				this.greenavr/4*1.5 >= this.redavr && this.blueavr*2 >= this.greenavr/4){
			//肌色エリア
			//this.reddist *= 1.3;
			this.greendist *= 1.1;
			//this.bluedist *= 1.3;
		}
		else if( (this.redmin>48 && this.redavr<=55 || this.bluemin>48 && this.blueavr<=55)  &&
				(this.greendist>this.reddist) && (this.greendist>this.bluedist) ){
			//緑の変化大
			this.greendist *= 1.5;
		}
		else if( this.greenavr/4>54 && this.greenmin/2>this.bluemax && this.greenmin/4>this.redmax && (this.greenmax/4-this.greenmin/4<=8) ){
			//緑が大で緑が狭い
			this.greendist = this.greendist*3/4;
		}
		else if(this.redmax+this.greenmax/4+this.bluemax <= 32){
			//暗色エリア
			if(this.reddist>this.greendist/2) {
				this.reddist *= 0.6;
				this.greendist *= 0.1;
				this.bluedist *= 0.05;
			}
			else if(this.bluedist>this.greendist/2) {
				this.reddist *= 0.1;
				this.greendist *= 0.05;
				this.bluedist *= 0.6;
			}
			else {
				this.reddist *= 0.1;
				this.greendist *= 0.6;
				this.bluedist *= 0.05;
			}
			if(this.redmax+this.greenmax/4+this.bluemax <= 16){
				//かなり暗色エリアの場合はさらに半分
				this.reddist *= 0.5;
				this.greendist *= 0.5;
				this.bluedist *= 0.5;
			}
		}
		else if(this.redmax+this.greenmax/4+this.bluemax <= 64){
			//やや暗色エリア
			//this.reddist *= 0.9;
			//this.greendist *= 0.9;
			//this.bluedist *= 0.8;
		}
	}
	private static final int max(int a, int b){
		return a>b?a:b;
	}
	/*private static final int min(int a, int b){
		return a<b?a:b;
	}
	private static final float max(float a, float b){
		return a<b?a:b;
	}*/
	private static final float min(float a, float b){
		return a<b?a:b;
	}
	private static final float abs(float a){
		return a>0?a:-a;
	}
}


class IndexColor{
	static BufferedImage makeIndexColor(IndexColorDialog dialog, 
			int colors, Color[] protects, int dithermode)
	{	
		BufferedImage bi = dialog.owner.getImage();
		if(bi==null){
			new AnswerDialog(dialog, International.getDialogText("Could't open the file."), null, "OK", null, null);
			return null;
		}
		
		int width = bi.getWidth();
		int height = bi.getHeight();

		if(Runtime.getRuntime().maxMemory()-(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())
				<5*width*height){
			new AnswerDialog(dialog, International.getDialogText("Memory overflow."), null, "OK", null, null);
			return null;
		}
		BufferedImage srcbi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D srcg = srcbi.createGraphics();
		srcg.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
		srcg.fillRect(0,0,srcbi.getWidth(), srcbi.getHeight());
		srcg = srcbi.createGraphics();
		srcg.drawImage(bi, 0, 0, null);
		
		return makeIndexColor(dialog, srcbi, colors, protects, dithermode);
	}
	
	
	static BufferedImage makeIndexColor(IndexColorDialog dialog, BufferedImage srcbi, 
			int colors, Color[] in_protects, int dithermode)
	{
		dialog.owner.progress(0);
		
		System.gc();
		
		//6+8+6の20bitに変換
		int width = srcbi.getWidth();
		int height = srcbi.getHeight();
		if(Runtime.getRuntime().maxMemory()-(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())
				<5*srcbi.getWidth()*srcbi.getHeight()){
			new AnswerDialog(dialog, International.getDialogText("Memory overflow."), null, "OK", null, null);
			return null;
		}
		BufferedImage minibi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		DataBuffer db = srcbi.getRaster().getDataBuffer();
		DataBuffer minidb = minibi.getRaster().getDataBuffer();
		for(int v=0; v<height; v++){
			for(int h=0; h<width; h++){
				int c = db.getElem(h+v*width);
				int r = (c>>16)&0xFF;
				int g = (c>>8)&0xFF;
				int b = (c>>0)&0xFF;
				minidb.setElem(h+v*width, ((r/4)<<14)+((g)<<6)+((b/4)));
			}
		}

		dialog.owner.progress(3);

		if(Runtime.getRuntime().maxMemory()-(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())
				<(1<<20)*4){

			new AnswerDialog(dialog, International.getDialogText("Memory overflow."), null, "OK", null, null);
			return null;
		}
		
		int[] counts = new int[1<<20];
		for(int i=0; i<counts.length; i++){
			counts[i] = 0;
		}

		byte[] gradarea = new byte[1<<16];//5,6,5でグラデーションのエリアを記憶
		boolean useTrans = false;
		
		if(in_protects!=null){
			for(int i=0; i<in_protects.length; i++){
				int d = (0x3F&(in_protects[i].getRed()>>2)<<16) + (0xFF&(in_protects[i].getGreen())<<8) + (0x3F&(in_protects[i].getBlue())>>2);
				counts[d]+=100;
			}
		}
		
		//含まれる色をカウント
		//DataBuffer db = srcbi.getRaster().getDataBuffer();
		//DataBuffer minidb = minibi.getRaster().getDataBuffer();
		int[] grad_d = new int[8];
		int cnt_renzoku;
		for(int v=0; v<height; v++){
			cnt_renzoku = 0;
			for(int h=0; h<width; h++){
				int orgd = db.getElem(h+v*width);
				if(((orgd>>24)&0xFF)==0) {
					useTrans = true;
					continue;//透明色は含まない
				}
				int d = minidb.getElem(h+v*width);
				grad_d[cnt_renzoku%8] = d;
				counts[d]+=2;
				int r = ((d>>14)&0x3F);
				int g = ((d>>6)&0xFF);
				int b = ((d)&0x3F);
				if(r>=54 && r<g/4+24 && r>g/4+4 &&g/4<b+16 && g/4>b+2){
					//肌色エリア
					counts[d]+=1;
				}
				if(in_protects!=null){
					for(int i=0; i<in_protects.length; i++){
						//保護カラー
						if(isNear(d, in_protects[i], 3)){
							counts[d]+=8;
						}
					}
				}
				if((colors==-1 || colors >= 16) && h-3>=0 && v-3>=0 && /*r+g+b<364 &&*/ r+g+b>24){
					//面かどうかを5ピクセルで判別
					int dd1 = minidb.getElem(h-3+v*width);
					int dd2 = minidb.getElem(h+(v-3)*width);
					int dd3 = minidb.getElem(h-2+(v-2)*width);
					int dd4 = minidb.getElem(h-3+(v-3)*width);
					int cnt = 0;
					if(abs(r - ((dd1>>14)&0x3F))<=3 && abs(r - ((dd2>>14)&0x3F))<=3
							 && abs(r - ((dd3>>14)&0x3F))<=1 && abs(r - ((dd4>>14)&0x3F))<=1){
						cnt++;
					}
					if(abs(g - ((dd1>>6)&0xFF))<=15 && abs(g - ((dd2>>6)&0xFF))<=15
							&& abs(g - ((dd3>>6)&0xFF))<=7 && abs(g - ((dd4>>6)&0xFF))<=7 )
					{
						cnt++;
					}
					if(abs(b - ((dd1)&0x3F))<=3 && abs(b - ((dd2)&0x3F))<=3
							 && abs(b - ((dd3)&0x3F))<=1 && abs(b - ((dd4)&0x3F))<=1){
						cnt++;
					}
					if(cnt==3){
						//counts[d]+=1;
						
						int dxr = ((d>>14)&0x3F)>>1;
						int dxg = ((d>>6)&0xFF)>>2;
						int dxb = ((d)&0x3F)>>1;
						if(gradarea[((dxr)<<11)+((dxg)<<5)+(dxb)]==0){
							//面はgradarea==1として登録
							gradarea[((dxr)<<11)+((dxg)<<5)+(dxb)]=1;
						}
						
						cnt_renzoku++;
						if(cnt_renzoku>=2 && h-cnt_renzoku>=0){
							int dd5 = minidb.getElem(h-cnt_renzoku+v*width);
							//グラデーションの方向が変わっていないか
							int houkou = 0;
							if(((r-((dd1>>14)&0x3F))>0) == ((((dd1>>14)&0x3F)-((dd5>>14)&0x3F))<0)){
								houkou++;
							}
							if(((g-((dd1>>6)&0xFF))>0) == ((((dd1>>6)&0xFF)-((dd5>>6)&0xFF))<0)){
								houkou++;
							}
							if(((b-((dd1)&0x3F))>0) == ((((dd1)&0x3F)-((dd5)&0x3F))<0)){
								houkou++;
							}
							if(houkou>=2){
								cnt_renzoku = 0;
							}
						}
						if(cnt_renzoku>=8){
							//8個以上連続して類似した色が続くとより優先度を高くする
							int add = 1;
							
							int dd5 = minidb.getElem(h-cnt_renzoku+v*width);
							if((r-((dd5>>14)&0x3F))<=3 && (g-((dd5>>6)&0xFF))<=7 && (b-((dd5)&0x3F))<=3){
								//微妙な差の場合はむしろ量子化誤差かもしれない
								//add = -1;
							}
							
							for(int k=1; k<8; k++){ //0は処理しない
								int ddr = ((grad_d[k]>>14)&0x3F)>>1;
								int ddg = ((grad_d[k]>>6)&0xFF)>>2;
								int ddb = ((grad_d[k])&0x3F)>>1;
								//counts[grad_d[k]]+=8;
								if(gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]<127 && gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]>-127){
									gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]+=add;
								}
							}
							cnt_renzoku = 1; //0を処理しない代わりにこれを1にする
						}
					}
					else cnt_renzoku = 0;
				}
			}
		}

		//縦向きに走査してグラデーションを調べる
		if(colors==-1 || colors >= 128){
			for(int h=0; h<width; h+=2){
				cnt_renzoku = 0;
				for(int v=0; v<height; v++){
					int orgd = db.getElem(h+v*width);
					if(((orgd>>24)&0xFF)==0) continue;//透明色は含まない
					int d = minidb.getElem(h+v*width);
					grad_d[cnt_renzoku%8] = d;
					int r = ((d>>14)&0x3F);
					int g = ((d>>6)&0xFF);
					int b = ((d)&0x3F);
					if((colors==-1 || colors >= 32) && v-1>=0 && /*r+g+b<364 &&*/ r+g+b>24){
						//面
						int dd1 = minidb.getElem(h+(v-1)*width);
						int cnt = 0;
						if(abs(r - ((dd1>>14)&0x3F))<=3){
							cnt++;
						}
						if(abs(g - ((dd1>>6)&0xFF))<=15)
						{
							cnt++;
						}
						if(abs(b - ((dd1)&0x3F))<=3){
							cnt++;
						}
						if(cnt==3){
							cnt_renzoku++;
							if(cnt_renzoku>=2 && v-cnt_renzoku>=0){
								int dd3 = minidb.getElem(h+(v-cnt_renzoku)*width);
								//グラデーションの方向が変わっていないか
								int houkou = 0;
								if(((r-((dd1>>14)&0x3F))>0) == ((((dd1>>14)&0x3F)-((dd3>>14)&0x3F))<0)){
									houkou++;
								}
								if(((g-((dd1>>6)&0xFF))>0) == ((((dd1>>6)&0xFF)-((dd3>>6)&0xFF))<0)){
									houkou++;
								}
								if(((b-((dd1)&0x3F))>0) == ((((dd1)&0x3F)-((dd3)&0x3F))<0)){
									houkou++;
								}
								if(houkou>=2){
									cnt_renzoku = 0;
								}
							}
							if(cnt_renzoku>=8){
								//8個以上連続して類似した色が続くとより優先度を高くする
								int add = 1;

								int dd3 = minidb.getElem(h+(v-cnt_renzoku)*width);
								if((r-((dd3>>14)&0x3F))<=3 && (g-((dd3>>6)&0xFF))<=7 && (b-((dd3)&0x3F))<=3){
									//微妙な差の場合はむしろ量子化誤差かもしれない
									//add = -1;
								}
								
								for(int k=1; k<8; k++){
									//counts[grad_d[k]]+=8;
									int ddr = ((grad_d[k]>>14)&0x3F)>>1;
									int ddg = ((grad_d[k]>>6)&0xFF)>>2;
									int ddb = ((grad_d[k])&0x3F)>>1;
									if(gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]>0 && gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]<126){
										gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]+=add;
									}
									else if(gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]==127){
										gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]=-128;
									}
									else if(gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]>-127){
										gradarea[((ddr)<<11)+((ddg)<<5)+(ddb)]--;
									}
								}
								cnt_renzoku = 1;
							}
						}
						else cnt_renzoku = 0;
					}
				}
			}
		}

		//グラデーションエリアをぼかす
		/*for(int r=1; r<31; r++){
			for(int g=1; g<63; g++){
				for(int b=1; b<31; b++){
					gradarea[(r<<11)+(g<<5)+b] = (byte) ((
						gradarea[(r<<11)+(g<<5)+b]+
						gradarea[((r+1)<<11)+(g<<5)+b]+
						gradarea[(r<<11)+((g+1)<<5)+b]+
						gradarea[(r<<11)+(g<<5)+b+1]+2)/4);
				}
			}
		}*/
		
		dialog.owner.progress(8);
		
		boolean autoColors = false;
		if(colors<0){
			colors = 256;
			autoColors = true;
		}
		if(useTrans){
			colors--;
		}
		
		//カラーの配置を分析し、どこかで分ける
		ArrayList<colorCnt> colorList = new ArrayList<colorCnt>();
		int total = 0;
		for(int i=0; i<counts.length; i++){
			total += counts[i];
		}
		if(total>=1000*1000){
			for(int i=0; i<counts.length; i++){
				counts[i] = (counts[i]+9)/10;
			}
			total/=10;
		}

		//彩度を分析
		//ついでに明るさ最高と最低も
		int brightmax_r = 0;
		int brightmax_g = 0;
		int brightmax_b = 0;
		float brightmax = 0;
		int brightmin_r = 0;
		int brightmin_g = 0;
		int brightmin_b = 0;
		float brightmin = 1.0f;
		int[] satmax_r = new int[32];
		int[] satmax_g = new int[32];
		int[] satmax_b = new int[32];
		float[] f = new float[3];
		float[] fb = new float[3];
		for(int r=0; r<64; r++){
			for(int g=0; g<256; g++){
				for(int b=0; b<64; b++){
					if(counts[r*256*64+g*64+b]>total/100000){
						Color.RGBtoHSB(r*255/63, g, b*255/63, f);
						int c = (int)(f[0]*31.99f);
						Color.RGBtoHSB((satmax_r[c]), (satmax_g[c]), (satmax_b[c]), fb);
						if(f[1]*f[2]>=fb[1]*fb[2]){
							//彩度の高いものを採用
							satmax_r[c] = r*4;
							satmax_g[c] = g;
							satmax_b[c] = b*4;
						}
						if(f[2]>brightmax){
							brightmax = f[2];
							brightmax_r = r*4;
							brightmax_g = g;
							brightmax_b = b*4;
						}
						if(f[2]<brightmin){
							brightmin = f[2];
							brightmin_r = r*4;
							brightmin_g = g;
							brightmin_b = b*4;
						}
					}
				}
			}
		}

		float[] sat_r = new float[32*2];
		float[] sat_g = new float[32*2];
		float[] sat_b = new float[32*2];
		float[] sat_p = new float[32*2];
		int[] sat_cnt = new int[32*2];
		for(int r=0; r<64; r++){
			for(int g=0; g<256; g++){
				for(int b=0; b<64; b++){
					if(counts[r*256*64+g*64+b]>0){
						Color.RGBtoHSB(r*255/63, g, b*255/63, f);
						int c = (int)(f[0]*31.99f);
						if(f[2]>0.5f) c+=32;
						Color.RGBtoHSB((int)(sat_r[c]/sat_p[c]), (int)(sat_g[c]/sat_p[c]), (int)(sat_b[c]/sat_p[c]), fb);
						if(f[1]*f[2]>=fb[1]*fb[2]*0.8f){
							//彩度の高いものを採用
							float a = (1.0f/(counts[r*256*64+g*64+b]+1000))*(f[1]*f[2]); //広いエリアは優先しなくても分割される
							sat_r[c] += a*r*4;
							sat_g[c] += a*g;
							sat_b[c] += a*b*4;
							sat_p[c] += a;
							sat_cnt[c] += counts[r*256*64+g*64+b];
						}
					}
				}
			}
		}

		Color[] saturations = new Color[32*2]; //32色相別にそれっぽい彩度の強い色を記憶
		Color[] saturations2 = new Color[32*2]; //32色相別にそれっぽい彩度の強い色を記憶
		{
			//平均より低い彩度の色は採用しない
			float avsat = 0;
			int cnt = 0;
			for(int j=0; j<saturations.length; j++){
				if(sat_p[j]==0) continue;
				saturations[j] = new Color((int)(sat_r[j]/sat_p[j]), (int)(sat_g[j]/sat_p[j]), (int)(sat_b[j]/sat_p[j]));
				Color.RGBtoHSB(saturations[j].getRed(), saturations[j].getGreen(), saturations[j].getBlue(), f);
				avsat += f[1]*f[2];
				cnt++;
			}
			avsat = avsat/cnt;
			
			for(int j=0; j<saturations.length; j++){
				if(saturations[j]==null) continue;
				Color.RGBtoHSB(saturations[j].getRed(), saturations[j].getGreen(), saturations[j].getBlue(), f);
				if(f[1]*f[2]<avsat*0.3f){
					saturations2[j] = saturations[j];
					saturations[j] = null;
				}
			}

			if(colors<=32 && dithermode!=-1){
				
			}
			else if(colors>=32){
				//数の多い色は不要
				int avcnt = 0;
				int scnt = 0;
				for(int j=0; j<saturations.length; j++){
					if(saturations[j]==null) continue;
					avcnt += sat_cnt[j];
					scnt++;
				}
				avcnt /= scnt+1;
				for(int j=0; j<saturations.length; j++){
					if(saturations[j]==null) continue;
					if(sat_cnt[j]>avcnt*1.1){
						saturations2[j] = saturations[j];
						saturations[j] = null;
					}
				}
			}

			int saturationscnt = 0;
			for(int j=0; j<saturations.length; j++){
				if(saturations[j]==null) continue;
				saturationscnt++;
			}
			while(saturationscnt>=colors){
				//数は分割数よりも少なくする
				int mincnt = 0;
				for(int j=0; j<saturations.length; j++){
					if(saturations[j]==null) continue;
					mincnt = sat_cnt[j];
				}
				for(int j=0; j<saturations.length; j++){
					if(saturations[j]==null) continue;
					if(sat_cnt[j]==mincnt){
						saturations[j] = null;
						saturationscnt--;
					}
				}
			}

			//類似色相をまとめる。
			for(int j=0; j<saturations.length; j++){
				if(saturations[j]==null) continue;
				int c = (j+1)%saturations.length;
				if(c==4) continue;//肌色
				//if(c==1||c==6||c==8||c==9||c==10||c==12|c==13||c==15||c==17||c==19||c==21||c==23||c==25||c==27||c==28||c==30||c==31||c==32){
					//c--;
					if(saturations[c]==null) continue;
					Color.RGBtoHSB(saturations[c].getRed(), saturations[c].getGreen(), saturations[c].getBlue(), f);
					Color.RGBtoHSB(saturations[j].getRed(), saturations[j].getGreen(), saturations[j].getBlue(), fb);
					if(abs(f[0]-fb[0])<10 && abs(f[1]-fb[1])<0.3f && abs(f[2]-fb[2])<0.2f){
						if(sat_cnt[c]<sat_cnt[j]){
							saturations2[c] = saturations[c];
							saturations[c] = null;
						}
						else{
							saturations2[j] = saturations[j];
							saturations[j] = null;
							j++;
						}
					}
				//}
			}
		}
		
		//ディザ使用で色数が少ないときは彩度の高い色は保護カラーにする
		Color[] protects = null;
		if(colors<=32 && dithermode!=-1){
			int cnt = 2;
			if(in_protects!=null) cnt+=in_protects.length;
			for(int j=0; j<saturations.length; j++){
				if(saturations[j]!=null) cnt++;
			}
			if(cnt>0){
				protects = new Color[cnt];
				int cn = 0;
				if(in_protects!=null){
					for(int j=0; j<in_protects.length; j++){
						protects[cn] = in_protects[j];
						cn++;
					}
				}
				//最も明るい色
				protects[cn] = new Color(brightmax_r, brightmax_g, brightmax_b);
				cn++;
				//最も暗い色
				protects[cn] = new Color(brightmin_r, brightmin_g, brightmin_b);
				cn++;
				for(int j=0; j<saturations.length; j++){
					if(saturations[j]!=null){
						protects[cn] = saturations[j];
						saturations[j] = null;
						cn++;
					}
				}
			}
		}
		
		dialog.owner.progress(12);
		
		int[] countHist = new int[256];
		
		colorList.add(new colorCnt(0,64,0,256,0,64,total));
		colorList.get(0).initArea[0] = 0;
		colorList.get(0).initArea[1] = 64;
		colorList.get(0).initArea[2] = 0;
		colorList.get(0).initArea[3] = 256;
		colorList.get(0).initArea[4] = 0;
		colorList.get(0).initArea[5] = 64;
		//最初のエリアの範囲を締める
		colorList.get(0).setDist(countHist, counts, dithermode, colorList.size(), gradarea, protects, saturations);
		
		boolean dietedPalette = false;
		
		while(colorList.size()<colors){
			dialog.owner.progress(12+50*colorList.size()/colors);
			
			//色空間をメディアンカット法で切り分けて行く
			
			//分割対象のエリアを決める。範囲が広くピクセルの多いエリア。
			float maxdist = 0;
			colorCnt maxarea = null;
			for(int i=0; i<colorList.size(); i++){
				colorCnt colarea = colorList.get(i);
				float dist = ((float)colarea.reddist+1)+
							((float)colarea.greendist+1)+
								((float)colarea.bluedist+1);
				//float dist = max(((float)colarea.reddist+1)/**(colarea.redmax-colarea.redmin)*//**(colarea.redmax-colarea.redmin)*/,
				//		max(((float)colarea.greendist+1)/**(colarea.greenmax-colarea.greenmin)/2*//**(colarea.greenmax-colarea.greenmin)/2*/,
				//				((float)colarea.bluedist+1)/**(colarea.bluemax-colarea.bluemin)*//**(colarea.bluemax-colarea.bluemin)*/));
				
				if(protects!=null && colarea.count>=0){
					int protectNum=0;
					//for(int j=0; j<protects.length; j++){
					for(int jj=colorList.size()-1; jj<colorList.size()-1+protects.length; jj++){
						int j = (jj)%protects.length;
						//保護カラー
						if(protects[j].getRed()>=colarea.redmin*4 && protects[j].getRed()<=colarea.redmax*4 &&
							protects[j].getGreen()>=colarea.greenmin && protects[j].getGreen()<=colarea.greenmax &&
							protects[j].getBlue()>=colarea.bluemin*4 && protects[j].getBlue()<=colarea.bluemax*4)
						{
							protectNum++;
							dist *= 1.2f;
						}
					}
					if(protectNum>=2){
						//保護カラーが2色以上含まれるエリアを分割
						//maxarea = colarea;
						//break;
						dist *= 1.2f;
					}
				}
				
				//for(int j=0; j<saturations.length; j++){
				Color[] sats;
				if(colorList.size()<128) sats = saturations;
				else sats = saturations2;
				for(int jj=colorList.size(); jj<colorList.size()+sats.length; jj++){
					int j = (jj)%sats.length;
					//彩度最高のカラー優先
					if(colarea.count<0) continue;
					if(sats[j]==null) continue;
					if(sats[j].getRed()>=colarea.redmin*4 && sats[j].getRed()<=colarea.redmax*4 &&
						sats[j].getGreen()>=colarea.greenmin && sats[j].getGreen()<=colarea.greenmax &&
						sats[j].getBlue()>=colarea.bluemin*4 && sats[j].getBlue()<=colarea.bluemax*4)
					{
						int max,min,areamax,areamin;
						if(sats[j].getRed()>sats[j].getGreen()&&sats[j].getRed()>sats[j].getBlue()){
							max = sats[j].getRed();
							areamax = (int)colarea.trueredavr*4;
						}else if(sats[j].getGreen()>sats[j].getRed()&&sats[j].getGreen()>sats[j].getBlue()){
							max = sats[j].getGreen();
							areamax = (int)colarea.truegreenavr;
						}else{ 
							max = sats[j].getBlue();
							areamax = (int)colarea.trueblueavr*4;
						}
						if(sats[j].getRed()<sats[j].getGreen()&&sats[j].getRed()<sats[j].getBlue()){
							min = sats[j].getRed();
							areamin = (int)colarea.trueredavr*4;
						}else if(sats[j].getGreen()<sats[j].getRed()&&sats[j].getGreen()<sats[j].getBlue()){
							min = sats[j].getGreen();
							areamin = (int)colarea.truegreenavr;
						}else{ 
							min = sats[j].getBlue();
							areamin = (int)colarea.trueblueavr*4;
						}
						
						if((areamax-areamin)<(max-min)*2/3){ //エリア内の彩度が2/3以下
							if(i%2==0 && (dithermode!=-1 && colorList.size()<16)){
								dist *= 5.5f;
								//System.out.println("saturations["+j+"] by dist15="+dist);
								//break;
							}
							else if(i%2==1 &&(colorList.size()<16 || (dithermode!=-1 && colorList.size()<32))){
								dist *= 3.5f;
								//System.out.println("saturations["+j+"] by dist5="+dist);
								break;
							}else{
								dist *= 2.5f;
							}
						}
					}
				}
				
				if(colorList.size()>4){
					//グラデーション部分、面部分を優先
					boolean isSurface = false;
					boolean isMainGrad = false;
					boolean isSubGrad = false;
					for(int r=colarea.redmin>>1; r<=(colarea.redmax-1)>>1; r++){
						for(int g=colarea.greenmin>>2; g<=(colarea.greenmax-1)>>2; g++){
							for(int b=colarea.bluemin>>1; b<=(colarea.bluemax-1)>>1; b++){
								int grad = gradarea[((r)<<11)+((g)<<5)+(b)];
								if(grad==0){}
								else if(grad==1){
									isSurface = true;
								}
								else if(grad>32){
									isMainGrad = true;
									break;
								}
								else if(grad>1 || grad<0){
									isSubGrad = true;
								}
							}
							if(isMainGrad) break;
						}
						if(isMainGrad) break;
					}
					if(colorList.size()<colors/4 || colorList.size()<8){
						//始めは面を優先
						if(isSurface && !isSubGrad) dist *= 2f;
						else if(isMainGrad) dist *= 1.5f;
						else if(isSubGrad) dist *= 1.2f;
						else dist *= 0.2f;
					}
					else if(colorList.size()<colors/2){
						//次にグラデーションを優先
						if(isSurface) dist *= 3f;
						else if(isMainGrad) dist *= 2f;
						else if(isSubGrad) dist *= 3f;
					}
					else if(!dietedPalette){
						//次に全グラデーションを優先
						if(isSubGrad) dist *= 3f;
						else if(isMainGrad || isSurface) dist *= 2f;
						else dist *= 0.02f;
					}
					else{
						//仕上げにメインパレット最優先
						if(isMainGrad) dist *= 10f;
						//if(isSubGrad) dist *= 20f;
						//else dist *= 0.02f;
					}
				}
				
				dist = (dist/10000f)*(dist/10000f)*(colarea.count/1f+(float)total/colorList.size()/10f);

				if(dist>=maxdist){
					maxdist = dist;
					maxarea = colarea;
				}
			}
			
			if(maxarea==null){
				maxarea = colorList.get(0);
			}

			/*System.out.println(" maxdist:"+maxdist);
			System.out.println("select area!  count:"+maxarea.count);
			System.out.println("redmin:"+maxarea.redmin+" redmax:"+maxarea.redmax
					+" greenmin:"+maxarea.greenmin+" greenmax:"+maxarea.greenmax
					+" bluemin:"+maxarea.bluemin+" bluemax:"+maxarea.bluemax);
			System.out.println("reddist:"+maxarea.reddist
					+" greendist:"+maxarea.greendist
					+" bluedist:"+maxarea.bluedist);*/
			
			maxarea.setDist(countHist, counts, dithermode, colorList.size(), gradarea, protects, saturations);

			//分割方向(r,g,b)を決める ピクセルがその色方向に偏っていてピクセルを多く分割できる位置
			if(maxarea.reddist==0 && maxarea.greendist==0 && maxarea.bluedist==0){
				//分割不可能
				if(maxarea.count==-10000){
					break;
				}
				maxarea.count = -10000;
				continue;
			}
			
			int colsize = colorList.size();
			if(useTrans) colsize++;
			if(autoColors && (colsize==2 || colsize==4 || colsize==8 || colsize==16 || colsize==32 || colsize==64 || colsize==128)){
				if(maxarea.reddist+ maxarea.greendist+maxarea.bluedist<=total/500){
					break;
				}
			}
			
			colorCnt newarea;
			newarea = new colorCnt(maxarea.redmin, maxarea.redmax, maxarea.greenmin, maxarea.greenmax, maxarea.bluemin, maxarea.bluemax, 0);

			if(maxarea.reddist>=maxarea.greendist && maxarea.reddist>=maxarea.bluedist){
				//赤を分割
				
				//分割に都合が良い場所を求める。
				
				//ヒストグラムを求める
				//float average=0;
				int counttotal=0;
				for(int r=maxarea.redmin; r<maxarea.redmax; r++){
					countHist[r] = 0;
					for(int g=newarea.greenmin; g<newarea.greenmax; g++){
						for(int b=newarea.bluemin; b<newarea.bluemax; b++){
							countHist[r] += counts[r*256*64+g*64+b];
						}
					}
					//average += r*countHist[r];
					counttotal += countHist[r];
				}
				//average/=counttotal;
				int areawidth = (maxarea.redmax-maxarea.redmin)*4;
				if(areawidth>32){
					//ヒストグラムをぼかす
					for(int j=0; j<areawidth/32; j++){
						for(int i=newarea.redmin+1; i<newarea.redmax-1; i++){
							countHist[i] = (countHist[i-1]+2*countHist[i]+countHist[i+1])/4;
						}
					}
				}
				//System.out.println("average"+average+"    counttotal:"+counttotal);
				
				//平均値に近くてピクセルの少ないところを探す
				float maxrank = -1000000;
				for(int i=maxarea.redmin+1; i<maxarea.redmax; i++){
					float rank = (((float)counttotal)/(countHist[i]+1*counttotal/areawidth)-(6+48.0f/areawidth)*abs(maxarea.redavr-i));
					//System.out.println("rank"+rank+"    "+countHist[i]);
					if(rank>=maxrank){
						int splitcount = 0;
						for(int j=maxarea.redmin; j<=i; j++){
							splitcount += countHist[j];
						}
						if(splitcount>maxarea.count*2/8 && splitcount<maxarea.count*6/8){
							//分割
							maxrank = rank;
							newarea.redmin = i;
						}
					}
				}
				if(maxrank == -1000000) newarea.redmin = max((int)maxarea.redavr+1,maxarea.redmin+1);
				maxarea.redmax = newarea.redmin;
				
				newarea.setInitArea(maxarea); //初期の色空間を記憶
				newarea.initArea[0] = newarea.redmin;
				maxarea.initArea[1] = maxarea.redmax;
			}
			else if(maxarea.greendist>=maxarea.reddist && maxarea.greendist>=maxarea.bluedist){
				//緑を分割
				
				//分割に都合が良い場所を求める。
				
				//ヒストグラムを求める
				//float average=0;
				int counttotal=0;
				for(int g=maxarea.greenmin; g<maxarea.greenmax; g++){
					countHist[g] = 0;
					for(int r=newarea.redmin; r<newarea.redmax; r++){
						for(int b=newarea.bluemin; b<newarea.bluemax; b++){
							countHist[g] += counts[r*256*64+g*64+b];
						}
					}
					//average += g*countHist[g];
					counttotal += countHist[g];
				}
				//average/=counttotal;
				int areawidth = (maxarea.greenmax-maxarea.greenmin)*1;
				if(areawidth>32){
					//ヒストグラムをぼかす
					for(int j=0; j<areawidth/32/4; j++){
						for(int i=newarea.greenmin+1; i<newarea.greenmax-1; i++){
							countHist[i] = (countHist[i-1]+2*countHist[i]+countHist[i+1])/4;
						}
					}
				}
				//System.out.println("average"+average+"    counttotal:"+counttotal);
				
				//平均値に近くてピクセルの少ないところを探す
				float maxrank = -1000000;
				for(int i=maxarea.greenmin+1; i<maxarea.greenmax; i++){
					float rank = (((float)counttotal)/(countHist[i]+1*counttotal/areawidth)-(6+48.0f/areawidth)*abs(maxarea.greenavr-i));
					//System.out.println("rank"+rank+"    "+countHist[i]);
					if(rank>=maxrank){
						int splitcount = 0;
						for(int j=maxarea.greenmin; j<=i; j++){
							splitcount += countHist[j];
						}
						if(splitcount>maxarea.count*2/8 && splitcount<maxarea.count*6/8){
							//分割
							maxrank = rank;
							newarea.greenmin = i;
						}
					}
				}
				if(maxrank == -1000000) newarea.greenmin = (int)max((int)maxarea.greenavr+1,maxarea.greenmin+1);
				maxarea.greenmax = newarea.greenmin;
				
				newarea.setInitArea(maxarea); //初期の色空間を記憶
				newarea.initArea[2] = newarea.greenmin;
				maxarea.initArea[3] = maxarea.greenmax;
			}
			else{
				//青を分割
				
				//分割に都合が良い場所を求める。
				
				//ヒストグラムを求める
				//float average=0;
				int counttotal=0;
				for(int b=maxarea.bluemin; b<maxarea.bluemax; b++){
					countHist[b] = 0;
					for(int r=newarea.redmin; r<newarea.redmax; r++){
						for(int g=newarea.greenmin; g<newarea.greenmax; g++){
							countHist[b] += counts[r*256*64+g*64+b];
						}
					}
					//average += b*countHist[b];
					counttotal += countHist[b];
				}
				//average/=counttotal;
				int areawidth = (maxarea.bluemax-maxarea.bluemin)*4;
				if(areawidth>32){
					//ヒストグラムをぼかす
					for(int j=0; j<areawidth/32; j++){
						if(j%2==0){
							for(int i=newarea.greenmin+1; i<newarea.greenmax-1; i++){
								countHist[i] = (countHist[i-1]+2*countHist[i]+countHist[i+1])/4;
							}
						}else{
							for(int i=newarea.greenmax-2; i>=newarea.greenmin+1; i--){
								countHist[i] = (countHist[i-1]+2*countHist[i]+countHist[i+1])/4;
							}
						}
					}
				}
				//System.out.println("average"+average+"    counttotal:"+counttotal);

				//平均値に近くてピクセルの少ないところを探す
				float maxrank = -1000000;
				for(int i=maxarea.bluemin+1; i<maxarea.bluemax; i++){
					float rank = (((float)counttotal)/(countHist[i]+1*counttotal/areawidth)-(6+48.0f/areawidth)*abs(maxarea.blueavr-i));
					//System.out.println("rank"+rank+"    "+countHist[i]);
					if(rank>=maxrank){
						int splitcount = 0;
						for(int j=maxarea.bluemin; j<=i; j++){
							splitcount += countHist[j];
						}
						if(splitcount>maxarea.count*2/8 && splitcount<maxarea.count*6/8){
							//分割
							maxrank = rank;
							newarea.bluemin = i;
						}
					}
				}
				if(maxrank == -1000000) newarea.bluemin = (int)max((int)maxarea.blueavr+1,maxarea.bluemin+1);
				maxarea.bluemax = newarea.bluemin;
				
				newarea.setInitArea(maxarea); //初期の色空間を記憶
				newarea.initArea[4] = newarea.bluemin;
				maxarea.initArea[5] = maxarea.bluemax;
			}
			
			
			//新しい領域のピクセル数を数える
			int count = 0;
			for(int r=newarea.redmin; r<newarea.redmax; r++){
				for(int g=newarea.greenmin; g<newarea.greenmax; g++){
					for(int b=newarea.bluemin; b<newarea.bluemax; b++){
						count += counts[r*256*64+g*64+b];
					}
				}
			}
			newarea.count = count;
			
			maxarea.count = 0;
			for(int r=maxarea.redmin; r<maxarea.redmax; r++){
				for(int g=maxarea.greenmin; g<maxarea.greenmax; g++){
					for(int b=maxarea.bluemin; b<maxarea.bluemax; b++){
						maxarea.count += counts[r*256*64+g*64+b];
					}
				}
			}
			if(maxarea.count==0) {
				//System.out.println("maxarea null:"+maxarea.count);
			}
			
			//範囲を締める
			newarea.setDist(countHist, counts, dithermode, colorList.size(), gradarea, protects, saturations);
			maxarea.setDist(countHist, counts, dithermode, colorList.size(), gradarea, protects, saturations);
			
			//彩度が大きいエリアが出来たら彩度優先カラーを消去
			{
				float[] f1 = Color.RGBtoHSB((int)(newarea.trueredavr)*4, (int)(newarea.truegreenavr),(int)(newarea.trueblueavr)*4, null);
				
				for(int j=0; j<saturations.length; j++){
					if(saturations[j]!=null){
						float[] f2 = Color.RGBtoHSB(saturations[j].getRed(), saturations[j].getGreen(), saturations[j].getBlue(), null);
						if(abs(f1[0]-f2[0])>30) continue;
						
						int max,min,areamax,areamin;
						if(saturations[j].getRed()>saturations[j].getGreen()&&saturations[j].getRed()>saturations[j].getBlue()){
							max = saturations[j].getRed();
							areamax = (int)newarea.trueredavr*4;
						}else if(saturations[j].getGreen()>saturations[j].getRed()&&saturations[j].getGreen()>saturations[j].getBlue()){
							max = saturations[j].getGreen();
							areamax = (int)newarea.truegreenavr;
						}else{ 
							max = saturations[j].getBlue();
							areamax = (int)newarea.trueblueavr*4;
						}
						if(saturations[j].getRed()<saturations[j].getGreen()&&saturations[j].getRed()<saturations[j].getBlue()){
							min = saturations[j].getRed();
							areamin = (int)newarea.trueredavr*4;
						}else if(saturations[j].getGreen()<saturations[j].getRed()&&saturations[j].getGreen()<saturations[j].getBlue()){
							min = saturations[j].getGreen();
							areamin = (int)newarea.truegreenavr;
						}else{ 
							min = saturations[j].getBlue();
							areamin = (int)newarea.trueblueavr*4;
						}
						if(areamax-areamin>(max-min)*2/3){
							saturations[j] = null;
							//締め直し
							newarea.setDist(countHist, counts, dithermode, colorList.size(), gradarea, protects, saturations);
							maxarea.setDist(countHist, counts, dithermode, colorList.size(), gradarea, protects, saturations);
						}
					}
				}
			}
			
			//リストに追加
			colorList.add(newarea);

			if(colorList.size()==colors && !dietedPalette){
				//パレットのグラデーション部分を統合
				dietedPalette = true;
				dietPalette(colorList, gradarea);

				
				//ところで関係ないけどカウントが0のパレットがもしあれば不要
				for(int i=0; i<colorList.size(); i++){
					if(colorList.get(i).count==0){
						//System.out.println("colorList.remove(colorList.get(i));"+i);
						colorList.remove(colorList.get(i));
						i--;
					}
				}
			}
			
			/*{
			 	//アニメーション表示
				BufferedImage tmpbi = colorSelect(srcbi, colors, dithermode, colorList, counts, in_protects, dialog, minibi, gradarea);
				dialog.owner.mainPane.getGraphics().drawImage(tmpbi,0,0,null);
				tmpbi.flush();
				tmpbi = null;
			}*/
			
			/*System.out.println("newarea.count:"+newarea.count);
			System.out.println(" redmin:"+newarea.redmin+" redmax:"+newarea.redmax
					+" greenmin:"+newarea.greenmin+" greenmax:"+newarea.greenmax
					+" bluemin:"+newarea.bluemin+" bluemax:"+newarea.bluemax);
			System.out.println(" reddist:"+newarea.reddist
					+" greendist:"+newarea.greendist
					+" bluedist:"+newarea.bluedist);

			System.out.println("maxarea.count:"+maxarea.count);
			System.out.println(" redmin:"+maxarea.redmin+" redmax:"+maxarea.redmax
					+" greenmin:"+maxarea.greenmin+" greenmax:"+maxarea.greenmax
					+" bluemin:"+maxarea.bluemin+" bluemax:"+maxarea.bluemax);
			System.out.println(" reddist:"+maxarea.reddist
					+" greendist:"+maxarea.greendist
					+" bluedist:"+maxarea.bluedist);*/
		}
		
		return colorSelect(srcbi, colors, dithermode, colorList, counts, in_protects, dialog, minibi, gradarea);
	}
	
	
	//グラデーションで使用するパレットで似たものがあれば統合してパレット縮小
	static void dietPalette(ArrayList<colorCnt> colorList, byte[] gradarea){
		
		colorCnt[] palettes = new colorCnt[colorList.size()];
		for(int hue=0; hue<32+1; hue++){
			//System.out.println("hue:"+hue);
			//色相の似たパレットを抽出
			float[] cover = new float[]{
					1.0f,0.9f,0.6f,0.5f,
					0.5f,0.6f,0.6f,0.8f,
					1.0f,1.1f,1.1f,1.1f,
					1.0f,0.9f,0.8f,0.8f,
					0.7f,0.6f,0.6f,0.6f,
					0.6f,0.7f,0.7f,0.7f,
					0.6f,0.6f,0.6f,0.7f,
					0.8f,0.8f,0.9f,0.9f,
					32f};
			float[] f = new float[4];
			float[] hues = new float[colorList.size()];
			float[] sats = new float[colorList.size()];
			float[] bris = new float[colorList.size()];
			int plt_i = 0;
			for(int i=0; i<colorList.size(); i++){
				RGBtoHSBY((int)colorList.get(i).trueredavr*4, (int)colorList.get(i).truegreenavr, (int)colorList.get(i).trueblueavr*4, f);
				if((f[0]*32+cover[hue])%32>=hue && f[0]*32-cover[hue]<=hue){ //+-cover[hue]の範囲
					if(hue!=32 || (f[1]*f[2])<0.1f){ //hue==32のときは彩度の低いエリア
						palettes[plt_i] = colorList.get(i);
						hues[plt_i] = f[0];
						sats[plt_i] = f[1];
						bris[plt_i] = f[2];
						plt_i++;
					}
				}
			}
			//System.out.println("plt_i:"+plt_i);
			if(plt_i<=1) continue;
			//色相方向の平均値と平均距離を求める
			float hue_avr = 0;
			float hue_dist = 0;
			for(int i=0; i<plt_i; i++){
				hue_avr += hues[i]/plt_i;
			}
			for(int i=0; i<plt_i; i++){
				hue_dist += abs(hues[i]-hue_avr)/plt_i;
			}
			//System.out.println("hue_avr:"+hue_avr);
			//System.out.println("hue_dist:"+hue_dist);
			//平均距離を大幅に越えたものは除外
			int plt_cnt = plt_i;
			if(hue!=32){
				for(int i=0; i<plt_i; i++){
					if(abs(hues[i]-hue_avr) > hue_dist*2){
						//System.out.println("abs(hues[i]-hue_avr):"+abs(hues[i]-hue_avr));
						//System.out.println("除外:"+i);
						palettes[i] = null;
						plt_cnt--;
					}
				}
				//改めて平均と距離を求める
				hue_avr = 0;
				for(int i=0; i<plt_i; i++){
					if(palettes[i] != null){
						hue_avr += hues[i]/plt_cnt;
					}
				}
				hue_dist = 0;
				for(int i=0; i<plt_i; i++){
					if(palettes[i] != null){
						hue_dist += abs(hues[i]-hue_avr)/plt_cnt;
					}
				}
			}
			//System.out.println("hue_avr2:"+hue_avr);
			//System.out.println("hue_dist2:"+hue_dist);
			//距離が離れすぎている場合は次の色相へ
			if(hue_dist*32>cover[hue]){
				continue;
			}
			//範囲内の数が多すぎる場合はこういう画像なので統合しない
			if(plt_cnt>=colorList.size()/2){
				continue;
			}
			/*//平均を中心に2分割して総カウント数を求める
			int[] area_count = new int[]{0,0};
			for(int i=0; i<plt_i; i++){
				if(palettes[i] == null) continue;
				int area;
				if(hues[i]<hue_avr) area=0; else area=1;
				area_count[area] += palettes[i].count;
			}
			//2分割した中で明度と彩度の一致する範囲を決定
			float[] sat_min = new float[]{1.0f,1.0f};
			float[] sat_max = new float[]{0.0f,0.0f};
			float[] bri_min = new float[]{1.0f,1.0f};
			float[] bri_max = new float[]{0.0f,0.0f};
			for(int i=0; i<plt_i; i++){
				if(palettes[i] == null) continue;
				int area;
				if(hues[i]<hue_avr) area=0; else area=1;
				if(sats[i]<sat_min[area]){
					sat_min[area] = sats[i];
				}
				if(sats[i]>sat_max[area]){
					sat_max[area] = sats[i];
				}
				if(bris[i]<bri_min[area]){
					bri_min[area] = bris[i];
				}
				if(bris[i]>bri_max[area]){
					bri_max[area] = bris[i];
				}
			}
			float sat_union_min = max(sat_min[0], sat_min[1]);
			float sat_union_max = min(sat_max[0], sat_max[1]);
			float bri_union_min = max(bri_min[0], bri_min[1]);
			float bri_union_max = min(bri_max[0], bri_max[1]);
			//System.out.println("sat_union_min:"+sat_union_min);
			//System.out.println("sat_union_max:"+sat_union_max);
			//System.out.println("bri_union_min:"+bri_union_min);
			//System.out.println("bri_union_max:"+bri_union_max);
			//共通範囲がなければ次の色相へ
			if(sat_union_min>=sat_union_max) continue;
			if(bri_union_min>=bri_union_max) continue;*/
			//その範囲のパレットを統合。親はcountの大きいほう。
			while(true){
				//最も近いカラーのペアを探す
				float near_dist = 0.15f;//動的に変更したい
				int near_i = -1;
				int near_j = -1;
				for(int i=0; i<plt_i-1; i++){
					if(palettes[i] == null) continue;
					for(int j=i+1; j<plt_i; j++){
						if(palettes[j] == null) continue;
						/*if(sats[i]<sat_union_min-0.05f || sats[i]>sat_union_max+0.05f) continue;
						if(sats[j]<sat_union_min-0.05f || sats[j]>sat_union_max+0.05f) continue;
						if(bris[i]<bri_union_min-0.05f || bris[i]>bri_union_max+0.05f) continue;
						if(bris[j]<bri_union_min-0.05f || bris[j]>bri_union_max+0.05f) continue;*/
						if(hues[i]<hue_avr == hues[j]<hue_avr) continue;

						//int area_i;
						float alpha = 1.0f;
						//if(hues[i]<hue_avr) area_i=0; else area_i=1;
						//if(area_count[area_i]>area_count[1-area_i]){
						int parent_idx, child_idx;
						if(palettes[i].count>palettes[j].count) {
							parent_idx = j;
							child_idx = i;
						}
						else {
							parent_idx = i;
							child_idx = j;
						}
						
						{
							//int dispose_count; //吸収されるほうのカウント数
							if(palettes[child_idx].count<palettes[parent_idx].count/5) alpha *= 0.4f;
							else alpha *= palettes[child_idx].count/palettes[parent_idx].count*8f;
							
							//面を考慮
							boolean isSurface = false;
							for(int r=palettes[j].redmin>>1; r<=(palettes[j].redmax-1)>>1; r++){
								for(int g=palettes[j].greenmin>>2; g<=(palettes[j].greenmax-1)>>2; g++){
									for(int b=palettes[j].bluemin>>1; b<=(palettes[j].bluemax-1)>>1; b++){
										if(gradarea[((r)<<11)+((g)<<5)+(b)]>0||gradarea[((r)<<11)+((g)<<5)+(b)]!=0){
											isSurface = true;
											break;
										}
									}
								}
							}
							if(isSurface) alpha *= 10.5f;
						}
						/*else{
							int dispose_count; //吸収されるほうのカウント数
							dispose_count = palettes[i].count;
							if(dispose_count<area_count[area_i]/300) alpha *= 0.1f;
							else if(dispose_count<area_count[area_i]/150) alpha *= 0.3f;
							else if(dispose_count>area_count[area_i]/40) alpha *= 2.0f;
							//面を考慮
							boolean isSurface = false;
							for(int r=palettes[i].redmin>>1; r<=(palettes[i].redmax-1)>>1; r++){
								for(int g=palettes[i].greenmin>>2; g<=(palettes[i].greenmax-1)>>2; g++){
									for(int b=palettes[i].bluemin>>1; b<=(palettes[i].bluemax-1)>>1; b++){
										if(gradarea[((r)<<11)+((g)<<5)+(b)]>0||gradarea[((r)<<11)+((g)<<5)+(b)]!=0){
											isSurface = true; //とは言ってもだいたいここに来るのは面に相当するものばかりだが。
											break;
										}
									}
								}
							}
							if(isSurface) alpha *= 3.5f;
						}*/
							
						if((abs(sats[i]*bris[i]-sats[j]*bris[j])+abs(bris[i]-bris[j])+sats[i]*sats[j]*abs(hues[i]-hues[j])*2)*alpha < near_dist){
							near_dist = (abs(sats[i]*bris[i]-sats[j]*bris[j])+abs(bris[i]-bris[j])+sats[i]*sats[j]*abs(hues[i]-hues[j])/2)*alpha;
							near_i = parent_idx;
							near_j = child_idx;
						}
					}
				}
				if(near_i == -1) break;
					
				//統合処理
				//System.out.println("near_dist:"+near_dist);
				//System.out.println("Compaction:"+near_i+"+"+near_j);
				//int area_i;
				//if(hues[near_i]<hue_avr) area_i=0; else area_i=1;
				//if(area_count[area_i]>area_count[1-area_i]){ //総カウント数の多いほうに結合

				{
					colorCnt parent = palettes[near_i];
					while(parent.child!=null) {parent = parent.child;}
					parent.child = palettes[near_j];
					colorList.remove(palettes[near_j]);
					palettes[near_j] = null;
				}
				/*else{
					colorCnt parent = palettes[near_j];
					while(parent.child!=null) {parent = parent.child;}
					parent.child = palettes[near_i];
					colorList.remove(palettes[near_i]);
					palettes[near_i] = null;
				}*/
			}
		}

	}
	
	
	//選択した256色を元に元画像を減色する
	static BufferedImage colorSelect(BufferedImage srcbi, int colors, int dithermode,
			ArrayList<colorCnt> colorList, int[] counts, Color[] protects, IndexColorDialog dialog, BufferedImage minibi, byte[] gradarea)
	{
		int width = srcbi.getWidth();
		int height = srcbi.getHeight();
		DataBuffer db = srcbi.getRaster().getDataBuffer();
		DataBuffer minidb = minibi.getRaster().getDataBuffer();

		Color[] srcColorBest = new Color[colors];

		if(dithermode==-1){
			//色を決める。エリア内のピクセルの色の平均値 ピクセルの差が十分にあれば代表値
			for(int i=0; i<colorList.size(); i++){
				int red = 0;
				int green = 0;
				int blue = 0;
				int totalpix = 0;
				int maxpix = 0;
				int maxrgb = 0;

				colorCnt area = colorList.get(i);
				for(int r=area.redmin; r<area.redmax; r++){
					for(int g=area.greenmin; g<area.greenmax; g++){
						for(int b=area.bluemin; b<area.bluemax; b++){
							int c = counts[r*256*64+g*64+b];
							if(c==0) continue;
							/*if(r>=28*2 && r<g/2+12*2 && r>g/2+2*2 &&g/2<b+8*2 && g/2>b+1*2){
								//肌色エリア  はここではなく最初のピクセル数を数えるときに考慮する
								c *= 2;
							}*/
							red += r*c;
							green += g*c;
							blue += b*c;
							totalpix += c;
							if(c>maxpix){
								maxpix = c;
								maxrgb = r*256*64+g*64+b;
							}
						}
					}
				}
				if(totalpix==0) totalpix = 1;

				boolean isFound = false;
				if(protects!=null){
					for(int j=0; j<protects.length; j++){
						if(protects[j].getRed()>=area.redmin*4 && protects[j].getRed()<area.redmax*4 &&
							protects[j].getGreen()>=area.greenmin && protects[j].getGreen()<area.greenmax &&
							protects[j].getBlue()>=area.bluemin*4 && protects[j].getBlue()<area.bluemax*4)
						{
							//保護カラー
							srcColorBest[i] = protects[j];
	
							isFound = true;
							break;
						}
					}
				}
				
				if(isFound){
				}
				else if(maxpix >= totalpix/2){
					//代表値
					
					//24bitカラーでrgb値を一致させるために、元画像を左上から8pixとばして検索して一番最初に見つかった色にする
					for(int v=0; v<height; v+=8){
						for(int h=0; h<width; h+=8){
							int color = db.getElem(h+v*width);
							if(maxrgb == ((((color>>16)>>2)&0x3F)<<14)+((((color>>8)&0xFF)<<6)+((color>>2)&0x3F))){
								//完全に一致
								srcColorBest[i] = new Color((color>>16)&0xFF, ((color>>8)&0xFF), (color&0xFF));
								break;
							}
						}
					}
					if(srcColorBest[i] == null){
						//8pixとばしでは見つからないので1pixで検索
						for(int v=0; v<height; v+=1){
							for(int h=0; h<width; h+=1){
								int color = db.getElem(h+v*width);
								if(maxrgb == ((((color>>16)>>2)&0x3F)<<14)+((((color>>8)&0xFF)<<6)+((color>>2)&0x3F))){
									//完全に一致
									srcColorBest[i] = new Color((color>>16)&0xFF, ((color>>8)&0xFF), (color&0xFF));
									break;
								}
							}
						}
					}
					if(srcColorBest[i] == null){
						srcColorBest[i] = new Color((maxrgb>>14)*255/63, ((maxrgb>>6)&0xFF)*255/255, (maxrgb&0x3F)*255/63);
					}
				}
				else{
					//平均値
					srcColorBest[i] = new Color(red/totalpix*255/63, green/totalpix*255/255, blue/totalpix*255/63);
				}
				
				//##debug indexで色分け
				//srcColorBest[i] = new Color(i, (i*4)%256, (i*16)%256); 

				//##debug グラデーションエリア表示
				/*boolean isSurface = false;
				//for(int r=area.redmin>>1; r<=(area.redmax-1)>>1; r++){
					//for(int g=area.greenmin>>2; g<=(area.greenmax-1)>>2; g++){
						//for(int b=area.bluemin>>1; b<=(area.bluemax-1)>>1; b++){
							int r = (int)area.trueredavr>>1;
							int g = (int)area.truegreenavr>>2;
							int b = (int)area.trueblueavr>>1;
							int grad = gradarea[((r)<<11)+((g)<<5)+(b)];
							if(grad>32){
								srcColorBest[i] = Color.BLUE; //横方向/縦方向のグラデーション
								isSurface = true;
								//break;
							}
							else if(grad>1){
								srcColorBest[i] = Color.YELLOW; //横方向/縦方向のグラデーション
								isSurface = true;
								//break;
							}
							if(grad==1){
								srcColorBest[i] = Color.BLACK; //面
								isSurface = true;
								//break;
							}
							if(grad==-128){
								srcColorBest[i] = Color.RED; //強いグラデーション
								isSurface = true;
								//break;
							}
							if(grad<0){
								srcColorBest[i] = Color.GREEN; //縦方向のみのグラデーション
								isSurface = true;
								//break;
							}
						/*}
						if(isSurface )break;
					}
					if(isSurface )break;
				}*/
				
			}

			dialog.owner.progress(65);
			
			//色空間にどの色に近いのかを書き込む
			short[] colorAry = new short[1<<20];
			for(int i=0; i<colorAry.length; i++){
				colorAry[i] = -1;
			}
			for(short i=0; i<colorList.size(); i++){
				colorCnt area = colorList.get(i);
				while(area!=null){
					for(int r=area.redmin; r<area.redmax; r++){
						for(int g=area.greenmin; g<area.greenmax; g++){
							for(int b=area.bluemin; b<area.bluemax; b++){
								colorAry[r*256*64+g*64+b] = i;
							}
						}
					}
					area = area.child;
				}
			}

			dialog.owner.progress(68);
			
			//元画像を指定色のみに変換する
			width = srcbi.getWidth();
			height = srcbi.getHeight();
			//int[] ncc = new int[4];
			for(int v=0; v<height; v++){
				if(v%8==0) dialog.owner.progress(68+31*v/height);
				
				for(int h=0; h<width; h++){
					int cc = db.getElem(h+v*width);
					if((cc&0xFF000000)==0){
						db.setElem(h+v*width, 0x0000FF00);
						continue;
					}
					int c = minidb.getElem(h+v*width)&0x000FFFFF;
					//上下左右のうち2つ以上が一致する 色が近い色ならばそちらを採用
					/*if(h-1>=0) ncc[0] = db.getElem(h-1+v*width); else ncc[0] = cc;
					if(h+1<width) ncc[1] = db.getElem(h+1+v*width); else ncc[1] = cc;
					if(v-1>=0) ncc[2] = db.getElem(h+(v-1)*width); else ncc[2] = cc;
					if(v+1<height) ncc[3] = db.getElem(h+(v+1)*width); else ncc[3] = cc;
					
					if(ncc[0]!=cc &&(ncc[0]==ncc[1] || ncc[0]==ncc[2] || ncc[0]==ncc[3])){
						int nc = minidb.getElem(h-1+v*width)&0x000FFFFF;
						if(isNear(cc, srcColorBest[colorAry[nc]], 8*384/(colors+128))){
							c = nc;
						}
					}
					else if(ncc[1]!=cc &&(ncc[1]==ncc[2] || ncc[1]==ncc[3])){
						int nc = minidb.getElem(h+1+v*width)&0x000FFFFF;
						if(isNear(cc, srcColorBest[colorAry[nc]], 8*384/(colors+128))){
							c = nc;
						}
					}
					else if(ncc[2]!=cc &&(ncc[2]==ncc[3])){
						int nc = minidb.getElem(h+(v-1)*width)&0x000FFFFF;
						if(isNear(cc, srcColorBest[colorAry[nc]], 8*384/(colors+128))){
							c = nc;
						}
					}*/
					
					if(/*colorList.size()>32 &&*/ colorAry[c]!=-1){
						Color newcolor = srcColorBest[colorAry[c]];
						int d = (cc&0xFF000000) + (newcolor.getRed()<<16) + (newcolor.getGreen()<<8) +(newcolor.getBlue());
						db.setElem(h+v*width, d);
						continue;
					}
					boolean isFound = false;
					int nearoffset = 0;
					while(!isFound){
						for(int i=0; i<srcColorBest.length; i++){
							if(srcColorBest[i]==null) continue;
							if(isNear(cc, srcColorBest[i], nearoffset)){
								Color newcolor = srcColorBest[i];
								int d = (cc&0xFF000000) + (newcolor.getRed()<<16) + (newcolor.getGreen()<<8) +(newcolor.getBlue());
								db.setElem(h+v*width, d);
								isFound = true;
								break;
							}
						}
						nearoffset += nearoffset/2+1;
					}
				}
			}

			dialog.owner.progress(100);
			
			/*//明るさソート
			for(int i=0; i<srcColorBest.length-1; i++){
				if(srcColorBest[i]==null) continue;
				for(int j=i+1; j<srcColorBest.length; j++){
					if(srcColorBest[j]==null) continue;
					if(srcColorBest[i].getRed()+srcColorBest[i].getGreen()+srcColorBest[i].getBlue()
							<srcColorBest[j].getRed()+srcColorBest[j].getGreen()+srcColorBest[j].getBlue()){
						Color c = srcColorBest[i];
						srcColorBest[i] = srcColorBest[j];
						srcColorBest[j] = c;
					}
				}
			}*/
			//色相ソート
			for(int i=0; i<srcColorBest.length-1; i++){
				if(srcColorBest[i]==null) continue;
				for(int j=i+1; j<srcColorBest.length; j++){
					if(srcColorBest[j]==null) continue;
					float[] f1 = Color.RGBtoHSB(srcColorBest[i].getRed(),srcColorBest[i].getGreen(),srcColorBest[i].getBlue(), null);

					float[] f2 = Color.RGBtoHSB(srcColorBest[j].getRed(),srcColorBest[j].getGreen(),srcColorBest[j].getBlue(), null);
					if((int)(f1[0]*32)+f1[2]>(int)(f2[0]*32)+f2[2]){
						Color c = srcColorBest[i];
						srcColorBest[i] = srcColorBest[j];
						srcColorBest[j] = c;
					}
				}
			}
			
			//パレット表示
			/*for(int y=0; y<8; y++){
				for(int x=0; x<srcColorBest.length/8; x++){
					if(srcColorBest[x+y*srcColorBest.length/8]==null) continue;
					int d = 0xFF000000+(srcColorBest[x+y*srcColorBest.length/8].getRed()<<16)+(srcColorBest[x+y*srcColorBest.length/8].getGreen()<<8)+(srcColorBest[x+y*srcColorBest.length/8].getBlue()<<0);
					for(int i=0; i<4; i++){
						for(int j=0; j<4; j++){
							db.setElem(x*4+i+(y*4+j)*width, d);
						}
					}
				}
			}*/
			
			return srcbi;
		}

		/////////////////////
		//  ディザを使用する場合
		/////////////////////
		
		
		//色を決める。エリア内のピクセルの色の平均値 ピクセルの差が十分にあれば代表値
		int rmin = 255;
		int gmin = 255;
		int bmin = 255;
		int rmax = 0;
		int gmax = 0;
		int bmax = 0;
		for(int i=0; i<colorList.size(); i++){
			double red = 0;
			double green = 0;
			double blue = 0;
			double totalpix = 0;
			int maxpix = 0;
			int maxrgb = 0;

			colorCnt area = colorList.get(i);
			for(int r=area.redmin; r<area.redmax; r++){
				for(int g=area.greenmin; g<area.greenmax; g++){
					for(int b=area.bluemin; b<area.bluemax; b++){
						int c = counts[r*256*64+g*64+b];
						if(c==0) continue;
						red += r*c;
						green += g*c;
						blue += b*c;
						totalpix += c;
						if(c>maxpix){
							maxpix = c;
							maxrgb = r*256*64+g*64+b;
						}
						if(r*255/63<rmin) rmin = r*255/63;
						if(r*255/63>rmax) rmax = r*255/63;
						if(g<gmin) gmin = g;
						if(g>gmax) gmax = g;
						if(b*255/63<bmin) bmin = b*255/63;
						if(b*255/63>bmax) bmax = b*255/63;
					}
				}
			}
			if(totalpix==0) totalpix = 1;

			boolean isFound = false;
			if(protects!=null){
				for(int j=0; j<protects.length; j++){
					if(protects[j].getRed()>=area.redmin*4 && protects[j].getRed()<area.redmax*4 &&
						protects[j].getGreen()>=area.greenmin && protects[j].getGreen()<area.greenmax &&
						protects[j].getBlue()>=area.bluemin*4 && protects[j].getBlue()<area.bluemax*4)
					{
						//保護カラー
						srcColorBest[i] = protects[j];
	
						isFound = true;
						break;
					}
				}
			}
			
			if(isFound){
				
			}
			else if(maxpix >= totalpix*3/4){
				//代表値
				srcColorBest[i] = new Color((int)((0x003F&(maxrgb>>14))*255/63), ((maxrgb>>6)&0x00FF)*255/255, (maxrgb&0x003F)*255/63);
			}
			else{
				//平均値を基本にする
				int cred = ((int)(red/totalpix)*255/63);
				int cgreen = ((int)(green/totalpix)*255/255);
				int cblue = ((int)(blue/totalpix)*255/63);
				
				//全空間でrgbの彩度が最も高い場合はそれを採用する
				int minbright = 1000;
				int maxbright = 0;
				/*int redsat = -255;
				int greensat = -255;
				int bluesat = -255;
				int rgsat = -255;
				int gbsat = -255;
				int rbsat = -255;*/
				for(int j=0; j<colorList.size(); j++){
					colorCnt aarea = colorList.get(j);
					if(i==j) continue;
					if(aarea.redavr+aarea.greenavr/4+aarea.blueavr<minbright) {
						minbright = (int)(aarea.redavr+aarea.greenavr/4+aarea.blueavr);
					}
					if(aarea.redavr+aarea.greenavr/4+aarea.blueavr>maxbright) {
						maxbright = (int)(aarea.redavr+aarea.greenavr/4+aarea.blueavr);
					}
					/*if(aarea.redmax-min(aarea.greenmax/4,aarea.bluemax)>redsat) {
						redsat = aarea.redmax-min(aarea.greenmax/4,aarea.bluemax);
					}
					if(aarea.greenmax/4-min(aarea.redmax,aarea.bluemax)>greensat) {
						greensat = aarea.greenmax/4-min(aarea.redmax,aarea.bluemax);
					}
					if(aarea.bluemax-min(aarea.greenmax/4,aarea.redmax)>bluesat) {
						bluesat = aarea.bluemax-min(aarea.greenmax/4,aarea.redmax);
					}
					if(min(aarea.redmax,aarea.greenmax/4)-aarea.bluemax>rgsat) {
						rgsat = min(aarea.redmax,aarea.greenmax/4)-aarea.bluemax;
					}
					if(min(aarea.bluemax,aarea.greenmax/4)-aarea.redmax>gbsat) {
						gbsat = min(aarea.bluemax,aarea.greenmax/4)-aarea.redmax;
					}
					if(min(aarea.redmax,aarea.bluemax)-aarea.greenmax/4>rbsat) {
						rbsat = min(aarea.redmax,aarea.bluemax)-aarea.greenmax/4;
					}*/
				}
				if(area.redavr+area.greenavr/4+area.blueavr<minbright) {
					//明るい
					cred = (cred*3+area.redmin*255/63)/4;
					cgreen = (cgreen*3+area.greenmin*255/255)/4;
					cblue = (cblue*3+area.bluemin*255/63)/4;
				}
				else if(area.redavr+area.greenavr/4+area.blueavr>maxbright) {
					//暗い
					cred = (cred*3+(area.redmax-1)*255/63)/4;
					cgreen = (cgreen*3+(area.greenmax-1)*255/255)/4;
					cblue = (cblue*3+(area.bluemax-1)*255/63)/4;
				}
				/*else if(min(area.redmax,area.bluemax)-area.greenmax/4>rbsat){
					cred = (cred*3+(area.redmax-1)*255/63)/4;
					cblue = (cblue*3+(area.bluemax-1)*255/63)/4;
				}
				else if(min(area.redmax,area.greenmax/4)-area.bluemax>rgsat){
					cred = (cred*3+(area.redmax-1)*255/63)/4;
					cgreen = (cgreen*3+(area.greenmax-1)*255/255)/4;
				}
				else if(min(area.bluemax,area.greenmax/4)-area.redmax>gbsat){
					cgreen = (cgreen*3+(area.greenmax-1)*255/255)/4;
					cblue = (cblue*3+(area.bluemax-1)*255/63)/4;
				}
				else if(area.redmax-min(area.greenmax/4,area.bluemax)>redsat){
					cred = (cred*3+(area.redmax-1)*255/63)/4;
				}
				else if(area.greenmax/4-min(area.redmax,area.bluemax)>greensat){
					cgreen = (cgreen*3+(area.greenmax-1)*255/255)/4;
				}
				else if(area.bluemax-min(area.greenmax/4,area.redmax)>bluesat){
					cblue = (cblue*3+(area.bluemax-1)*255/63)/4;
				}*/
				srcColorBest[i] = new Color(cred, cgreen, cblue);
			}
			
			//srcColorBest[i] = new Color(i, (i*4)%256, (i*16)%256); //indexで色分け
		}

		dialog.owner.progress(70);
		
		counts = null;
		System.gc();
		BufferedImage newimg = new BufferedImage(srcbi.getWidth(), srcbi.getHeight(), BufferedImage.TYPE_INT_ARGB);

		if(dithermode==0 || dithermode==1){
			//色空間にどの色に近いのかを書き込む
			short[] colorAry = new short[1<<20];
			/*for(int i=0; i<colorAry.length; i++){
				colorAry[i] = -1;
			}*/
			for(short i=0; i<colorList.size(); i++){
				colorCnt area = colorList.get(i);
				while(area!=null){
					for(int r=area.initArea[0]; r<area.initArea[1]; r++){
						for(int g=area.initArea[2]; g<area.initArea[3]; g++){
							for(int b=area.initArea[4]; b<area.initArea[5]; b++){
								colorAry[r*256*64+g*64+b] = i;
							}
						}
					}
					area = area.child;
				}
			}

			dialog.owner.progress(73);

			DataBuffer newdb = newimg.getRaster().getDataBuffer();
			width = srcbi.getWidth();
			height = srcbi.getHeight();
			for(int v=0; v<height; v++){
				for(int h=0; h<width; h++){
					newdb.setElem(h+v*width, 0);
				}
			}

			DataBuffer srcdb = srcbi.getRaster().getDataBuffer();
			for(int v=0; v<height; v++){
				if(v%8==0) dialog.owner.progress(73+26*v/height);
				//横方向はジグザグにスキャンする
				int hstart = 0;
				int hend = width;
				int add=1;
				if(v%2==1){
					hstart = width-1;
					hend = -1;
					add=-1;
				}
				for(int h=hstart; h!=hend; h+=add){
					int color = srcdb.getElem(h+v*width);
					
					//r,g,bを求める
					int carrycolor = newdb.getElem(h+v*width);
					int r = ((color>>16)&0xFF)+(byte)((carrycolor>>16)&0xFF);
					int g = ((color>>8)&0xFF)+(byte)((carrycolor>>8)&0xFF);
					int b = ((color)&0xFF)+(byte)((carrycolor>>0)&0xFF);
					int rr, gg, bb;
					rr = r;
					if(r>rmax) {
						r = rmax;
					} else if(r<rmin) {
						r = rmin;
					}
					gg = g;
					if(g>gmax) {
						g = gmax;
					} else if(g<gmin) {
						g = gmin;
					}
					bb = b;
					if(b>bmax) {
						b = bmax;
					} else if(b<bmin) {
						b = bmin;
					}
					
					//ピクセルをインデックスカラーの近いものにする
					int index = colorAry[(r>>2)*256*64+(g>>0)*64+(b>>2)];
					Color newcolor = srcColorBest[index];
					if(colorList.size()<=32){
						//力技検索
						int rrr, ggg, bbb;
						{
							rrr = (1*r + 7*newcolor.getRed())/8;
							ggg = (1*g + 7*newcolor.getGreen())/8;
							bbb = (1*b + 7*newcolor.getBlue())/8;
						}
						boolean isFound = false;
						int nearoffset = 0;
						while(!isFound){
							for(int i=0; i<srcColorBest.length; i++){
								if(srcColorBest[i]==null) continue;
								if(isNearWithHue((rrr<<16)+(ggg<<8)+bbb, srcColorBest[i], nearoffset)){
									newcolor = srcColorBest[i];
									isFound = true;
									break;
								}
							}
							nearoffset += nearoffset/2+1;
						}
						if(srcColorBest[index]!=newcolor){
							//誤差蓄積防止
							rr = (newcolor.getRed());
							gg = (newcolor.getGreen());
							bb = (newcolor.getBlue());
						}
					}
					
					//あまりにもかけ離れている場合は採用しない
					if(colorList.size()>=64 && abs(((color>>16)&0xFF)-newcolor.getRed()) +abs(((color>>8)&0xFF)-newcolor.getGreen()) +abs(((color)&0xFF)-newcolor.getBlue()) >36*544/(32+colorList.size())){
						if(colorList.size()<=64){
							//力技検索
							boolean isFound = false;
							int nearoffset = 1;
							while(!isFound){
								for(int i=0; i<srcColorBest.length; i++){
									if(srcColorBest[i]==null) continue;
									if(isNearWithHue(color, srcColorBest[i], nearoffset)){
										newcolor = srcColorBest[i];
										isFound = true;
										break;
									}
								}
								nearoffset += nearoffset/2+1;
							}
							rr = newcolor.getRed();
							gg = newcolor.getGreen();
							bb = newcolor.getBlue();
						}
						else{
							r = ((color>>16)&0xFF);
							g = ((color>>8)&0xFF);
							b = ((color)&0xFF);
							index = colorAry[(r>>2)*256*64+(g>>0)*64+(b>>2)];
							newcolor = srcColorBest[index];
						}
					}
					
					//ピクセルに反映
					int d = (color&0xFF000000) + (newcolor.getRed()<<16) + (newcolor.getGreen()<<8) +(newcolor.getBlue());
					if((color&0xFF000000)==0) newdb.setElem(h+v*width, 0x0000FF00);
					else newdb.setElem(h+v*width, d);

					//余りを求める
					int cr = (rr - newcolor.getRed());
					if(cr>127) cr = 127;
					if(cr<-127) cr = -127;
					int cg = (gg - newcolor.getGreen());
					if(cg>127) cg = 127;
					if(cg<-127) cg = -127;
					int cb = (bb - newcolor.getBlue());
					if(cb>127) cb = 127;
					if(cb<-127) cb = -127;
					
					//誤差を拡散する
					if(dithermode == 0){
						//Floyd-Steinburg Algorithm
						if(h+add>=0 && h+add<width) {
							int dd = newdb.getElem(h+add+v*width);
							int er = ((byte)((dd>>16)&0xFF)+cr*14/32);
							int eg = ((byte)((dd>>8)&0xFF)+cg*14/32);
							int eb = ((byte)((dd)&0xFF)+cb*14/32);
							newdb.setElem(h+add+v*width, ((0x00FF&er)<<16)+((0x00FF&eg)<<8)+((0x00FF&eb)));
						}
						if(v+1>=height) continue;
						if(h-add>=0 && h-add<width){
							int dd = newdb.getElem(h-add+(v+1)*width);
							int er = (byte)((dd>>16)&0xFF)+cr*3/16;
							int eg = (byte)((dd>>8)&0xFF)+cg*3/16;
							int eb = (byte)((dd)&0xFF)+cb*3/16;
							newdb.setElem(h-add+(v+1)*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
						{
							int dd = newdb.getElem(h+(v+1)*width);
							int er = (byte)((dd>>16)&0xFF)+cr*10/32;
							int eg = (byte)((dd>>8)&0xFF)+cg*10/32;
							int eb = (byte)((dd)&0xFF)+cb*10/32;
							newdb.setElem(h+(v+1)*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
						if(h+add>=0 && h+add<width){
							int dd = newdb.getElem(h+add+(v+1)*width);
							int er = (byte)((dd>>16)&0xFF)+cr*1/16;
							int eg = (byte)((dd>>8)&0xFF)+cg*1/16;
							int eb = (byte)((dd)&0xFF)+cb*1/16;
							newdb.setElem(h+add+(v+1)*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
					}else{
						//Bill Atkinson Algorithm
						if(h+add>=0 && h+add<width) {
							int dd = newdb.getElem(h+add+v*width);
							int er = (byte)((dd>>16)&0xFF)+cr/8;
							int eg = (byte)((dd>>8)&0xFF)+cg/8;
							int eb = (byte)((dd)&0xFF)+cb/8;
							newdb.setElem(h+add+v*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
						if(h+add*2>=0 && h+add*2<width) {
							int dd = newdb.getElem(h+add*2+v*width);
							int er = (byte)((dd>>16)&0xFF)+cr/8;
							int eg = (byte)((dd>>8)&0xFF)+cg/8;
							int eb = (byte)((dd)&0xFF)+cb/8;
							newdb.setElem(h+add*2+v*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
						if(v+1>=height) continue;
						if(h-1>=0){
							int dd = newdb.getElem(h-1+(v+1)*width);
							int er = (byte)((dd>>16)&0xFF)+cr/8;
							int eg = (byte)((dd>>8)&0xFF)+cg/8;
							int eb = (byte)((dd)&0xFF)+cb/8;
							newdb.setElem(h-1+(v+1)*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
						{
							int dd = newdb.getElem(h+(v+1)*width);
							int er = (byte)((dd>>16)&0xFF)+cr/8;
							int eg = (byte)((dd>>8)&0xFF)+cg/8;
							int eb = (byte)((dd)&0xFF)+cb/8;
							newdb.setElem(h+(v+1)*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
						if(h+1<width){
							int dd = newdb.getElem(h+1+(v+1)*width);
							int er = (byte)((dd>>16)&0xFF)+cr/8;
							int eg = (byte)((dd>>8)&0xFF)+cg/8;
							int eb = (byte)((dd)&0xFF)+cb/8;
							newdb.setElem(h+1+(v+1)*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
						if(v+2<height){
							int dd = newdb.getElem(h+(v+2)*width);
							int er = (byte)((dd>>16)&0xFF)+cr/8;
							int eg = (byte)((dd>>8)&0xFF)+cg/8;
							int eb = (byte)((dd)&0xFF)+cb/8;
							newdb.setElem(h+(v+2)*width, ((0xFF&er)<<16)+((0xFF&eg)<<8)+(0xFF&eb));
						}
					}
				}
			}
		}
		else if(dithermode==2){
			//パターン処理はdrawImageに任せる
			
			//IndexColorModelを作成
			byte[] r_a = new byte[256];
			byte[] g_a = new byte[256];
			byte[] b_a = new byte[256];
			for(int i=0; i<srcColorBest.length; i++){
				if(srcColorBest[i] == null) continue;
				r_a[i] = (byte) srcColorBest[i].getRed();
				g_a[i] = (byte) srcColorBest[i].getGreen();
				b_a[i] = (byte) srcColorBest[i].getBlue();
			}
			
			IndexColorModel colorModel = new IndexColorModel(8, colors, r_a, g_a, b_a);
			
			//TYPE_BYTE_INDEXEDのBufferedImageを作る
			BufferedImage indeximg = new BufferedImage(srcbi.getWidth(), srcbi.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, colorModel);
			Graphics2D indexg = indeximg.createGraphics();
			indexg.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
			indexg.fillRect(0,0,indeximg.getWidth(), indeximg.getHeight());
			indexg = indeximg.createGraphics();
			indexg.drawImage(srcbi, 0,0, null);
			
			Graphics2D newg = newimg.createGraphics();
			newg.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
			newg.fillRect(0,0,newimg.getWidth(), newimg.getHeight());
			newimg.createGraphics().drawImage(indeximg, 0,0, null);
		}

		dialog.owner.progress(100);
		
		return newimg;
	}


	/*private final boolean isNear(Color color1, Color color2, int near){
		if(abs(color1.getRed()-color2.getRed())>near){
			return false;
		}
		if(abs(color1.getGreen()-color2.getGreen())>near){
			return false;
		}
		if(abs(color1.getBlue()-color2.getBlue())>near){
			return false;
		}
		return true;
	}*/
	

	//色相も考慮したisNear関数
	private static final boolean isNearWithHue(int argb, Color color, int near){
		if(abs(color.getRed()-((argb>>16)&0xFF))<=near){
			if(abs(color.getGreen()-((argb>>8)&0xFF))<=near){
				if(abs(color.getBlue()-((argb>>0)&0xFF))<=near){
					return true;
				}
			}
		}
		float hue1;
		float hue2;
		float sat1;
		float sat2;
		float bri1;
		float bri2;
		bri1 = (0.29891f*color.getRed() + 0.58661f*color.getGreen() + 0.1144f*color.getBlue());
		if(color.getRed()>color.getBlue()&&color.getRed()>color.getGreen()){
			//bri1 = color.getRed();
			sat1 = (color.getRed()-min(color.getGreen(),color.getBlue()));
			hue1 = 60*(color.getGreen()-color.getBlue())/sat1;
		}
		else if(color.getGreen()>color.getBlue()&&color.getGreen()>color.getRed()){
			//bri1 = color.getGreen();
			sat1 = (color.getGreen()-min(color.getRed(),color.getBlue()));
			hue1 = 120+60*(color.getBlue()-color.getRed())/sat1;
		}
		else if(color.getBlue()>color.getGreen()&&color.getBlue()>color.getRed()){
			//bri1 = color.getBlue();
			sat1 = (color.getBlue()-min(color.getRed(),color.getGreen()));
			hue1 = 240+60*(color.getRed()-color.getGreen())/sat1;
		}
		else {
			hue1 = -1;
			sat1 = 0;
			//bri1 = color.getRed();
		}
		bri2 = (0.29891f*((argb>>16)&0xFF) + 0.58661f*((argb>>8)&0xFF) + 0.1144f*((argb>>0)&0xFF));
		if(((argb>>16)&0xFF)>((argb>>0)&0xFF)&&((argb>>16)&0xFF)>((argb>>8)&0xFF)){
			//bri2 = ((argb>>16)&0xFF);
			sat2 = (((argb>>16)&0xFF)-min(((argb>>8)&0xFF),((argb>>0)&0xFF)));
			hue2 = 60*(((argb>>8)&0xFF)-((argb>>0)&0xFF))/sat2;
		}
		else if(((argb>>8)&0xFF)>((argb>>0)&0xFF)&&((argb>>8)&0xFF)>((argb>>16)&0xFF)){
			//bri2 = ((argb>>8)&0xFF);
			sat2 = (((argb>>8)&0xFF)-min(((argb>>16)&0xFF),((argb>>0)&0xFF)));
			hue2 = 120+60*(((argb>>0)&0xFF)-((argb>>16)&0xFF))/sat2;
		}
		else if(((argb>>0)&0xFF)>((argb>>8)&0xFF)&&((argb>>0)&0xFF)>((argb>>16)&0xFF)){
			//bri2 = ((argb>>0)&0xFF);
			sat2 = (((argb>>0)&0xFF)-min(((argb>>16)&0xFF),((argb>>8)&0xFF)));
			hue2 = 240+60*(((argb>>16)&0xFF)-((argb>>8)&0xFF))/sat2;
		}
		else{
			sat2 = 0;
			hue2 = -1;
			//bri2 = ((argb>>0)&0xFF);
		}
		if(sat1*2<=near&&sat2<=near){ //グレー系
			if(abs(bri1-bri2)<=near*3){
				return true;
			}
		}
		if((hue1!=-1)&&(hue2!=-1)&&(abs((hue1-hue2+720)%360)*((sat1+128)/256*(sat2+256)/256)<=near*1+1)){
			//色相が近い
			if(abs(bri1-bri2)<=(near+10)*2 && abs(sat1-sat2)<=(near+2)*2){
				//明るさと彩度はおおよそ近い
				return true;
			}
		}
		return false;
	}
	

	private static final boolean isNear(int argb, Color color, int near){
		if(abs(color.getRed()-((argb>>16)&0xFF))>near){
			return false;
		}
		if(abs(color.getGreen()-((argb>>8)&0xFF))>near){
			return false;
		}
		if(abs(color.getBlue()-((argb>>0)&0xFF))>near){
			return false;
		}
		/*if((color.getBlue()-((argb>>0)&0xFF)) - (color.getGreen()-((argb>>8)&0xFF))>near){
			return false;
		}
		if((color.getGreen()-((argb>>8)&0xFF)) - (color.getRed()-((argb>>16)&0xFF))>near){
			return false;
		}
		if((color.getRed()-((argb>>16)&0xFF)) - (color.getBlue()-((argb>>0)&0xFF))>near){
			return false;
		}*/
		return true;
	}
	
	
	private static final int max(int a, int b){
		return a>b?a:b;
	}
	private static final int min(int a, int b){
		return a<b?a:b;
	}
	private static final int abs(int a){
		return a>0?a:-a;
	}
	/*private static final float max(float a, float b){
		return a<b?a:b;
	}
	private static final float min(float a, float b){
		return a<b?a:b;
	}*/
	private static final float abs(float a){
		return a>0?a:-a;
	}
	
	//hsbとYUV輝度を求める
	private static final void RGBtoHSBY(int r, int g, int b, float [] f){
		if(r>=g&&r>=b){
			f[2] = r/255f;
			if(r>0){
				f[1] = ((float)r-min(g,b))/r;
				if(f[1]>0){
					f[0] = (60f*((float)g-b)/((float)r-min(g,b)))/360f;
					if(f[0]<0) f[0]+=1.0f;
				}
				else f[0] = 0f;
			}
			else{
				f[2] = 0f;
				f[1] = 0f;
				f[0] = 0f;
			}
		}
		else if(g>=r&&g>=b){
			f[2] = g/255f;
			f[1] = ((float)g-min(r,b))/g;
			if(f[1]>0){
				f[0] = (120f+60f*((float)b-r)/((float)g-min(r,b)))/360f;
			}
			else f[0] = 0f;
		}
		else {
			f[2] = b/255f;
			f[1] = ((float)b-min(r,g))/b;
			if(f[1]>0){
				f[0] = (240f+60f*((float)r-g)/((float)b-min(r,g)))/360f;
			}
			else f[0] = 0f;
		}
		f[3] = (0.29891f*r + 0.58661f*g + 0.1144f*b)/255f;
	}
}