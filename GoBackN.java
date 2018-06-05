package simple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GoBackN implements Runnable, Serializable {

    DatagramSocket socket;
    int ClientId;
    String fileName;
    Server server;
    int port;
    InetAddress IPAddress;
    
    private Queue<Packet> ReadyQueue = new LinkedList<Packet>();
    private HashMap<Integer, ArrayList<String>> statesOfPackets = new HashMap<Integer, ArrayList<String>>();
    private HashMap<Integer, Boolean> AckMap = new HashMap<Integer, Boolean>();
    private LinkedList<Packet> windowPacket = new LinkedList<Packet>();
    private int MaxWindowSize = 1000;
    private int timeOut = 4000;
    private int windowSize;
    private int timeout ;
    private float probOfCorrupt=(float)0.2;
    private float probOfLoss = (float) 0.3;

    public GoBackN(int ClientID, String fileName, Server server, int port, InetAddress IPAddress, int MaxWindowSize, int timeOut) throws SocketException {
        this.socket = new DatagramSocket();
        this.port = port;
        this.IPAddress = IPAddress;
        this.ClientId = ClientID;
        this.fileName = fileName;
        this.server = server;
        this.timeOut = timeOut;
        this.MaxWindowSize = MaxWindowSize;
    }

    void sendFileToClient(byte[][] packets) throws IOException,Exception {


        byte[] recieveAck = new byte[1000];

        pushToQueue(packets);
        windowSize = Math.min(ReadyQueue.size(), MaxWindowSize);      
        PushToWindow(ReadyQueue, windowPacket);        
        int index = 0;
        int windowPacketSize = windowPacket.size();      
        SetPosition(windowPacket);
        while (windowPacketSize != 0) {
         
            Packet p = windowPacket.get(index);
        
            if (p.getRetransmit() == 0) {
                p.setState("Sent");
                p.setRetransmit(1);
                
            } else {
                p.setState("retransmit");
                int r = p.getRetransmit() + 1;
                p.setRetransmit(r);
                 
            }
            write(p.getSeq(), p.getState());
            if(!isLoss(probOfLoss)){
                if(!isCorrupted(probOfCorrupt)){
                    System.out.println("packet no "+p.getSeq()+"is not corrupted");
                    SendPacket(p);
                    windowPacketSize--;
                    index++;
                }
                else{
                   
                    p.setChecksum(-1);
                    System.out.println("packet no "+p.getSeq()+"is corrupted");
                    SendPacket(p);
                    windowPacketSize--;
                    index++;
                }
            }
            else{
                   System.out.println("packet no "+p.getSeq()+"is Lost");
                  continue;   
            }
           
        }

        while (true) {
            
            index = 0;
            timeout = 2000;

            int seqAck;
           
            socket.setSoTimeout(timeout);

            try {

                do {
                    DatagramPacket receiveAckPacket = new DatagramPacket(recieveAck, recieveAck.length);
                    socket.receive(receiveAckPacket);
                    AckPacket ap = (AckPacket) deserialize(receiveAckPacket.getData());
                    seqAck = ap.getSeqNo();
                    System.out.println(" Seq no  " + seqAck);
                   } while (windowPacket.getFirst().getSeq() != seqAck);
                
                System.out.println("ACCEPTED ack packet seq no " + seqAck);
               
                windowPacket.remove(0);
                if (ReadyQueue.size() > 1) {
                    Packet p = ReadyQueue.poll();             
                    p.setChecksum(CheckSum(p.getPayload()));
                    windowPacket.addLast(p);
                    SetPosition(windowPacket);
                    if(!isLoss(probOfLoss)){
                        if(!isCorrupted(probOfCorrupt))
                        {
                          System.out.println("packet no "+p.getSeq()+"is not corrupted");
                            SendPacket(p);
                        }
                        else{
                                 System.out.println("packet no "+p.getSeq()+"is corrupted");

                            p.setChecksum(-1);
                            SendPacket(p);
                        }
                    }
                    else{
                        System.out.println("packet no "+p.getSeq()+"is loss");
                        continue;
                    }
                    
                } else if (!windowPacket.isEmpty()) {
                    Packet p;
                    if (ReadyQueue.size() == 1) {
                        p = ReadyQueue.poll();
                        windowPacket.addLast(p);
                        SetPosition(windowPacket);
                        p.setChecksum(CheckSum(p.getPayload()));

                        if(!isLoss(probOfLoss)){
                        if(!isCorrupted(probOfCorrupt))
                        {    
                            System.out.println("packet no "+p.getSeq() +"is not corrupted");
                            SendPacket(p);
                        }
                        else{
                            System.out.println("packet no "+p.getSeq()+" is corrupted");
                            p.setChecksum(-1);
                            SendPacket(p);
                        }
                    }
                    else{ 
                            System.out.println("packet no "+p.getSeq()+" is lost");
                           continue;
                            
                    }

                        //    SetPosition(windowPacket);
                    }

          

                } else {
                    break;
                }
            
            } catch (SocketTimeoutException e) {
               System.out.println(" time outttttttttttt");
                windowPacketSize = windowPacket.size();
               // System.out.println("hiiiii ana exception henaa   ");
              //  System.out.println("windowPacketSize : " + windowPacketSize);
                while (windowPacketSize != 0) {
                    
                    ///leh hena bne3mel  get index msh get zero
                    Packet p = windowPacket.get(index);
                    //Packet p = windowPacket.get(0);
                                        
                    p.setChecksum(CheckSum(p.getPayload()));

                    
                    
                    
                    System.out.println("index  :" + index + "seq no :" + p.getSeq());

                    if (p.getRetransmit() == 0) {
                        p.setState("Sent");
                        p.setRetransmit(1);
                    } else {
                        p.setState("retransmit");
                        int r = p.getRetransmit() + 1;
                        p.setRetransmit(r);
                    }
                    write(p.getSeq(), p.getState());
                    if(!isLoss(probOfLoss)){
                        if(!isCorrupted(probOfCorrupt))
                        {
                            System.out.println("packet no "+p.getSeq()+"is not corrupted");

                            SendPacket(p);
                            index++;
                            windowPacketSize--;
                        }
                        else{
                            System.out.println("packet no "+p.getSeq()+"is corrupted");
       
                            p.setChecksum(-1);
                            SendPacket(p);
                            index++;
                           windowPacketSize--;
                        }
                    }
                    else{
                   System.out.println("packet no "+p.getSeq()+"is Lost");
                  continue;   
            }
                    
                }
            }
            System.out.println("");
            //System.out.println("da5al tani   ");
        }
            
       System.out.println("Done " );
       Packet done=new Packet(-2, "Done".getBytes(),"Done","Done");
       byte [] done1= serialize(done);
       DatagramPacket sendPacket = new DatagramPacket(done1, done1.length, IPAddress, port);
       socket.send(sendPacket);
    }

    @Override
    public void run() {
        byte[][] packets;
        try {
     
      
           long start =System.currentTimeMillis();
            packets = fileChunks(fileName);
            sendFileToClient(packets);
            long end=System.currentTimeMillis();
            System.out.println("time taken by the go back n to deliever a file "+(end-start));


    }   catch (IOException ex) {
            Logger.getLogger(GoBackN.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(GoBackN.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void SetPosition(LinkedList<Packet> windowPacket) {
        int i = 0;
        for (Packet p : windowPacket) {
            p.setPosition(i);
            i++;

            // System.out.println("Set Position   seq no " + p.getSeq() + "position no " + p.getPosition());
        }

    }

    public void PushToWindow(Queue<Packet> ReadyQueue, LinkedList<Packet> windowPacket) {
        Packet p;
       // System.out.println("simple.GoBackN.PushToWindow()");
        
        for (int i = 0; i < windowSize; i++) {
            p = ReadyQueue.poll();
            //  System.out.println("pakt : " + new String(p.getPayload()));
            p.setRetransmit(0);
            windowPacket.addLast(p);
        }

    }

    public void pushToQueue(byte[][] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (i == bytes.length - 1) {

                Packet p = new Packet(i, bytes[i], "Ready", "True");
                 p.setChecksum(CheckSum(p.getPayload()));
                ReadyQueue.add(p);
                //System.out.println("pakt : "+new String (p.getPayload()));
                write(p.getSeq(), p.getState());
            } else {
                Packet p = new Packet(i, bytes[i], "Ready", "False");
                 p.setChecksum(CheckSum(p.getPayload()));
                ReadyQueue.add(p);
                //System.out.println("pakt : "+new String (p.getPayload()));
                write(p.getSeq(), p.getState());
            }
        }
     //   System.out.println("pushToQueue() " + ReadyQueue.size());
//         while(ReadyQueue.size()!=0){
//             System.out.println("ready queue");
//         }
    }

    public void write(int seq, String s) {
        if (statesOfPackets.containsKey(seq)) {
            ArrayList<String> arrayList = statesOfPackets.get(seq);
            arrayList.add(s);
        } else {
            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add(s);
            statesOfPackets.put(seq, arrayList);
        }
    }

    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(obj);
        objectStream.flush();

        return byteStream.toByteArray();

    }

    public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        return (AckPacket) objectStream.readObject();
    }

    public static boolean isLoss(float probOfLoss) {
        Random rand = new Random();
        int random = rand.nextInt(10) + 1;
        System.out.println("random of data loss: " + random);
        if (random <= probOfLoss * 10) {

            //MSH 3ARFA MOMKEN NESTA5DEMHA F EH FL STOP AND WAIT
            // write(seq, State.Loss);
            //ap.setState("Loss");
            return true;
        } else {
            return false;
        }
    }

    public void SendPacket(Packet p) throws IOException {
        byte[] packet = serialize(p);
        DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, IPAddress, port);
        socket.send(sendPacket);
    }

    byte[][] fileChunks(String fileName) throws FileNotFoundException, IOException {
      //   System.out.println("file here" + fileName + "/");

        File file = new File(fileName); //3ayzin na5od esm el file mn el server aslan ykoon el client ba3etholo w n7oto f const el serverconnection
        FileInputStream in;
        in = new FileInputStream(file);
        byte[] byteBuf = new byte[512];
        byte[] fileToBeSend;
        int numBytesRead;
        int length1 = (int) file.length() / 512;
        int length2 = (int) file.length() % 512;
        int length = length1;
        if (length2 != 0) {
            length = length1 + 1;
        }
        byte[][] arr = new byte[length][512];
        int i, k, j, counter, counter2, var = 0;
        j = 0;
        int d = 0;
        int w=0;
        fileToBeSend = Files.readAllBytes(new File(fileName).toPath());
        counter = 0;
        while ((numBytesRead = in.read(byteBuf)) != -1) {
            k = j + numBytesRead;
            w = 0;
            for (i = j; i < k; i++) {

                arr[counter][w] = fileToBeSend[i];

                w++;

            }
            j = k;
            counter++;
        }
        
         return arr;
    }
        public static long CheckSum(byte[] buf) {
    int length = buf.length;
    int i = 0;

    long sum = 0;
    long data;

    // Handle all pairs
    while (length > 1) {
      data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
      sum += data;
      // 1's complement carry bit correction in 16-bits (detecting sign extension)
      if ((sum & 0xFFFF0000) > 0) {
        sum = sum & 0xFFFF;
        sum += 1;
      }

      i += 2;
      length -= 2;
    }

    // Handle remaining byte in odd length buffers
    if (length > 0) {
      sum += (buf[i] << 8 & 0xFF00);
      // 1's complement carry bit correction in 16-bits (detecting sign extension)
      if ((sum & 0xFFFF0000) > 0) {
        sum = sum & 0xFFFF;
        sum += 1;
      }
    }

    // Final 1's complement value correction to 16-bits
    sum = ~sum;
    sum = sum & 0xFFFF;
        System.out.println("CheckSum : "+sum);
    return sum;
    

  }
//          private static int CheckSum(byte[] message) {
//        int checksum = 0;
//        for (int i = 0; i < message.length; i++) {
//            checksum += message[i];
//        }
//        return checksum;
//    }
  public static boolean isCorrupted(float probOfCorrupt) {
        //get checksum from packet
        Random rand = new Random();
        int random = rand.nextInt(10) + 1;

        System.out.println("random  of corruption of ack  :" + random);
        if (random <= probOfCorrupt * 10) {
           // System.out.println("7asl corruption");
            //MSH 3ARFA MOMKEN NESTA5DEMHA F EH FL STOP AND WAIT
            // write(seq, State.Loss);
            //ap.setState("Loss");
            return true;
        } else {
            return false;

        }

    }

}
