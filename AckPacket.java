
package simple;

import java.io.Serializable;


public class AckPacket implements  Serializable{
    private String AckORNak;
    private int seqNo;
    private int checkSum;
    private int position;
    public AckPacket(String AckORNak,int seqNo){
        this.AckORNak=AckORNak;
        this.seqNo=seqNo;
    }
public AckPacket(){
    
}

   
    public String getAckORNak() {
        return AckORNak;
    }

    public void setAckORNak(String AckORNak) {
        this.AckORNak = AckORNak;
        
    }
    public int getSeqNo(){
        return seqNo;
    }
    public void setSeqNo(int seqNo){
        this.seqNo=seqNo;
    }
    public int getCheckSum(){
        return checkSum;
    }
    public void setCheckSum(int checkSum){
        this.checkSum=checkSum;
    }
     public int getPostion(){
        return position;
    }
    public void setPosition(int value){
        this.position=value;
    }
    
}
