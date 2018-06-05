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
import java.util.Random;


public class StopAndWait implements Runnable, Serializable {

    DatagramSocket socket;
    int ClientId;
    DatagramPacket recievedPacket;
    String fileName;
    Server server;
    int port;
    InetAddress IPAddress;
    private int timeout;
    private float probOfLoss = (float) 0.3;
    private float probOfCorrupt = (float) 0.2;

    public StopAndWait(int ClientID, String fileName, Server server, int port, InetAddress IPAddress, int timeout) throws SocketException {

        this.socket = new DatagramSocket();
        this.port = port;
        this.IPAddress = IPAddress;
        this.ClientId = ClientID;
        this.fileName = fileName;
        this.server = server;
        this.timeout = timeout;

    }

    void sendFileToClient(byte[][] packets) throws IOException, Exception {

        int seqack = -1;

        for (int i = 0; i < packets.length; i++) {
            
            System.out.println();
       //     System.out.println("packet in byte "+packets[i]);
            if (i == packets.length - 1) {
                byte[] recieveAck = new byte[1000];
                
                Packet p = new Packet(i, packets[i], "Sent", "True");
                p.setChecksum(CheckSum(p.getPayload()));
                if (isCorrupted(probOfCorrupt)) {
                    p.setChecksum(-1);
                }
                int seqpacket = p.getSeq();
                if (!isLoss(probOfLoss)) {
                    System.out.println("last packet is not lost (Seq no):" + seqpacket);
                    byte[] packet = serialize(p);
                    DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, IPAddress, port);
                    socket.send(sendPacket);
                }

                socket.setSoTimeout(timeout);

                try {

                    DatagramPacket receiveAckPacket = new DatagramPacket(recieveAck, recieveAck.length);
                    socket.receive(receiveAckPacket);

                    System.out.println("packet seq no received ack :" + p.getSeq());
                    AckPacket ap = new AckPacket();
                    ap = (AckPacket) deserialize(receiveAckPacket.getData());
                    seqack = ap.getSeqNo();
                    System.out.println("seq no of ack : " + seqack);
                    if (seqpacket == seqack) {
                        continue;
                    } else
                        i=i-1;

                } catch (SocketTimeoutException ste) {

                    System.out.println("socket time out for seq no of packet :" + p.getSeq());
                    i = i - 1;
                    System.out.println("i :" + i);

                }
            } //last packet eli fo2 
            else {
                byte[] recieveAck = new byte[1000];
                Packet p = new Packet(i, packets[i], "Sent", "False");
                p.setChecksum(CheckSum(p.getPayload()));
                int seqpacket = p.getSeq();
                if (isCorrupted(probOfCorrupt)) {
                    p.setChecksum(-1);
                }

                if (!isLoss(probOfLoss)) {
                    System.out.println("packet is not lost (Seq no):" + seqpacket);
                    byte[] packet = serialize(p);
                    DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, IPAddress, port);
                    socket.send(sendPacket);

                }

                socket.setSoTimeout(timeout);

                try {
                    
                    DatagramPacket receiveAckPacket = new DatagramPacket(recieveAck, recieveAck.length);
                    socket.receive(receiveAckPacket);
                    System.out.println("packet seq no received ack :" + p.getSeq());
                    AckPacket ap = new AckPacket();
                    ap = (AckPacket) deserialize(receiveAckPacket.getData());
                    seqack = ap.getSeqNo();
                    System.out.println("seq no of ack : " + seqack);
                    if (seqpacket == seqack) {
                        continue;
                    }
                    else
                        i=i-1;
       
                } catch (SocketTimeoutException ste) {
                 
                    System.out.println("socket time out for seq no of packet :" + p.getSeq());
                    i = i - 1;
                    System.out.println("i :" + i);

                }

            }

        }
    }

    public void run() {
        byte[][] packets;

        try {
            long start =System.currentTimeMillis();
            packets = fileChunks(fileName);
            sendFileToClient(packets);
            long end=System.currentTimeMillis();
            System.out.println("time taken by the stop and wait to deliever a file "+(end-start));
        } catch (IOException ex) {
            System.out.println("file doesn't exist ");
        } catch (Exception ex) {
        }

    }

    byte[][] fileChunks(String fileName) throws FileNotFoundException, IOException {

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
        int w;
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
        System.out.println("random of data packet :" + random);
        if (random <= probOfLoss * 10) {
      //      System.out.println(" loss in data ");
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

        Random rand = new Random();
        int random = rand.nextInt(10) + 1;
        System.out.println("random  of corruption packet :" + random);
        if (random <= probOfCorrupt * 10) {
         //   System.out.println("corruption in packet ");
            return true;
        } else {
            return false;

        }

    }

}

    
    

    

