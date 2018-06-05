package simple;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;


public class Server {

    public ArrayList<SRSender> connectionSR = new ArrayList<SRSender>();
    public ArrayList<StopAndWait> connectionSandW = new ArrayList<StopAndWait>();
    public ArrayList<GoBackN> connectionGBN = new ArrayList<GoBackN>();

    public Server() throws SocketException, IOException {

        DatagramSocket sGeneral = new DatagramSocket(3333);

        int ClientID = 0;
        while (true) {

            byte[] receiveData0 = new byte[1024];
            byte[] receiveData1 = new byte[1024];

            DatagramPacket recievePacket0 = new DatagramPacket(receiveData0, receiveData0.length);
            sGeneral.receive(recievePacket0);//esm el method
            String method = new String(receiveData0).trim();
            System.out.println("method name :" + method);

            DatagramPacket recievePacket1 = new DatagramPacket(receiveData1, receiveData1.length);
            sGeneral.receive(recievePacket1);//esm el file
            String fileName = new String(receiveData1).trim();
            System.out.println("file name :" + fileName);

            InetAddress IPAddress = recievePacket1.getAddress();
            int port = recievePacket1.getPort();
            System.out.println("client port number : " + port);
            int maxWindowSize = 4;
            int timeout = 2000;
              String extension=null;
            int x=fileName.lastIndexOf('.');
            if(x>0){
                
                extension=fileName.substring(x+1);
            }
            String clientFile;
            
            switch (method) {
                case "GobackN": {
                     clientFile=method+ClientID+"."+extension;
                    System.out.println("file to write in name bardo "+clientFile);
                    DatagramPacket sendPacket = new DatagramPacket(clientFile.getBytes(), clientFile.getBytes().length, IPAddress, port);
                    sGeneral.send(sendPacket);
                    System.out.println("Client ID : "+ClientID);
                    GoBackN Gb = new GoBackN(ClientID++, fileName, this, port, IPAddress, maxWindowSize, timeout);
                    Thread t1 = new Thread(Gb);
                    t1.start();
                    connectionGBN.add(Gb);
                    break;
                }
                case "SelectRepeat": {
                     clientFile=method+ClientID+"."+extension;
                    System.out.println("file to write in name "+clientFile);
                    DatagramPacket sendPacket = new DatagramPacket(clientFile.getBytes(), clientFile.getBytes().length, IPAddress, port);
                    sGeneral.send(sendPacket);
                    System.out.println("Client ID : "+ClientID);
                    SRSender SR = new SRSender(ClientID++, fileName, this, port, IPAddress, maxWindowSize, timeout);
                    Thread t1 = new Thread(SR);
                    t1.start();
                    connectionSR.add(SR);
                    break;
                }

                case "StopAndWait": {
                     clientFile=method+ClientID+"."+extension;
                    System.out.println("file to write in name "+clientFile);
                    DatagramPacket sendPacket = new DatagramPacket(clientFile.getBytes(), clientFile.getBytes().length, IPAddress, port);
                    sGeneral.send(sendPacket);
                    System.out.println("Client ID : "+ClientID);
                    StopAndWait sw = new StopAndWait(ClientID++, fileName, this, port, IPAddress, timeout);
                    Thread t1 = new Thread(sw);
                    t1.start();
                    connectionSandW.add(sw);
                    break;
                }
                default:
                    System.out.println("there is no method like that ");
                    break;

            }
        }
    }

    public static void main(String[] args) throws IOException {

        new Server();

    }
}
