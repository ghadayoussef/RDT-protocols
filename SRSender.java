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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SRSender implements Serializable, Runnable {

    int ClientId;
    String fileName;
    Server server;
    int port;
    InetAddress IPAddress;
    private float probOfLoss = (float) 0.01;
    private float probOfCorrupt = (float) 0.01;
    //private Queue<Packet> queue = new LinkedList(99999); // the large queue for message
    private Queue<Packet> ReadyQueue = new LinkedList<Packet>();
    private HashMap<Integer, Boolean> AckMap = new HashMap<Integer, Boolean>();
    private LinkedList<Packet> windowPacket = new LinkedList<Packet>();
    private int MaxWindowSize = 1000;
    private int windowSize;
    private int timeOut;
    private int Seq = 0; // the seq number of packet
    public boolean isBlock; // if the current packet is sending
    private HashMap<Integer, Boolean> mapOfAck = new HashMap<Integer, Boolean>();
    public HashMap<Integer, ArrayList<String>> statesOfPackets = new HashMap<Integer, ArrayList<String>>();//the queue have all packet with different state
    private DatagramSocket socket; // the socket
    private Timer timeoutTimer; // the timer to schedule timeout
    private final int PACKET_SIZE = 512;//the size of packet
    private int windows[]; // the windows to get the feedback from client array of boolean ack and nak
    private final int ACK = 1; // ack
    private final int NAK = 0; // nak
    private int numberOfTimeouts; // the number of timeouts

    public SRSender(int ClientID, String fileName, Server server, int port, InetAddress IPAddress, int MaxWindowSize, int timeOut) throws SocketException {
        this.socket = new DatagramSocket();
        this.port = port;
        this.IPAddress = IPAddress;
        this.ClientId = ClientID;
        this.fileName = fileName;
        this.server = server;
        this.timeOut = timeOut;
        this.MaxWindowSize = MaxWindowSize;
    }

    private SRSender() {
    }

    public void SendData(byte[][] packets) throws Exception {
        pushToQueue(packets);
        isBlock = true; // in transmission, block all traffic
        numberOfTimeouts = 0; // times of timeouts
        timeoutTimer = new Timer(true); // sent timer
        windowSize = 0; //size of windows
        while (true) {
            if (ReadyQueue.isEmpty() && windowSize == 0) {
                isBlock = false;//Block is false when we are not sending
                break;
            }
            if (windowSize == 0) { // if it is the first time to send
                isBlock = true;//we set Block to true if we are sending
                windowSize = Math.min(ReadyQueue.size(), MaxWindowSize);
                windows = new int[windowSize];
                Arrays.fill(windows, NAK);
                for (int i = 0; i < windowSize; i++) {
                    Packet p = ReadyQueue.poll();
                    if (isCorrupted(probOfCorrupt)) {
                        System.out.println("packet no" + p.getSeq() + "hena 7assal loss w corruption lel packet fl server");
                        p.setChecksum(-1);
                    }else
                        p.setChecksum(CheckSum(p.getPayload()));
                    
                    p.setState("Sent");
                    windowPacket.addLast(p);
                    write(p.getSeq(), p.getState());
                    SendPacket(p);
                }

            } else {
                isBlock = true;
                int emptySpace = adjustWindow();
                int[] newWindows = new int[windowSize];
                int ping = 0; // the variable to set windows
                //adjust list of sending windows
                for (int i = 0; i < emptySpace; i++) {
                    windowPacket.removeFirst();
                }
                // merge to new windows
                for (int i = emptySpace; i < windowSize; i++) {
                    newWindows[ping] = windows[i];
                    ping++;
                }
                // send new packet
                while (emptySpace-- != 0 && !ReadyQueue.isEmpty()) {
                    Packet p = ReadyQueue.poll();

                    windowPacket.addLast(p);
                    if (isCorrupted(probOfCorrupt)) {
                        System.out.println("packet no" + p.getSeq() + "hena 7assal corruption lel packet fl server");
                        p.setChecksum(-1);
                    }
                    else
                        p.setChecksum(CheckSum(p.getPayload()));
                    p.setState("Sent");
                    write(p.getSeq(), p.getState());
                    SendPacket(p);

                }
                // merge windows
                windows = newWindows;
                windowSize = windowPacket.size();
            }
            if (windowSize != 0) {
                isBlock = true;
                byte[] ackData = new byte[PACKET_SIZE];
                DatagramPacket AckPacket = new DatagramPacket(ackData, ackData.length);
                socket.receive(AckPacket);
                AckPacket ap = (AckPacket) deserialize(AckPacket.getData());

                ack(ap);
            } else {
                isBlock = false;
                windowSize = Math.min(ReadyQueue.size(), MaxWindowSize);
            }
        }
        System.out.println("Done " );
       Packet done=new Packet(-2, "Done".getBytes(),"Done","Done");
       byte [] done1= serialize(done);
       DatagramPacket sendPacket = new DatagramPacket(done1, done1.length, IPAddress, port);
       socket.send(sendPacket);
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
        System.out.println("pushToQueue() " + ReadyQueue.size());
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
        int i = 0;
        System.out.println("random  of corruption packet at servrer" + random);
        if (random <= probOfCorrupt * 10) {
            System.out.println("7asl corruption lel packet");
            //MSH 3ARFA MOMKEN NESTA5DEMHA F EH FL STOP AND WAIT
            // write(seq, State.Loss);
            //ap.setState("Loss");
            return true;
        } else {
            return false;

        }

    }

    public static boolean isLoss(float probOfLoss) {
        Random rand = new Random();
        int random = rand.nextInt(10) + 1;
        System.out.println("random  of loss packet at server " + random);
        if (random <= probOfLoss * 10) {
            System.out.println("7asl loss lel packet");
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
        if (!isLoss(probOfLoss)) {
            socket.send(sendPacket);
        } else {
            p.setState("Lost");
            System.out.println("packet no " + p.getSeq() + "is lost ");

        }

        timeoutTimer.schedule(new PacketTimeout(p), timeOut);
    }

    

    byte[][] fileChunks(String fileName) throws FileNotFoundException, IOException {
        System.out.println("file here" + fileName + "/");

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

    /**
     * the method will move the first nak in windows to the first position.
     *
     * @return the number of shifts
     * @throws Exception the exception
     */
    private int adjustWindow() throws Exception {
        int windowMoved = 0;
        for (int i = 0; i < windowSize; i++) {
            if (windows[i] == ACK) {
                windowMoved++;
            } else {
                break;
            }
        }
        return windowMoved;
    }

    private void ack(AckPacket ap) {
        for (int i = 0; i < windowSize; i++) {
            Packet p = windowPacket.get(i);
            if (ap.getSeqNo() == p.getSeq()) { // if it acked
                windows[i] = ACK;
                p.setAck(true);
                p.setState("ACKED");
                write(p.getSeq(), p.getState());
                System.out.println("seg no of ackkkkkk : " + p.getSeq());
            }

        }
    }

    @Override
    public void run() {
        byte[][] packets;
        try {
            
            long start =System.currentTimeMillis();
            packets = fileChunks(fileName);            
            SendData(packets);           
            long end=System.currentTimeMillis();
            System.out.println("time taken by selective repeat to deliever a file "+(end-start));


        } catch (IOException ex) {
            System.err.println("file does not existnnnnnnnnn");
            Logger.getLogger(StopAndWait.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(StopAndWait.class.getName()).log(Level.SEVERE, null, ex);
        }
        // this.socket.close();
    }

    private class PacketTimeout extends TimerTask {

        private Packet p;

        public PacketTimeout(Packet p) {
            this.p = p;
        }

        public void run() {
            //if packet has not been ACKed
            numberOfTimeouts++;
            try {
                if (!p.isAcked()) {
                    System.out.println("not acked packet no" + p.getSeq());
                    if (isCorrupted(probOfCorrupt)) {
                        p.setChecksum(-1);
                        System.out.println("packet no" + p.getSeq() + "hena 7assal corruption lel packet fl server fl retransmission ba3d el time out");
                    }else
                        p.setChecksum(CheckSum(p.getPayload()));

                    int r = p.getRetransmit() + 1;
                    p.setRetransmit(r);
                    p.setState("Retransmit");
                    write(p.getSeq(), p.getState());
                    SendPacket(p);

                }
            } catch (Exception e) {

            }
        }
    }

}
