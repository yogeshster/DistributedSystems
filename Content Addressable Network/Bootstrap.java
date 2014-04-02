
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bootstrap server
 * @author Yogesh Jagadeesan
 */

class Table
{
    String Name;
    InetAddress ipAddress;
    Integer port;
}
public class Bootstrap implements Runnable{

    
    ArrayList<Table> entries;
    HashMap<String,Integer> peerToEntry;
    DatagramSocket ds;
    DatagramPacket dp;
    Integer nodesNumber;
    byte[] buffer;
    
    
    /**
     * constructor initializes arguments
     */
    public Bootstrap()
    {
        buffer = new byte[20000];
        dp = new DatagramPacket(buffer, buffer.length);
        entries = new ArrayList();
        peerToEntry = new HashMap();
        nodesNumber = 1;
        try {
            ds = new DatagramSocket(53717, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
        } catch (SocketException | UnknownHostException ex) {
            Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * thread of execution that accepts packets from other nodes in CAN.
     */
    @Override
    public void run() {
        while(true)
        {
            try {
                ds.receive(dp);
                String information = new String(dp.getData(), 0, dp.getLength());
                String[] arguments = information.split("\\|");
                
                if(arguments[arguments.length-1].trim().equalsIgnoreCase("Join"))
                {
                    String response;
                    if(entries.size()<1)
                        response = "P"+nodesNumber.toString()+"|+None";
                    else
                    {
                        Integer random = (int)(Math.random()*(entries.size()));
                        response = new String("P"+nodesNumber.toString()+"|"+entries.get(random).Name+"|"+entries.get(random).ipAddress.toString().replace("/","")+"|"+entries.get(random).port.toString()+"|JoinResponse");
                    }
                    byte[] resp = response.getBytes();
                    ds.send(new DatagramPacket(resp,resp.length,dp.getAddress(),dp.getPort()));
                    nodesNumber++;
                }
                else if(arguments[arguments.length-1].trim().equalsIgnoreCase("Success"))
                {
                
                    for(Integer i=0; i<entries.size(); i++)
                    {
                        if(entries.get(i).ipAddress.toString().replace("/","").equals(arguments[1].replace("/","")))
                        {
                            entries.remove(entries.get(i));
                            break;
                        }
                    }
                    Integer start = 3;
                    if(arguments[3].equalsIgnoreCase("null"))
                        start+=3;
                    for(Integer i=0; i<2; i++)
                    {
                        Table entry = new Table();
                        entry.Name = arguments[start++].trim();
                        entry.ipAddress = InetAddress.getByName(arguments[start++].trim());
                        entry.port = Integer.parseInt(arguments[start++].trim());
                        entries.add(entry);
                        if(start<arguments.length &&  arguments[start].trim().equalsIgnoreCase("Success"))
                                break;
                    }
                }
                else if(arguments[0].equalsIgnoreCase("Removed"))
                {
                    Integer removalIndex = null;
                    for(Integer i=0; i<entries.size(); i++)
                    {
                        if(entries.get(i).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[1]) && entries.get(i).port.toString().equalsIgnoreCase(arguments[2]))
                        {
                            removalIndex = i;
                            break;
                        }
                    }
                    if(removalIndex != null)
                        entries.remove(entries.get(removalIndex));
                }
            } catch (IOException ex) {
                Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Execution begins here.
     * @param args not used
     */
    public static void main(String[] args) {
        // TODO code application logic here
        Bootstrap b = new Bootstrap();
        new Thread(b).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try
        {
            while(true)
            {
                String message = br.readLine();
                if(message.equalsIgnoreCase("display"))
                {
                    for(Integer i=0; i<b.entries.size(); i++)
                        System.out.println("IP: "+b.entries.get(i).ipAddress+" Port: "+b.entries.get(i).port);
                }
            }
        }
        catch(Exception e)
        {            
        }
    }
}
