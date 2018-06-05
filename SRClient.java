package simple;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import javax.imageio.ImageIO;
import static simple.GoBackNClient.concat;

class GoBackClient {

    private static String FILENAME ;
    public static int ACK=1;
    public static int NAK=0;
    public static int[] windows;
    public static int windowSize=4;
    public static int startWindow;
    public static void main(String args[]) throws Exception {
    float probabilityOfLossAck = (float) 0.01;
    float probOfCorrupt = (float) 0.01;

    HashMap<Integer, String> file = new HashMap<Integer, String>();   
        ArrayList<byte[]> image = new ArrayList<byte[]>();

        int seqPos = -1;
        int i=0;            
        DatagramSocket clientSocket = new DatagramSocket();
        while (true) {                      
            BufferedWriter bw = null;
            FileWriter fw = null;
            BufferedImage img = null;
                         byte[] Images = null;

            int f = 1;
            InetAddress IPAddress = InetAddress.getByName("localhost");

            byte[] fileSize = new byte[1000];
            int seq;
            long checksum;
            String data;
            byte[] receiveData = new byte[1000];
            String ff;

            System.out.println("enter method name :");
            Scanner s0 = new Scanner(System.in);
            String method = s0.nextLine();
            byte[] sendData0;
            //System.out.println("method before" +method+"/");

            method = method.trim();
            sendData0 = method.getBytes();
            DatagramPacket sendPacket0 = new DatagramPacket(sendData0, sendData0.length, IPAddress, 3333);
            clientSocket.send(sendPacket0);
            //System.out.println("method after" +new String (sendData0).trim());

            System.out.println("enter name of file :");
            Scanner s1 = new Scanner(System.in);
            String sentence = s1.nextLine();
            sentence = sentence.trim();
            byte[] sendData1;

            sendData1 = sentence.getBytes();
            DatagramPacket sendPacket1 = new DatagramPacket(sendData1, sendData1.length, IPAddress, 3333);
            clientSocket.send(sendPacket1);
byte[] receiveData0 = new byte[1000];
            DatagramPacket receivePacket0 = new DatagramPacket(receiveData0, receiveData0.length);
            clientSocket.receive(receivePacket0);

            String fileName;
           
            fileName = new String(receiveData0).trim();
            System.out.println("file name " + fileName);
           
            int x=sentence.lastIndexOf('.');
             String str=null;
            if(x>0){
                
               str=sentence.substring(0, x) ;
            }
            FILENAME = str + "_" + fileName;
            System.out.println("esm el file " + FILENAME);
            fw = new FileWriter(FILENAME);
            bw = new BufferedWriter(fw);
                     
            int ServerPort;   
            windows = new int[windowSize];
            Arrays.fill(windows, NAK);
            startWindow=0;

            while (f == 1) {

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                ServerPort = receivePacket.getPort();

                Packet p;
                p = (Packet) deserialize(receivePacket.getData());
               String modifiedSentence = new String(p.getPayload());//get the recieved data from the socket 
                if (modifiedSentence.equals("Done")) {
                    System.out.println("Done");
                        for(int j=0;j<file.size();j++)
                 {
                    bw.write(file.get(j));


                 }
                    f = 0;
                    bw.close();
                    break;
                  
                }
                seq = p.getSeq();

                int seqNak;
                checksum = CheckSum(p.getPayload());

                data = new String(p.getPayload());
                //System.out.println("da5l tany aho ");

                if (!isLoss(probabilityOfLossAck)) {
                    if (checksum == p.getChecksum()) {
                        if (isCorrupted(probOfCorrupt)) {
                            seqNak = -1;
                            System.out.println("seq no " + seq);
                            System.out.println("payload  " + new String(p.getPayload()));
                            AckPacket ap = new AckPacket("Ack", seqNak);
                            System.out.println("seq no of ack    " + ap.getSeqNo());
                            System.out.println();

                            byte[] sendAck = serialize(ap);
                            DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                            clientSocket.send(sendAcknowlege);

                        } else {
                            
                            System.out.println("seq no " + seq);
                            System.out.println("payload  " + new String(p.getPayload()));
                            AckPacket ap = new AckPacket("Ack", seq);
                            System.out.println("seq no of ack    " + ap.getSeqNo());
                            System.out.println();

                            byte[] sendAck = serialize(ap);
                            DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                            ackPacket(seq);
                            clientSocket.send(sendAcknowlege);
                            adjustWindow();
                        }
                        if (!file.containsKey(seq)) {
                            file.put(seq, data);
                        }
                    } else {
                        System.out.println(" corrupt of data : " + seq);
                        System.out.println();
                    }

                } else {
                    System.out.println("losss Ack : " + seq);
                    //System.out.println("simple.GoBackClient.main()");
                    System.out.println();
                }

               // System.out.println("5allas aho ");
                System.out.println();
            }

        }
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        return (Packet) objectStream.readObject();
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        o.flush();
        return b.toByteArray();
    }

    public static boolean isLoss(float probabilityOfLossAck) {
        Random rand = new Random();
        int random = rand.nextInt(10) + 1;
        int i = 0;
        System.out.println("random  of loss  of ack " + random);
        if (random <= probabilityOfLossAck * 10) {
            System.out.println("7asl loss");
            //MSH 3ARFA MOMKEN NESTA5DEMHA F EH FL STOP AND WAIT
            // write(seq, State.Loss);
            //ap.setState("Loss");
            return true;
        } else {
            return false;

        }
    }

    public static boolean isCorrupted(float probOfCorrupt) {
        //get checksum from packet
        Random rand = new Random();
        int random = rand.nextInt(10) + 1;

        System.out.println("random  of corruption of ack " + random);
        if (random <= probOfCorrupt * 10) {
            System.out.println("7asl corruption");
            // write(seq, State.Loss);
            //ap.setState("Loss");
            return true;
        } else {
            return false;

        }

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
    private static void adjustWindow() {
        //shift window
        while (true) {
            if (windows[0] == ACK) {
                for (int i = 0; i < windowSize - 1; i++) {
                    windows[i] = windows[i + 1];
                }
                windows[windowSize - 1] = NAK;
                startWindow++;
            } else {
                break;
            }
        }
        for(int i=0;i<windows.length;i++){
            System.out.println("window after adjust"+windows[i]+" i : "+i);
        }
    }

    private static void ackPacket(int seqNum) {
        if (startWindow <= seqNum) {
            if (seqNum - startWindow < windowSize) {
                windows[seqNum - startWindow] = ACK;
            }
        }
        System.out.println("windowStart "+startWindow+ "seq no  :"+seqNum);
        for(int i=0;i<windows.length;i++){
            System.out.println("window before adjust"+windows[i]+" i : "+i );
        }
        //  write(seqNum,"Ack");
    }
}
