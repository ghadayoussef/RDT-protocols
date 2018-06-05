
package simple;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import javax.imageio.ImageIO;
import static simple.StopAndWaitClient.concat;

class GoBackNClient {

    private static String FILENAME;

    public static void main(String args[]) throws Exception {
        float probabilityOfLossAck = (float) 0.01;
        float probOfCorrupt = (float) 0.01;
        int seqNak;
        ArrayList<byte[]> image = new ArrayList<byte[]>();


        HashMap<Integer, String> file = new HashMap<Integer, String>();
        int i = 0;
        int seqPos = -1;
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
             String str = null;
            if(x>0){
                
               str=sentence.substring(0, x) ;
            }
            FILENAME = str + "_" + fileName;
            System.out.println("esm el file " + FILENAME);
            fw = new FileWriter(FILENAME);
            bw = new BufferedWriter(fw);

            int ServerPort;
            while (f == 1) {

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                ServerPort = receivePacket.getPort();

                Packet p = new Packet();
                p = (Packet) deserialize(receivePacket.getData());

                String modifiedSentence = new String(p.getPayload());//get the recieved data from the socket 
                
                if (modifiedSentence.equals("Done")) {
                    //last packet to be received
                    System.out.println("last packet to be received");
                    
                for(int j=0;j<file.size();j++)
                 {
                    bw.write(file.get(j));

                 }
               
                    f = 0;
                    bw.close();
                    break;
                }
                seq = p.getSeq();
                seqNak = -1;
                checksum = CheckSum(p.getPayload());
                seqPos = p.getPosition();
                data = new String(p.getPayload());
                // System.out.println("da5l tany aho "+modifiedSentence);
//               
                if (seqPos == 0) {
                    //ack is not loss

                    if (!isLoss(probabilityOfLossAck)) { //checksum of the packet is not corrupted
                        if (checksum == p.getChecksum()) {
                            if (isCorrupted(probOfCorrupt)) {//ack is corrupted
                                
                               
                                System.out.println("seq no of packet " + seq);
                                //  System.out.println("payload  " + new String(p.getPayload()));
                                AckPacket ap = new AckPacket("Ack", seqNak);
                                System.out.println("seq no of ack    " + ap.getSeqNo());
                                System.out.println("7asal corruption fl ack ");
                                byte[] sendAck = serialize(ap);
                                DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                                clientSocket.send(sendAcknowlege);
                            } else {
                               
                                System.out.println("seq no of packet " + seq);
                                // System.out.println("payload  " + new String(p.getPayload()));
                                AckPacket ap = new AckPacket("Ack", seq);
                                System.out.println("seq no of ack    " + ap.getSeqNo());
                                System.out.println();
                                byte[] sendAck = serialize(ap);
                                DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                                clientSocket.send(sendAcknowlege);
                            }

                            if (!file.containsKey(seq)) {
                                file.put(seq, data);
                            }

                        } else {
                            
                            System.out.println(" corrupt of data with seq no : " + seq);
                            System.out.println();
                            continue;
                        }
                    } else {
                       
                        System.out.println("losss Ack : " + seq);
                        //System.out.println("simple.GoBackClient.main()");
                        System.out.println();
                        continue;
                    }
                } else if (seqPos != 0) {
                    if (!isLoss(probabilityOfLossAck)) { //checksum of the packet is not corrupted
                        if (checksum == p.getChecksum()) {
                            if (isCorrupted(probOfCorrupt)) {//ack is corrupted
                               
                                System.out.println("seq no of packet" + seq);
                                //System.out.println("payload  " + new String(p.getPayload()));
                                AckPacket ap = new AckPacket("Ack", seqNak);
                                System.out.println("seq no of ack    " + ap.getSeqNo());
                                System.out.println("7asal corruption fl ack");
                                byte[] sendAck = serialize(ap);
                                DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                                clientSocket.send(sendAcknowlege);
                            } else {
                                
                                System.out.println("seq no of packet" + seq);
                                //System.out.println("payload  " + new String(p.getPayload()));
                                AckPacket ap = new AckPacket("Ack", seq);
                                System.out.println("seq no of ack    " + ap.getSeqNo());
                                System.out.println();
                                byte[] sendAck = serialize(ap);
                                DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                                clientSocket.send(sendAcknowlege);
                            }

                            if (!file.containsKey(seq)) {
                                file.put(seq, data);
                            }

                        } else {
                            
                            System.out.println(" corrupt of data with seq no: " + seq);
                            System.out.println();
                            continue;
                        }
                    } else {
                       
                        System.out.println("losss Ack : " + seq);
                        //System.out.println("simple.GoBackClient.main()");
                        System.out.println();
                        continue;
                    }
                }
             
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

        System.out.println("random no  of loss of ack " + random);
        if (random <= probabilityOfLossAck * 10) {
            //  System.out.println("7asl loss");
            //MSH 3ARFA MOMKEN NESTA5DEMHA F EH FL STOP AND WAIT
            // write(seq, State.Loss);
            //ap.setState("Loss");
            return true;
        } else {
            return false;

        }
    }

//    private static int CheckSum(byte[] message) {
//        int checksum = 0;
//        for (int i = 0; i < message.length; i++) {
//            checksum += message[i];
//        }
//        return checksum;
//    }
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

    public static boolean isCorrupted(float probOfCorrupt) {
        //get checksum from packet
        Random rand = new Random();
        int random = rand.nextInt(10) + 1;

        System.out.println("random  of corruption of ack " + random);
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
      public static byte[] concat(ArrayList<byte[]> arrays) {

        // Determine the length of the result array
        int totalLength = 0;
        for (int i = 0; i < arrays.size(); i++) {
            totalLength += arrays.get(i).length;
        }

        // create the result array
        byte[] result = new byte[totalLength];
        // ByteBuffer bb = ByteBuffer.allocate(totalLength);

        // copy the source arrays into the result array
        int currentIndex = 0;
        for (int i = 0; i < arrays.size(); i++) {
            // bb.put(arrays.get(i));
            System.arraycopy(arrays.get(i), 0, result, currentIndex, arrays.get(i).length);
            currentIndex += arrays.get(i).length;
            //  System.out.println("array real "+arrays.get(i));
            // System.out.println("array aftr "+result[i]);
        }

        //  result = bb.array();
        return result;
    }

}
