package simple;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import javax.imageio.ImageIO;

class StopAndWaitClient {

    private static String FILENAME;

    public static void main(String args[]) throws Exception {

        float probabilityOfLossAck = (float) 0.3;
        float probOfCorrupt = (float) 0.2;
        int i = 0;
        int seqNo = -1;
        int seqNak = -1;
        int j = 0;
        byte[] Images = null;
        DatagramSocket clientSocket = new DatagramSocket();

        ArrayList<byte[]> image = new ArrayList<byte[]>();

        while (true) {

            BufferedWriter bw = null;
            FileWriter fw = null;
            BufferedImage img = null;
            int f = 1;

            byte[] receiveData = new byte[1000];

            InetAddress IPAddress = InetAddress.getByName("localhost");

            System.out.println("enter the method you want :");
            Scanner s0 = new Scanner(System.in);
            String method = s0.nextLine();

            byte[] sendData0;
            System.out.println("method :" + method);

            method = method.trim();
            sendData0 = method.getBytes();
            DatagramPacket sendPacket0 = new DatagramPacket(sendData0, sendData0.length, IPAddress, 3333);
            clientSocket.send(sendPacket0);

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

                System.out.println();

                int seq;
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                ServerPort = receivePacket.getPort();
                //      System.out.println("ana fel client w dah server port eli etft7 ll client :"+ServerPort);

                Packet p = new Packet();
                p = (Packet) deserialize(receivePacket.getData());
                String modifiedSentence = new String(p.getPayload());//get the recieved data from the socket 
                //System.out.println("payload "+p.getPayload());

                seq = p.getSeq();

                if (seqNo == seq) {
                    //packet mokarar
                    System.out.println("packet etba3at mokarar");

                    if (!isLoss(probabilityOfLossAck)) {
                        if (!isCorrupted(probOfCorrupt)) {
                            AckPacket ap = new AckPacket("Ack", seq);
                            System.out.println("seq no of ack :" + ap.getSeqNo());
                            byte[] sendAck = serialize(ap);
                            DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                            clientSocket.send(sendAcknowlege);

                        } else {

                            AckPacket ap = new AckPacket("Ack", -3);
                            System.out.println("seq no of ack    " + ap.getSeqNo());
                            byte[] sendAck = serialize(ap);
                            DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                            clientSocket.send(sendAcknowlege);
                            continue;
                        }

                    }
                } else {
                    System.out.println("packet gded ");
                    seqNo = seq;
                    image.add(p.getPayload());
                    if (sentence.contains("txt")) {
                        bw.write(new String(p.getPayload()).trim());
                    }
                      if (isLoss(probabilityOfLossAck)) {
                        if (CheckSum(p.getPayload()) == p.getChecksum()) {
                        }

                        continue;

                    } else//ack is not loss
                    {
                        if (CheckSum(p.getPayload()) == p.getChecksum()) {
                            if (!isCorrupted(probOfCorrupt)) {
                                AckPacket ap = new AckPacket("Ack", seq);
                                System.out.println("seq no of ack : " + ap.getSeqNo());
                                byte[] sendAck = serialize(ap);
                                DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                                clientSocket.send(sendAcknowlege);
//                             image.add(p.getPayload());
//                        bw.write(new String(p.getPayload()));
                            } else {
                                AckPacket ap = new AckPacket("Ack", -3);
                                byte[] sendAck = serialize(ap);
                                DatagramPacket sendAcknowlege = new DatagramPacket(sendAck, sendAck.length, IPAddress, ServerPort);
                                System.out.println("seq no of ack :" + ap.getSeqNo());
                                clientSocket.send(sendAcknowlege);
//                             image.add(p.getPayload());
//                             bw.write(new String(p.getPayload()));
                                continue;
                            }

                        } else {
                            continue;
                        }
                    }
                }

                    
                    //   System.out.println(image);
                    //packet gdidA 

                  
                if ("True".equals(p.getFlag())) {
                    if (sentence.contains("jpg")) {
                        Images = concat(image);
                        img = ImageIO.read(new ByteArrayInputStream(Images));
                        ImageIO.write(img, "jpg", new File(FILENAME));
                    } else {
                        bw.close();
                    }

                    f = 0;

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
        System.out.println("random of ack loss: " + random);
        if (random <= probabilityOfLossAck * 10) {

            return true;
        } else {
            return false;

        }
    }

//    public static int CheckSum(byte[] message) {
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
        int i = 0;
        System.out.println("random  of ack corruption " + random);
        if (random <= probOfCorrupt * 10) {
            //    System.out.println("corruption of ack packet ");

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

