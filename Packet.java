
package simple;

import java.io.Serializable;

public class Packet implements  Serializable{
    private int seq; // the seq number
    private boolean acked; // if it is acked
    private byte[] payload; // the datagram
    private int len; // the length of data
    public String state; // the state of packet
    public String flag;//if it is the last packet it will be set to true
    private int reTransmits ; // the times of retransmits
    private int position ;
    private long checksum;

    
    public Packet() {
    }

    public Packet(Packet p) {
        this.acked = p.acked;//used in SRSender
        this.len = p.len;
        this.payload = p.payload;
        this.state = p.state;
        this.reTransmits = p.reTransmits;
    }

    /**
     * the useful constructor
     *
     * @param seq     the number of seq
     * @param payload the datagram
     * @param state   the state of packet
     */
    public Packet(int seq, byte[] payload, String state,String flag) {
        this.seq = seq;
        this.payload = payload;
        this.state = state;
        this.len = payload.length;
        this.flag=flag;
    }

    /**
     * set seq number
     *
     * @param seq the number of seq
     */
    public void setSeq(int seq) {
        this.seq = seq;
    }

    /**
     * set ack to packet
     *
     * @param ack the ack
     */
    public void setAck(boolean ack) {
        this.acked = ack;
    }

    /**
     * set payload
     *
     * @param payload the datagram
     */
    public void setPayLoad(byte[] payload) {
        this.payload = payload;
    }
    public byte[] getPayload() {
        return payload;
    }
    public int getSeq() {
        return seq;
    }
    public String getFlag() {
        return flag;
    }
    public boolean isAcked() {
        return acked;
    }
    public String getState() {
        return state;
    }
    public void setState(String value){
       this.state=value;
    }
    public int getPosition() {
        return position;
    }
    public void setPosition(int value){
       this.position=value;
    }

    public int getRetransmit() {
        return reTransmits;
    }
    public void setRetransmit(int value){
       this.reTransmits=value;
    }
    public long getChecksum() {
        return checksum;
    }
    public void setChecksum(long value){
       this.checksum=value;
    }
 
}