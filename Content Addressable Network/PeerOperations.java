import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Operations of peer
 *
 * @author Yogesh Jagadeesan
 */


public class PeerOperations implements Runnable{
    
    Peer p;
    DatagramSocket ds;
    DatagramPacket dp;
    byte[] buffer;
    Peer nodeToJoin = null;
    Boolean suspendFlag;
    
    /**
     * Initializes neighbours, files and takeovers.
     */
    public PeerOperations()
    {
        p = new Peer();
        p.neighbours = new ArrayList();
        p.files = new ArrayList();
        p.takeovers = new ArrayList();
        buffer = new byte[20000];
        dp = new DatagramPacket(buffer, buffer.length);
        suspendFlag = false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            p.name = "Y";
            p.myAddress = (InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
            p.port = 53717;
            System.out.println("Enter ip of bootstrap: ");
            p.bootStrapIp = InetAddress.getByName(reader.readLine());
            p.bootStrapPort = 53717;
            p.randomX = (Math.random()*10);
            p.randomY = (Math.random()*10);
            ds = new DatagramSocket(p.port, p.myAddress);
            
        } catch (IOException ex) {
            Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Starting point for join operation
     * @param source    source address
     * @param port      source port
     */
    void BeginJoinOperations(InetAddress source, Integer port)
    {
        //send itself
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
            oos.flush();
            oos.writeObject((Object)p);
            oos.close();
            ds.send(new DatagramPacket(bos.toByteArray(), bos.toByteArray().length, source, port));
            
            //Update itself
            ds.receive(dp);
            ByteArrayInputStream bis = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));
            Peer updatedPeer = (Peer) ois.readObject();
            p.loX = updatedPeer.loX;
            p.loY = updatedPeer.loY;
            p.hiX = updatedPeer.hiX;
            p.hiY = updatedPeer.hiY;
            p.neighbours = new ArrayList(updatedPeer.neighbours);
            p.files = new ArrayList(updatedPeer.files);
            p.takeovers = new ArrayList(updatedPeer.takeovers);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Updates neighbours 
     * @param arguments message got from peer
     * @param nodeToJoin    other node.
     * @throws UnknownHostException 
     */
    void updateNeighbours(String[] arguments, Peer nodeToJoin) throws UnknownHostException
    {
        InetAddress removedIP = null;
        Integer removedPort = null;
        Double oldHiy = null;
        Double oldHix = null;
        Double oldLoy = null;
        Double oldLox = null;
        try
        {
            removedIP = InetAddress.getByName(arguments[2]);
            removedPort = Integer.parseInt(arguments[6]);
            oldHiy = Double.parseDouble(arguments[arguments.length-1]);
            oldHix = Double.parseDouble(arguments[arguments.length-2]);
            oldLoy = Double.parseDouble(arguments[arguments.length-3]);
            oldLox = Double.parseDouble(arguments[arguments.length-4]);
        }
        catch(Exception e)
        {
        }
                
        for(Integer i=0; i<p.neighbours.size(); i++)
        {

            if(p.neighbours.get(i).ipAddress.equals(removedIP) && p.neighbours.get(i).port.equals(removedPort) && p.neighbours.get(i).loX.equals(oldLox) && p.neighbours.get(i).loY.equals(oldLoy) && p.neighbours.get(i).hiX.equals(oldHix) && p.neighbours.get(i).hiY.equals(oldHiy))
            {
                p.neighbours.remove(p.neighbours.get(i));
                break;
            }
        }
        
        {
            if(((p.loX.doubleValue()==Double.parseDouble(arguments[9]) || p.hiX.doubleValue()==Double.parseDouble(arguments[7])) && p.hiY.doubleValue()!=Double.parseDouble(arguments[8]) && p.loY.doubleValue()!=Double.parseDouble(arguments[10])) || ((p.hiY.doubleValue()==Double.parseDouble(arguments[8]) || p.loY.doubleValue()==Double.parseDouble(arguments[10])) && p.hiX.doubleValue()!=Double.parseDouble(arguments[7]) && p.loX.doubleValue()!=Double.parseDouble(arguments[9])))
            {
                p.neighbours.add(new neighbour(Double.parseDouble(arguments[7]), Double.parseDouble(arguments[8]), Double.parseDouble(arguments[9]), Double.parseDouble(arguments[10]), InetAddress.getByName(arguments[5]), Integer.parseInt(arguments[6]), (arguments[4])));
                p.neighbours.get(p.neighbours.size()-1).takeovers = new ArrayList(nodeToJoin.takeovers);
            }
            if(((p.loX.doubleValue()==Double.parseDouble(arguments[16]) || p.hiX.doubleValue()==Double.parseDouble(arguments[14])) && p.hiY.doubleValue()!=Double.parseDouble(arguments[15]) && p.loY.doubleValue()!=Double.parseDouble(arguments[17])) || ((p.hiY.doubleValue()==Double.parseDouble(arguments[15]) || p.loY.doubleValue()==Double.parseDouble(arguments[17])) && p.hiX.doubleValue()!=Double.parseDouble(arguments[14]) && p.loX.doubleValue()!=Double.parseDouble(arguments[16])))
            {
                p.neighbours.add(new neighbour(Double.parseDouble(arguments[14]), Double.parseDouble(arguments[15]), Double.parseDouble(arguments[16]), Double.parseDouble(arguments[17]), InetAddress.getByName(arguments[12]), Integer.parseInt(arguments[13]), (arguments[11])));
            }
        }
        
    }
    
    /**
     * Checks if the random points are in the current region.
     * @param Rx    Random x coordinate.
     * @param Ry    Random y coordinate.
     * @return 
     */
    Boolean checkRegions(Double Rx, Double Ry)
    {
        if(Rx.doubleValue()>=p.loX.doubleValue() && Rx.doubleValue()<=p.hiX.doubleValue() && Ry.doubleValue()>=p.loY.doubleValue() && Ry.doubleValue()<=p.hiY.doubleValue())
            return true;
        for(Integer i=0; i<p.takeovers.size(); i++)
        {
            if(Rx.doubleValue()>=p.takeovers.get(i).loX.doubleValue() && Rx.doubleValue()<=p.takeovers.get(i).hiX.doubleValue() && Ry.doubleValue()>=p.takeovers.get(i).loY.doubleValue() && Ry.doubleValue()<=p.takeovers.get(i).hiY.doubleValue())
                return true;
        }
        return false;
    }
    
    /**
     * routes to nearest successor
     * @param message   message to be routed.
     * @param Rx        Random x coordinate.
     * @param Ry        Random y coordinate.
     */
    void routeToSuccessor(String message, Double Rx, Double Ry)
    {
        Double smallestXDiff = Double.MAX_VALUE;
        Double smallestYDiff = Double.MAX_VALUE;
        InetAddress routeNext = null;
        Integer portL = null;
        Double minDistance = Double.MAX_VALUE;
        for(Integer i=0; i<p.neighbours.size(); i++)
        {
            if(p.neighbours.get(i).loX.doubleValue()<=Rx.doubleValue() && p.neighbours.get(i).hiX.doubleValue()>=Rx.doubleValue() && p.neighbours.get(i).loY.doubleValue()<=Ry.doubleValue() && p.neighbours.get(i).hiY.doubleValue()>=Ry.doubleValue())
            {
                routeNext = p.neighbours.get(i).ipAddress;
                portL = p.neighbours.get(i).port;
                break;
            }
            Double midX = p.neighbours.get(i).loX+((p.neighbours.get(i).hiX - p.neighbours.get(i).loX)/2);
            Double midY = p.neighbours.get(i).loY+((p.neighbours.get(i).hiY - p.neighbours.get(i).loY)/2);
            Double currDistance = Math.sqrt(Math.pow((Rx.doubleValue()-midX.doubleValue()), 2)+Math.pow(Ry.doubleValue()-midY.doubleValue(), 2));
            if(currDistance.doubleValue()<minDistance.doubleValue())
            {
                minDistance = currDistance;
                routeNext = p.neighbours.get(i).ipAddress;
                portL = p.neighbours.get(i).port;
            }
            Boolean isInTOver = false;
            for(Integer j=0; j<p.neighbours.get(i).takeovers.size(); j++)
            {
                TakeOver tOver = p.neighbours.get(i).takeovers.get(j);
                midX = tOver.loX+((tOver.hiX - tOver.loX)/2);
                midY = tOver.loY+((tOver.hiY - tOver.loY)/2);
                currDistance = Math.sqrt(Math.pow((Rx.doubleValue()-midX.doubleValue()), 2)+Math.pow(Ry.doubleValue()-midY.doubleValue(), 2));
                if(tOver.loX.doubleValue()<=Rx.doubleValue() && tOver.hiX.doubleValue()>=Rx.doubleValue() && tOver.loY.doubleValue()<=Ry.doubleValue() && tOver.hiY.doubleValue()>=Ry.doubleValue())
                {
                    routeNext = p.neighbours.get(i).ipAddress;
                    portL = p.neighbours.get(i).port;
                    isInTOver = true;
                    break;
                }
                if(currDistance.doubleValue() < minDistance.doubleValue())
                {
                    minDistance = currDistance;
                    routeNext = p.neighbours.get(i).ipAddress;
                    portL = p.neighbours.get(i).port;
                }
            }
            if(isInTOver)
                break;
        }
        try {
                ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, routeNext, portL));
        } catch (IOException ex) {
            Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * When a node leaves, the neighbours are updated here.
     * @param arguments                 arguments extracted from the message
     * @throws UnknownHostException
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    void updationOfNeighboursFromLeave(String[] arguments) throws UnknownHostException, IOException, ClassNotFoundException
    {
        InetAddress OldipAddr = InetAddress.getByName(arguments[1]);
        Integer OldPort = Integer.parseInt(arguments[2]);
        Boolean presentFlag = false;
        Integer removalIndex=20;
        //requests for takeovers
        ds.send(new DatagramPacket("RequestingTakeOvers".getBytes(), "RequestingTakeOvers".getBytes().length, InetAddress.getByName(arguments[3]), Integer.parseInt(arguments[4])));
        ds.receive(dp);
                    
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));
        Peer updatedPeer = (Peer) ois.readObject();
        
        for(Integer i=0; i<p.neighbours.size(); i++)
        {
            if(p.neighbours.get(i).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[1]) && p.neighbours.get(i).port.toString().equals(arguments[2]))
                removalIndex = i;
            else if(p.neighbours.get(i).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[3]) && p.neighbours.get(i).port.toString().equalsIgnoreCase(arguments[4]))
            {
                presentFlag = true;
                p.neighbours.get(i).loX = Double.parseDouble(arguments[5].trim());
                p.neighbours.get(i).loY = Double.parseDouble(arguments[6].trim());
                p.neighbours.get(i).hiX = Double.parseDouble(arguments[7].trim());
                p.neighbours.get(i).hiY = Double.parseDouble(arguments[8].trim());
             }
         }
        
         if(removalIndex.intValue() < 20)
             p.neighbours.remove(p.neighbours.get(removalIndex));
         if(!presentFlag)
         {
            neighbour ne = new neighbour();
            ne.ipAddress = InetAddress.getByName(arguments[3]);
            ne.port = Integer.parseInt(arguments[4]);
            ne.loX = Double.parseDouble(arguments[5]);
            ne.loY = Double.parseDouble(arguments[6]);
            ne.hiX = Double.parseDouble(arguments[7]);
            ne.hiY = Double.parseDouble(arguments[8]);
                    
            ne.takeovers = new ArrayList(updatedPeer.takeovers);
            p.neighbours.add(ne);
         }
         
         
         //update takeovers' neighbours as well in hopes of it being acquired again by a peer.
         presentFlag = false;
         removalIndex = 20;
         for(Integer i=0; i<p.takeovers.size(); i++)
         {
             TakeOver o = p.takeovers.get(i);
             for(Integer j=0; j<o.neighbours.size(); j++)
             {
                if(o.neighbours.get(j).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[1]) && o.neighbours.get(j).port.toString().equals(arguments[2]))
                    removalIndex = j;
                else if(o.neighbours.get(j).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[3]) && o.neighbours.get(j).port.toString().equalsIgnoreCase(arguments[4]))
                {
                    presentFlag = true;
                    o.neighbours.get(j).loX = Double.parseDouble(arguments[5].trim());
                    o.neighbours.get(j).loY = Double.parseDouble(arguments[6].trim());
                    o.neighbours.get(j).hiX = Double.parseDouble(arguments[7].trim());
                    o.neighbours.get(j).hiY = Double.parseDouble(arguments[8].trim());
                }
             }
             if(removalIndex.intValue() < 20)
                o.neighbours.remove(o.neighbours.get(removalIndex));
             if(!presentFlag)
             {
                neighbour ne = new neighbour();
                ne.ipAddress = InetAddress.getByName(arguments[3]);
                ne.port = Integer.parseInt(arguments[4]);
                ne.loX = Double.parseDouble(arguments[5]);
                ne.loY = Double.parseDouble(arguments[6]);
                ne.hiX = Double.parseDouble(arguments[7]);
                ne.hiY = Double.parseDouble(arguments[8]);
                    
                ne.takeovers = new ArrayList(updatedPeer.takeovers);
                o.neighbours.add(ne);
             }
          }
         //send ack
         ds.send(new DatagramPacket("Ack".getBytes(), "Ack".getBytes().length, OldipAddr, OldPort));
    }
    
    /**
     * Take over a node and updates its neighbours and itself.
     * @param arguments                 arguments extracted from the message
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    void TakeOverAndUpdate(String[] arguments) throws IOException, ClassNotFoundException
    {
        ds.send(new DatagramPacket("RequestingYou".getBytes(), "RequestingYou".getBytes().length, dp.getAddress(), dp.getPort()));
        ds.receive(dp);
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));
        Peer updatedPeer = (Peer) ois.readObject();
                
        TakeOver to = new TakeOver();
        to.loX = updatedPeer.loX;
        to.loY = updatedPeer.loY;
        to.hiX = updatedPeer.hiX;
        to.hiY = updatedPeer.hiY;
        to.neighbours = new ArrayList(updatedPeer.neighbours);
        to.files = new ArrayList(updatedPeer.files);
        p.takeovers.add(to);
        
        //store takeovers as well
        for(Integer i=0; i<updatedPeer.takeovers.size(); i++)
            p.takeovers.add(updatedPeer.takeovers.get(i));
                
        
        //Update neighbour's takeovers
        for(Integer i=0; i<p.neighbours.size(); i++)
        {
            ds.send(new DatagramPacket("UpdateTakeover".getBytes(), "UpdateTakeover".getBytes().length, (p.neighbours.get(i).ipAddress), p.neighbours.get(i).port));
            ds.receive(dp);
            String mess = new String(buffer, dp.getOffset(), dp.getLength());
            if(mess.equalsIgnoreCase("Ack") || mess.length()==3)
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
                oos.flush();
                oos.writeObject((Object)p);
                oos.close();
                ds.send(new DatagramPacket(bos.toByteArray(), bos.toByteArray().length, p.neighbours.get(i).ipAddress, p.neighbours.get(i).port));
            }
        }

        //Update takeover's neighbours takeovers
        for(Integer i=0; i<p.takeovers.size(); i++)
        {
            for(Integer j=0; j<p.takeovers.get(i).neighbours.size(); j++)
            {
                ds.send(new DatagramPacket("UpdateTakeover".getBytes(), "UpdateTakeover".getBytes().length, (p.takeovers.get(i).neighbours.get(j).ipAddress), p.takeovers.get(i).neighbours.get(j).port));
                ds.receive(dp);
                String mess = new String(buffer, dp.getOffset(), dp.getLength());
                if(mess.equalsIgnoreCase("Ack"))
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
                    oos.flush();
                    oos.writeObject((Object)p);
                    oos.close();
                    ds.send(new DatagramPacket(bos.toByteArray(), bos.toByteArray().length, InetAddress.getByName(p.takeovers.get(i).neighbours.get(j).ipAddress.toString().replace("/", "")), p.takeovers.get(i).neighbours.get(j).port));
                }
            }
        }
        //remove node being taken over from list of neighbours
        for(Integer i=0; i<p.neighbours.size(); i++)
        {
            if(p.neighbours.get(i).ipAddress.equals(updatedPeer.myAddress) && p.neighbours.get(i).port.equals(updatedPeer.port))
            {
                p.neighbours.remove(p.neighbours.get(i));
                break;
            }
        }
        //send confirmation
        ds.send(new DatagramPacket("Ack".getBytes(), "Ack".getBytes().length, InetAddress.getByName(arguments[1]), Integer.parseInt(arguments[2])));
    }
    
    /**
     * Updates the current zone and its neighbours
     * @param arguments                 arguments extracted from the message.
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    void updateZoneAndNeighbours(String[] arguments) throws IOException, ClassNotFoundException
    {
        //request for the the leaving neighbour's details
        //suspendFlag = true;
        Peer updatedPeer = null;
        ds.send(new DatagramPacket("RequestingYourNeighbours".getBytes(), "RequestingYourNeighbours".getBytes().length, InetAddress.getByName(arguments[5]), Integer.parseInt(arguments[6])));
        ds.receive(dp);
       
            ByteArrayInputStream bis = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));
            updatedPeer = (Peer) ois.readObject();
        
        
        HashMap<String, String> presentNeighbours = new HashMap();
                    
        Integer removalIndex = 20;
        for(Integer i=0; i<p.neighbours.size(); i++)
        {
            presentNeighbours.put(p.neighbours.get(i).ipAddress.toString().replace("/", "")+"|"+p.neighbours.get(i).port.toString(), p.neighbours.get(i).ipAddress.toString().replace("/", "")+"|"+p.neighbours.get(i).port.toString());
            if(!(p.neighbours.get(i).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[5]) && p.neighbours.get(i).port.toString().equalsIgnoreCase(arguments[6])))
            {
                String mess = "UpdateYourNeighbours|"+p.myAddress.toString().replace("/", "")+"|"+p.port+"|"+p.loX.toString()+"|"+p.loY.toString()+"|"+p.hiX.toString()+"|"+p.hiY.toString()+"|"+arguments[1].trim()+"|"+arguments[2].trim()+"|"+arguments[3].trim()+"|"+arguments[4].trim();
                ds.send(new DatagramPacket(mess.getBytes(), mess.getBytes().length, p.neighbours.get(i).ipAddress, p.neighbours.get(i).port));
                ds.receive(dp);
                
                String temp = new String(buffer, dp.getOffset(), dp.getLength());
            }
            else
                removalIndex = i;
         }
         if(removalIndex<20)
            p.neighbours.remove(p.neighbours.get(removalIndex));
         
         p.loX = Double.parseDouble(arguments[1].trim());
         p.loY = Double.parseDouble(arguments[2].trim());
         p.hiX = Double.parseDouble(arguments[3].trim());
         p.hiY = Double.parseDouble(arguments[4].trim());
                    
         //Update this peer's neighbours
         for(Integer i=0; i<updatedPeer.neighbours.size(); i++)
         {
            Double lx = updatedPeer.neighbours.get(i).loX;
            Double ly = updatedPeer.neighbours.get(i).loY;
            Double hx = updatedPeer.neighbours.get(i).hiX;
            Double hy = updatedPeer.neighbours.get(i).hiY;
            String ip = updatedPeer.neighbours.get(i).ipAddress.toString().replace("/", "");
            Integer port = updatedPeer.neighbours.get(i).port;
            String key = updatedPeer.neighbours.get(i).ipAddress.toString().replace("/", "")+"|"+updatedPeer.neighbours.get(i).port.toString();
            if(((p.loX.equals(hx) || p.hiX.equals(lx)) && (!p.hiY.equals(ly)) && (!p.loY.equals(hy))) || ((p.hiY.equals(ly) || p.loY.equals(hy)) && (!p.hiX.equals(lx)) && (!p.loX.equals(hx))))
            {
            	if((!presentNeighbours.containsKey(key) && (((((!(ip.equalsIgnoreCase(p.myAddress.toString().replace("/", "")) && p.port.equals(port)))))) || (((ip.equalsIgnoreCase(p.myAddress.toString().replace("/", ""))) && (!p.port.equals(port)))))))
                {
                    p.neighbours.add(new neighbour(updatedPeer.neighbours.get(i).loX, updatedPeer.neighbours.get(i).loY, updatedPeer.neighbours.get(i).hiX, updatedPeer.neighbours.get(i).hiY, updatedPeer.neighbours.get(i).ipAddress, updatedPeer.neighbours.get(i).port, updatedPeer.neighbours.get(i).name));
                    presentNeighbours.put(key, key);
                }
            }
        }
         
        //Update hash tables
        for(Integer i=0; i<updatedPeer.files.size(); i++)
            p.files.add(updatedPeer.files.get(i));
        //suspendFlag = false;
        ds.send(new DatagramPacket("Ack".getBytes(), "Ack".getBytes().length, InetAddress.getByName(arguments[5]), Integer.parseInt(arguments[6])));
    }
    
    /**
     * Thread of execution that accepts messages from other nodes.
     */
    @Override
    public void run() 
    {
        while(true)
        {
            try
            {
                String message = "|Do Nothing|";
                if(!suspendFlag)
                {
                    ds.receive(dp);
                    message = new String(buffer, dp.getOffset(), dp.getLength());
                }
                else
                    continue;
                
                String[] arguments = message.split("\\|");
                if(arguments[0].trim().equalsIgnoreCase("Forward"))
                {
                    //check its own coordinates and also its takeovers
                    Double Rx = new Double(Double.parseDouble(arguments[1]));
                    Double Ry = new Double(Double.parseDouble(arguments[2]));
                    Boolean ispNode = checkRegions(Rx, Ry);
                    if(ispNode)
                        BeginJoinOperations(InetAddress.getByName(arguments[3]), Integer.parseInt(arguments[4]));
                    else    //use greedy to forward to next neighbour
                            routeToSuccessor(message, Rx, Ry);
                }
                else if(arguments[0].trim().equalsIgnoreCase("UpdateTakeover"))
                {
                    ds.send(new DatagramPacket("Ack".getBytes(), "Ack".getBytes().length, dp.getAddress(), dp.getPort()));
                    //receive node with takeovers
                    ds.receive(dp);
                    Peer updatedPeer=null;
                    ByteArrayInputStream bis = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
                    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));
                    updatedPeer = (Peer) ois.readObject();
                
                    for(Integer i=0; i<p.neighbours.size(); i++)
                    {
                        if(p.neighbours.get(i).ipAddress.equals(updatedPeer.myAddress) && p.neighbours.get(i).port.equals(updatedPeer.port) && p.neighbours.get(i).loX.equals(updatedPeer.loX) && p.neighbours.get(i).loY.equals(updatedPeer.loY) && p.neighbours.get(i).hiX.equals(updatedPeer.hiX) && p.neighbours.get(i).hiY.equals(updatedPeer.hiY))
                        {
                            p.neighbours.get(i).takeovers = new ArrayList(updatedPeer.takeovers);
                        }
                    }
                }
                else if(arguments[0].trim().equalsIgnoreCase("TakeMeOver"))
                    TakeOverAndUpdate(arguments);
                else if(arguments[0].trim().equalsIgnoreCase("Removed"))
                {
                    String takeOverMessage = "RequestingTakeOvers";
                    ds.send(new DatagramPacket(takeOverMessage.getBytes(), takeOverMessage.getBytes().length, dp.getAddress(), dp.getPort()));
                    ds.receive(dp);
                    ByteArrayInputStream bis = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
                    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));
                    Peer updatedPeer = (Peer) ois.readObject();
                    updateNeighbours(arguments, updatedPeer);
                    ds.send(new DatagramPacket("Done".getBytes(), "Done".getBytes().length, dp.getAddress(), dp.getPort()));
                }
                else if(arguments[0].trim().equalsIgnoreCase("RemoveNeighboursTakeOver"))  //A takeover has been granted to a peer
                {
                    for(Integer i=0; i<p.neighbours.size(); i++)
                    {
                        for(Integer j=0; j<p.neighbours.get(i).takeovers.size(); j++)
                        {
                            if(p.neighbours.get(i).takeovers.get(j).loX.equals(Double.parseDouble(arguments[1])) && p.neighbours.get(i).takeovers.get(j).loY.equals(Double.parseDouble(arguments[2])) && p.neighbours.get(i).takeovers.get(j).hiX.equals(Double.parseDouble(arguments[3])) && p.neighbours.get(i).takeovers.get(j).hiY.equals(Double.parseDouble(arguments[4])))
                            {
                                p.neighbours.get(i).takeovers.remove(p.neighbours.get(i).takeovers.get(j));
                                break;
                            }
                        }
                    }
                }
                else if(arguments[0].trim().equalsIgnoreCase("Update"))
                {
                    Boolean alreadyPresent = false;
                    Integer removalIndex = 20;
                    for(Integer i=0; i<p.neighbours.size(); i++)
                    {
                        if(p.neighbours.get(i).ipAddress.equals(InetAddress.getByName(arguments[1])) && p.neighbours.get(i).port.equals(Integer.parseInt(arguments[12])))
                            removalIndex = i;
                        else if(p.neighbours.get(i).ipAddress.equals(InetAddress.getByName(arguments[6])) && p.neighbours.get(i).port.equals(Integer.parseInt(arguments[7])))
                            alreadyPresent = true;
                    }
                    if(removalIndex < 20)
                        p.neighbours.remove(p.neighbours.get(removalIndex));
                    if(!alreadyPresent)
                        p.neighbours.add(new neighbour(Double.parseDouble(arguments[8]), Double.parseDouble(arguments[9]), Double.parseDouble(arguments[10]), Double.parseDouble(arguments[11]), InetAddress.getByName(arguments[6]), Integer.parseInt(arguments[7]), ""));
                    
                    //update takeover's neighbours as well in hopes of it getting acquired by a new peer.                   
                    for(Integer i=0; i<p.takeovers.size(); i++)
                    {
                        removalIndex = 20;
                        alreadyPresent = false;
                        TakeOver tOver = p.takeovers.get(i);
                        for(Integer j=0; j<tOver.neighbours.size(); j++)
                        {
                            if(tOver.neighbours.get(j).ipAddress.equals(InetAddress.getByName(arguments[1])) && tOver.neighbours.get(j).port.equals(Integer.parseInt(arguments[12])))
                                removalIndex = j;
                            else if(tOver.neighbours.get(j).ipAddress.equals(InetAddress.getByName(arguments[6])) && tOver.neighbours.get(j).port.equals(Integer.parseInt(arguments[7])))
                                alreadyPresent = true;
                        }
                        if(removalIndex<20)
                            p.takeovers.get(i).neighbours.remove(p.takeovers.get(i).neighbours.get(removalIndex));
                        if(!alreadyPresent)
                            p.takeovers.get(i).neighbours.add(new neighbour(Double.parseDouble(arguments[8]), Double.parseDouble(arguments[9]), Double.parseDouble(arguments[10]), Double.parseDouble(arguments[11]), InetAddress.getByName(arguments[6]), Integer.parseInt(arguments[7]), ""));
                    }
                }
                else if(arguments[0].trim().equalsIgnoreCase("Replace"))
                {
                    p.neighbours.add(new neighbour(Double.parseDouble(arguments[9]), Double.parseDouble(arguments[10]), Double.parseDouble(arguments[11]), Double.parseDouble(arguments[12]), InetAddress.getByName(arguments[7]), Integer.parseInt(arguments[8]), ""));
                    Integer removalIndex = Integer.MAX_VALUE;
                    for(Integer i=0; i<(p.neighbours.size()-1); i++)
                    {
                        InetAddress addr = InetAddress.getByName(arguments[1].trim());
                        Integer port = Integer.parseInt(arguments[2].trim());
                        if((addr.equals(InetAddress.getByName(arguments[1])) && (port.intValue()==Integer.parseInt(arguments[2]))))
                        {
                            Double lx = p.neighbours.get(i).loX;
                            Double ly = p.neighbours.get(i).loY;
                            Double hx = p.neighbours.get(i).hiX;
                            Double hy = p.neighbours.get(i).hiY;
                            if((!((p.loX.equals(hx) || p.hiX.equals(lx)) && (!p.hiY.equals(ly)) && (!p.loY.equals(hy))) || ((p.hiY.equals(ly) || p.loY.equals(hy)) && (!p.hiX.equals(lx)) && (!p.loX.equals(hx)))))
                            {
                                removalIndex = i;
                                break;
                            }    
                        }
                    }
                    if(removalIndex < Integer.MAX_VALUE)
                        p.neighbours.remove(p.neighbours.get(removalIndex));
                }
                else if(arguments[0].trim().equalsIgnoreCase("Leave"))
                    updationOfNeighboursFromLeave(arguments);
                else if(arguments[0].trim().equalsIgnoreCase("UpdateZoneAndNeighbours"))
                    updateZoneAndNeighbours(arguments);
                else if(arguments[0].trim().equalsIgnoreCase("UpdateYourNeighbours"))
                {
                    //update my neighbours
                    for(Integer i=0; i<p.neighbours.size(); i++)
                    {
                        if(p.neighbours.get(i).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[1].trim()) && p.neighbours.get(i).port.toString().equalsIgnoreCase(arguments[2].trim()) && p.neighbours.get(i).loX.toString().equalsIgnoreCase(arguments[3].trim()) && p.neighbours.get(i).loY.toString().equalsIgnoreCase(arguments[4].trim()) && p.neighbours.get(i).hiX.toString().equalsIgnoreCase(arguments[5].trim()) && p.neighbours.get(i).hiY.toString().equalsIgnoreCase(arguments[6].trim()))
                        {
                            p.neighbours.get(i).loX = Double.parseDouble(arguments[7]);
                            p.neighbours.get(i).loY = Double.parseDouble(arguments[8]);
                            p.neighbours.get(i).hiX = Double.parseDouble(arguments[9]);
                            p.neighbours.get(i).hiY = Double.parseDouble(arguments[10]);
                        }
                    }
                    
                    //update my takeovers' neighbours
                    for(Integer i=0; i<p.takeovers.size(); i++)
                    {
                        TakeOver tOver = p.takeovers.get(i);
                        for(Integer j=0; j<tOver.neighbours.size(); j++)
                        {
                            if(tOver.neighbours.get(j).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[1].trim()) && tOver.neighbours.get(j).port.toString().equalsIgnoreCase(arguments[2].trim()) && tOver.neighbours.get(j).loX.toString().equalsIgnoreCase(arguments[3].trim()) && tOver.neighbours.get(j).loY.toString().equalsIgnoreCase(arguments[4].trim()) && tOver.neighbours.get(j).hiX.toString().equalsIgnoreCase(arguments[5].trim()) && tOver.neighbours.get(j).hiY.toString().equalsIgnoreCase(arguments[6].trim()))
                            {
                                tOver.neighbours.get(j).loX = Double.parseDouble(arguments[7]);
                                tOver.neighbours.get(j).loY = Double.parseDouble(arguments[8]);
                                tOver.neighbours.get(j).hiX = Double.parseDouble(arguments[9]);
                                tOver.neighbours.get(j).hiY = Double.parseDouble(arguments[10]);
                            }
                        }
                    }
                    ds.send(new DatagramPacket("Ack".getBytes(), "Ack".getBytes().length, dp.getAddress(), dp.getPort()));
                }
                else if(arguments[0].trim().equalsIgnoreCase("RequestingYou") || arguments[0].trim().equalsIgnoreCase("RequestingTakeOvers") || (arguments[0].trim().equalsIgnoreCase("RequestingYourNeighbours")))
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
                    oos.flush();
                    oos.writeObject((Object)p);
                    oos.close();
                    ds.send(new DatagramPacket(bos.toByteArray(), bos.toByteArray().length, dp.getAddress(), dp.getPort()));
                }
                else if(arguments[0].trim().equalsIgnoreCase("LeaveNetwork"))
                {
                    LeaveNetwork();
                    break;
                }
                else if((arguments[0].trim().equalsIgnoreCase("insert") || arguments[0].trim().equalsIgnoreCase("search")))
                        forwardOrInsertWord(message, arguments);
                else if(message.contains("insert") || message.contains("search"))
                {
                    String [] temp = message.split(" ");
                    insertOrSearchKeyword(temp[1], temp[0]);
                }
            }
            catch(Exception e)
            {
            }
        }
    }
    
    /**
     * Gets the word and either forwards it to the successive neighbour or inserts it to itself.
     * @param message       contains the message to be forwarded.
     * @param arguments     arguments extracted from the message to be processed.
     */
    void forwardOrInsertWord(String message, String[] arguments)
    {
        message += "|"+p.myAddress.toString().replace("/", "")+"|"+p.port.toString();
        if(attemptInsertOrSearchInCurrentZone(arguments[1], arguments[0]))
        {
            if(arguments[0].equalsIgnoreCase("search"))
            {
                Boolean found = false;
                for(Integer i=0; i<p.files.size(); i++)
                {
                    if(arguments[1].equalsIgnoreCase(p.files.get(i)))
                    {
                        message += "|Success";
                        found = true;
                        break;
                    }
                }
                if(!found)
                {
                    for(Integer i=0; i<p.takeovers.size(); i++)
                    {
                        Boolean innerFound = false;
                        for(Integer j=0; j<p.takeovers.get(i).files.size(); j++)
                        {
                            if(arguments[1].equalsIgnoreCase(p.takeovers.get(i).files.get(j)))
                            {
                                message += "|Success";
                                innerFound = true;
                                break;
                            }
                        }
                        if(innerFound)
                            break;
                    }    
                }    
            }
            else
                message += "|Success";
            if(!message.contains("Success"))
                message += "|Failure";
            try {
                ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(arguments[2]), Integer.parseInt(arguments[3])));
            } catch (UnknownHostException ex) {
                Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            InetAddress routeNext = null;
            Integer portL = null;
            Double Rx = computeXOrY(arguments[1], true);
            Double Ry = computeXOrY(arguments[1], false);
            Double minDistance = Double.MAX_VALUE;
            for(Integer i=0; i<p.neighbours.size(); i++)
            {
                if(p.neighbours.get(i).ipAddress.equals(dp.getAddress()) && p.neighbours.get(i).port.equals(dp.getPort()))
                    continue;
                if(p.neighbours.get(i).loX.doubleValue()<=Rx.doubleValue() && p.neighbours.get(i).hiX.doubleValue()>Rx.doubleValue() && p.neighbours.get(i).loY.doubleValue()<=Ry.doubleValue() && p.neighbours.get(i).hiY.doubleValue()>Ry.doubleValue())
                {
                    routeNext = p.neighbours.get(i).ipAddress;
                    portL = p.neighbours.get(i).port;
                    break;
                }
                Double midX = p.neighbours.get(i).loX+((p.neighbours.get(i).hiX - p.neighbours.get(i).loX)/2);
                Double midY = p.neighbours.get(i).loY+((p.neighbours.get(i).hiY - p.neighbours.get(i).loY)/2);
                Double currDistance = Math.sqrt(Math.pow((Rx.doubleValue()-midX.doubleValue()), 2)+Math.pow(Ry.doubleValue()-midY.doubleValue(), 2));
                if(currDistance.doubleValue()<minDistance.doubleValue())
                {
                    minDistance = currDistance;
                    routeNext = p.neighbours.get(i).ipAddress;
                    portL = p.neighbours.get(i).port;
                }
                Boolean isInTOver = false;
                for(Integer j=0; j<p.neighbours.get(i).takeovers.size(); j++)
                {
                    TakeOver tOver = p.neighbours.get(i).takeovers.get(j);
                    midX = tOver.loX+((tOver.hiX - tOver.loX)/2);
                    midY = tOver.loY+((tOver.hiY - tOver.loY)/2);
                    currDistance = Math.sqrt(Math.pow((Rx.doubleValue()-midX.doubleValue()), 2)+Math.pow(Ry.doubleValue()-midY.doubleValue(), 2));
                    if(tOver.loX.doubleValue()<=Rx.doubleValue() && tOver.hiX.doubleValue()>Rx.doubleValue() && tOver.loY.doubleValue()<=Ry.doubleValue() && tOver.hiY.doubleValue()>Ry.doubleValue())
                    {
                        routeNext = p.neighbours.get(i).ipAddress;
                        portL = p.neighbours.get(i).port;
                        isInTOver = true;
                        break;
                    }
                    if(currDistance.doubleValue() < minDistance.doubleValue())
                    {
                        minDistance = currDistance;
                        routeNext = p.neighbours.get(i).ipAddress;
                        portL = p.neighbours.get(i).port;
                    }
                }
                if(isInTOver)
                    break;
            }
            try {
                ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, routeNext, portL));
            } catch (UnknownHostException ex) {
                Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Computes x or y coordinates given a word.
     * @param word  word from which the calculation is to be done.
     * @param odd   whether the odd positions or the even positions of the word is to be used for calculation.
     * @return 
     */
    Double computeXOrY(String word, Boolean odd)
    {
        Double sum = 0.0;
        Integer idx = 0;
        if(odd)
            idx++;
        for(;idx<word.length(); idx+=2)
            sum += (int)word.charAt(idx);
        return sum%10;
    }
    
    /**
     * Checks if word is in the current zone
     * @param word  word to be checked.
     * @param loX   lower x coordinate of the zone
     * @param loY   lower y coordinate of the zone
     * @param hiX   higher x coordinate of the zone
     * @param hiY   higher y coordinate of the zone
     * @return 
     */
    Boolean wordInZone(String word, Double loX, Double loY, Double hiX, Double hiY)
    {
        Double x = computeXOrY(word, true);
        Double y = computeXOrY(word, false);
        if(x.doubleValue()>=loX.doubleValue() && x.doubleValue()<hiX.doubleValue() && y.doubleValue()>=loY.doubleValue() && y.doubleValue()<hiY.doubleValue())
            return true;
        return false;
    }
    
    /**
     * Attempts search or insertion of word in the current zone
     * @param word          word to be inserted
     * @param operation     operation to be performed. Whether insertion or search.
     * @return 
     */
    Boolean attemptInsertOrSearchInCurrentZone(String word, String operation)
    {
        if(wordInZone(word, p.loX, p.loY, p.hiX, p.hiY))
        {
            if(operation.equalsIgnoreCase("insert"))
                p.files.add(word);
            return true;
        }
        for(Integer i=0; i<p.takeovers.size(); i++)
        {
            Double loX = p.takeovers.get(i).loX;
            Double loY = p.takeovers.get(i).loY;
            Double hiX = p.takeovers.get(i).hiX;
            Double hiY = p.takeovers.get(i).hiY;
            if(wordInZone(word, loX, loY, hiX, hiY))
            {
                if(operation.equalsIgnoreCase("insert"))
                    p.takeovers.get(i).files.add(word);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Notifies neighbours of the change in one of their neighbours.
     * @param nodeToJoin    neighbours are extracted from this
     * @param splitMethod   whether split was done vertically or horizontally
     * @param oldLox        older low x.
     * @param oldLoy        older low y.
     * @param oldHix        older hi x.
     * @param oldHiy        older hi y.
     * @throws IOException 
     */
    void notifyNeighbours(Peer nodeToJoin, String splitMethod, Double oldLox, Double oldLoy, Double oldHix, Double oldHiy) throws IOException
    {
        String updateMessage = "Removed|"+splitMethod+"|"+nodeToJoin.myAddress.toString().replace("/","")+"|added|"+nodeToJoin.name+"|"+nodeToJoin.myAddress.toString().replace("/","")+"|"+nodeToJoin.port+"|"+nodeToJoin.loX.toString()+"|"+nodeToJoin.loY.toString()+"|"+nodeToJoin.hiX.toString()+"|"+nodeToJoin.hiY.toString()+"|"
                                            +p.name+"|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString()+"|"+p.loX.toString()+"|"+p.loY.toString()+"|"+p.hiX.toString()+"|"+p.hiY.toString()+"|OldValues|"+oldLox.toString()+"|"+oldLoy.toString()+"|"+oldHix.toString()+"|"+oldHiy.toString();
        for(Integer i=0; i<nodeToJoin.neighbours.size(); i++)
        {
            ds.send(new DatagramPacket(updateMessage.getBytes(), updateMessage.getBytes().length, nodeToJoin.neighbours.get(i).ipAddress, nodeToJoin.neighbours.get(i).port));
            ds.receive(dp);
            String newMessage = new String(buffer, dp.getOffset(), dp.getLength());
            if(newMessage.trim().equalsIgnoreCase("RequestingTakeOvers"))
            {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
                oos.flush();
                oos.writeObject((Object)nodeToJoin);
                oos.close();
                ds.send(new DatagramPacket(os.toByteArray(), os.toByteArray().length, dp.getAddress(), dp.getPort()));
                ds.receive(dp);
                String mess = new String(buffer, dp.getOffset(), dp.getLength());
            }
        }
    }
    
    /**
     * Updates hash tables between itself and the node that gets split
     * @param nodeToJoin    The node that gets split.
     */
    void updateHashTables(Peer nodeToJoin)
    {
        ArrayList<String> ntjTable = new ArrayList();
        ArrayList<String> pTable = new ArrayList();
        for(Integer i=0; i<nodeToJoin.files.size(); i++)
        {
            String currString = nodeToJoin.files.get(i);
            Double sumEven=0.0, sumOdd = 0.0;
            for(Integer j=0; j<currString.length(); j++)
            {
                if(j%2 == 0)
                    sumEven += (int)currString.charAt(j);
                else
                    sumOdd += (int)currString.charAt(j);
            }
            Double x = sumOdd%10;
            Double y = sumEven%10;
            if(x.doubleValue()>=p.loX.doubleValue() && x.doubleValue()<p.hiX.doubleValue() && y.doubleValue()>=p.loY.doubleValue() && y.doubleValue()<p.hiY.doubleValue())
                pTable.add(currString);
            else
                ntjTable.add(currString);
          }
          nodeToJoin.files = new ArrayList(ntjTable);
          p.files = new ArrayList(pTable);                   
    }
    
    /*
     * sends updated split zone to the actual node in order to update itself.
     */
    void updateNodeToJoin(Peer nodeToJoin) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
        oos.flush();
        oos.writeObject((Object)nodeToJoin);
        oos.close();
        ds.send(new DatagramPacket(os.toByteArray(), os.toByteArray().length, nodeToJoin.myAddress, nodeToJoin.port));
    }
    
    /**
     * updates neighbours in case a square is changing to a rectangle.
     * @param nodeToJoin    The node that is being split.
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    void updateNeighboursSquare(Peer nodeToJoin) throws IOException, ClassNotFoundException
    {
        //backup of old values
        Double oldLox = nodeToJoin.loX;
        Double oldLoy = nodeToJoin.loY;
        Double oldHix = nodeToJoin.hiX;
        Double oldHiy = nodeToJoin.hiY;
        
        p.loY = nodeToJoin.loY;
        p.hiY = nodeToJoin.hiY;
        if(p.randomX.doubleValue() <= ((nodeToJoin.loX.doubleValue()+nodeToJoin.hiX.doubleValue())/2))
        {
            p.loX = nodeToJoin.loX;
            p.hiX = nodeToJoin.loX + ((Math.abs(nodeToJoin.hiX.doubleValue() - nodeToJoin.loX.doubleValue()))/2);
            nodeToJoin.loX = p.hiX;
        }
        else
        {
            p.loX = nodeToJoin.loX + ((Math.abs(nodeToJoin.hiX.doubleValue() - nodeToJoin.loX.doubleValue()))/2);
            p.hiX = nodeToJoin.hiX;
            nodeToJoin.hiX = p.loX;
        }
        
        //notify neighbours of change
        notifyNeighbours(nodeToJoin, "SQR", oldLox, oldLoy, oldHix, oldHiy);
        
        
        //Update neighbours
        ArrayList<neighbour> newNeighboursNTJ = new ArrayList();
        for(Integer i=0; i<nodeToJoin.neighbours.size(); i++)
        {
            Double lx = nodeToJoin.neighbours.get(i).loX;
            Double ly = nodeToJoin.neighbours.get(i).loY;
            Double hx = nodeToJoin.neighbours.get(i).hiX;
            Double hy = nodeToJoin.neighbours.get(i).hiY;
            
            //request for takeovers
            ds.send(new DatagramPacket("RequestingTakeOvers".getBytes(), "RequestingTakeOvers".getBytes().length, nodeToJoin.neighbours.get(i).ipAddress, nodeToJoin.neighbours.get(i).port));
            ds.receive(dp);
            ByteArrayInputStream bis = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));
            Peer updatedPeer = (Peer) ois.readObject();
            
            if(((nodeToJoin.loX.equals(hx) || nodeToJoin.hiX.equals(lx)) && (!nodeToJoin.hiY.equals(ly)) && (!nodeToJoin.loY.equals(hy))) || ((nodeToJoin.hiY.equals(ly) || nodeToJoin.loY.equals(hy)) && (!nodeToJoin.hiX.equals(lx)) && (!nodeToJoin.loX.equals(hx))))
            {
                newNeighboursNTJ.add(new neighbour(nodeToJoin.neighbours.get(i).loX, nodeToJoin.neighbours.get(i).loY, nodeToJoin.neighbours.get(i).hiX, nodeToJoin.neighbours.get(i).hiY, nodeToJoin.neighbours.get(i).ipAddress, nodeToJoin.neighbours.get(i).port, nodeToJoin.neighbours.get(i).name));
                //update takeovers
                newNeighboursNTJ.get(newNeighboursNTJ.size()-1).takeovers = new ArrayList(updatedPeer.takeovers);
            }
            
            if(((p.loX.equals(hx) || p.hiX.equals(lx)) && (!p.hiY.equals(ly)) && (!p.loY.equals(hy))) || ((p.hiY.equals(ly) || p.loY.equals(hy)) && (!p.hiX.equals(lx)) && (!p.loX.equals(hx))))
            {
                p.neighbours.add(new neighbour(nodeToJoin.neighbours.get(i).loX, nodeToJoin.neighbours.get(i).loY, nodeToJoin.neighbours.get(i).hiX, nodeToJoin.neighbours.get(i).hiY, nodeToJoin.neighbours.get(i).ipAddress, nodeToJoin.neighbours.get(i).port, nodeToJoin.neighbours.get(i).name));
                //update takeovers
                p.neighbours.get(p.neighbours.size()-1).takeovers = new ArrayList(updatedPeer.takeovers);
            }
         }
         newNeighboursNTJ.add(new neighbour(p.loX, p.loY, p.hiX, p.hiY, p.myAddress, p.port, p.name));
         nodeToJoin.neighbours = new ArrayList(newNeighboursNTJ);
         p.neighbours.add(new neighbour(nodeToJoin.loX, nodeToJoin.loY, nodeToJoin.hiX, nodeToJoin.hiY, nodeToJoin.myAddress, nodeToJoin.port, nodeToJoin.name));
                 
         //Update hash tables
         updateHashTables(nodeToJoin);
         
         //Update nodetojoin
         updateNodeToJoin(nodeToJoin);
    }
    
    /**
     * Updates zone and splits it from rectangle to square.
     * @param nodeToJoin    node to be updated.
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    void updateNeighboursRectangle(Peer nodeToJoin) throws IOException, ClassNotFoundException
    {
        //backup of old values
        Double oldLox = nodeToJoin.loX;
        Double oldLoy = nodeToJoin.loY;
        Double oldHix = nodeToJoin.hiX;
        Double oldHiy = nodeToJoin.hiY;
        
        p.loX = nodeToJoin.loX;
        p.hiX = nodeToJoin.hiX;
        if(p.randomY.doubleValue() <= ((nodeToJoin.loY.doubleValue()+nodeToJoin.hiY.doubleValue())/2))
        {
            p.loY = nodeToJoin.loY;
            p.hiY = nodeToJoin.loY + ((Math.abs(nodeToJoin.hiY.doubleValue() - nodeToJoin.loY.doubleValue()))/2);
            nodeToJoin.loY = nodeToJoin.loY + ((Math.abs(nodeToJoin.hiY - nodeToJoin.loY))/2);
        }
        else
        {        
            p.loY = nodeToJoin.loY + ((Math.abs(nodeToJoin.hiY - nodeToJoin.loY))/2);
            p.hiY = nodeToJoin.hiY;
            nodeToJoin.hiY = nodeToJoin.loY + ((Math.abs(nodeToJoin.hiY - nodeToJoin.loY))/2);        
        }
        
        //notify neighbours of change
        notifyNeighbours(nodeToJoin, "RTS", oldLox, oldLoy, oldHix, oldHiy);
        
        //Update neighbours
       ArrayList<neighbour> newNeighboursNTJ = new ArrayList();
       for(Integer i=0; i<nodeToJoin.neighbours.size(); i++)
       {
            Double lx = nodeToJoin.neighbours.get(i).loX;
            Double ly = nodeToJoin.neighbours.get(i).loY;
            Double hx = nodeToJoin.neighbours.get(i).hiX;
            Double hy = nodeToJoin.neighbours.get(i).hiY;
            
            //request for takeovers
            ds.send(new DatagramPacket("RequestingTakeOvers".getBytes(), "RequestingTakeOvers".getBytes().length, nodeToJoin.neighbours.get(i).ipAddress, nodeToJoin.neighbours.get(i).port));
            ds.receive(dp);
            ByteArrayInputStream bis = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bis));
            Peer updatedPeer = (Peer) ois.readObject();
            
            if(((nodeToJoin.loX.equals(hx) || nodeToJoin.hiX.equals(lx)) && (!nodeToJoin.hiY.equals(ly)) && (!nodeToJoin.loY.equals(hy))) || ((nodeToJoin.hiY.equals(ly) || nodeToJoin.loY.equals(hy)) && (!nodeToJoin.hiX.equals(lx)) && (!nodeToJoin.loX.equals(hx))))
            {
                newNeighboursNTJ.add(new neighbour(nodeToJoin.neighbours.get(i).loX, nodeToJoin.neighbours.get(i).loY, nodeToJoin.neighbours.get(i).hiX, nodeToJoin.neighbours.get(i).hiY, nodeToJoin.neighbours.get(i).ipAddress, nodeToJoin.neighbours.get(i).port, nodeToJoin.neighbours.get(i).name));
                //update takeovers
                newNeighboursNTJ.get(newNeighboursNTJ.size()-1).takeovers = new ArrayList(updatedPeer.takeovers);
            }
            if(((p.loX.equals(hx) || p.hiX.equals(lx)) && (!p.hiY.equals(ly)) && (!p.loY.equals(hy))) || ((p.hiY.equals(ly) || p.loY.equals(hy)) && (!p.hiX.equals(lx)) && (!p.loX.equals(hx))))
            {
                p.neighbours.add(new neighbour(nodeToJoin.neighbours.get(i).loX, nodeToJoin.neighbours.get(i).loY, nodeToJoin.neighbours.get(i).hiX, nodeToJoin.neighbours.get(i).hiY, nodeToJoin.neighbours.get(i).ipAddress, nodeToJoin.neighbours.get(i).port, nodeToJoin.neighbours.get(i).name));
                //update takeovers
                p.neighbours.get(p.neighbours.size()-1).takeovers = new ArrayList(updatedPeer.takeovers);
            }
        }
        newNeighboursNTJ.add(new neighbour(p.loX, p.loY, p.hiX, p.hiY, p.myAddress, p.port, p.name));
        nodeToJoin.neighbours = new ArrayList(newNeighboursNTJ);
        p.neighbours.add(new neighbour(nodeToJoin.loX, nodeToJoin.loY, nodeToJoin.hiX, nodeToJoin.hiY, nodeToJoin.myAddress, nodeToJoin.port, nodeToJoin.name));
            
         //Update hash tables
         updateHashTables(nodeToJoin);
         
         //Update nodetojoin
         updateNodeToJoin(nodeToJoin);
    }
    
    /**
     * checks for takeovers and modifies them.
     * @param nodeToJoin    node to be updated.
     * @return 
     */
    Boolean checkAndModifyTakeOvers(Peer nodeToJoin)
    {
        String message = null;
        for(Integer i=0; i<nodeToJoin.takeovers.size(); i++)
        {
            if(p.randomX.doubleValue()>=nodeToJoin.takeovers.get(i).loX.doubleValue() && p.randomX.doubleValue()<=nodeToJoin.takeovers.get(i).hiX.doubleValue() && p.randomY.doubleValue()>=nodeToJoin.takeovers.get(i).loY.doubleValue() && p.randomY.doubleValue()<=nodeToJoin.takeovers.get(i).hiY.doubleValue())
            {
                p.loX = nodeToJoin.takeovers.get(i).loX;
                p.loY = nodeToJoin.takeovers.get(i).loY;
                p.hiX = nodeToJoin.takeovers.get(i).hiX;
                p.hiY = nodeToJoin.takeovers.get(i).hiY;
                p.neighbours = new ArrayList(nodeToJoin.takeovers.get(i).neighbours);
                
                
                //update neighbour's takeover to remove this take over
                for(Integer idx=0; idx<p.neighbours.size(); idx++)
                {
                    //TakeOver tOver = p.neighbours.get(idx).takeovers;
                    if(p.neighbours.get(idx).ipAddress.equals(nodeToJoin.myAddress) && (p.neighbours.get(idx).port.intValue()==nodeToJoin.port.intValue()))
                    {
                        Integer removalIndex = Integer.MAX_VALUE;
                        for(Integer jdx=0; jdx<p.neighbours.get(idx).takeovers.size(); jdx++)
                        {
                            TakeOver tOver = p.neighbours.get(idx).takeovers.get(jdx);
                            if(tOver.loX==p.loX && tOver.loY==p.loY && tOver.hiX==p.hiX && tOver.hiY==p.hiY)
                            {
                                removalIndex = jdx;
                                break;
                            }
                        }
                        if(removalIndex<Integer.MAX_VALUE)
                            p.neighbours.get(idx).takeovers.remove(p.neighbours.get(idx).takeovers.get(removalIndex));
                    }
                }
                
                //notify neighbours of changed peer.
                for(Integer j=0; j<nodeToJoin.takeovers.get(i).neighbours.size(); j++)
                {
                    if(!((nodeToJoin.takeovers.get(i).neighbours.get(j).ipAddress.equals(nodeToJoin.myAddress)) && (nodeToJoin.takeovers.get(i).neighbours.get(j).port.equals(nodeToJoin.port))))
                    {    
                        message = "Replace|"+nodeToJoin.myAddress.toString().replace("/","")+"|"+nodeToJoin.port.toString()+"|"+nodeToJoin.loX.toString()+"|"+nodeToJoin.loY.toString()+"|"+nodeToJoin.hiX.toString()+"|"+nodeToJoin.hiY.toString()+"|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString()+"|"+p.loX.toString()+"|"+p.loY.toString()+"|"+p.hiX.toString()+"|"+p.hiY.toString();

                        try {
                            ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, nodeToJoin.takeovers.get(i).neighbours.get(j).ipAddress, nodeToJoin.takeovers.get(i).neighbours.get(j).port));
                        } catch (IOException ex) {
                            Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                nodeToJoin.neighbours.add(new neighbour(p.loX, p.loY, p.hiX, p.hiY, p.myAddress, p.port, ""));
                
                
                //remind neighbours to remove one of their neighbour's takeover
                message = "RemoveNeighboursTakeOver|"+p.loX.toString()+"|"+p.loY.toString()+"|"+p.hiX.toString()+"|"+p.hiY.toString();
                for(Integer j=0; j<p.neighbours.size(); j++)
                {
                    if(!((p.neighbours.get(j).ipAddress.equals(nodeToJoin.myAddress)) && (p.neighbours.get(j).port.equals(nodeToJoin.port))))
                    {
                        try {
                            ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.neighbours.get(j).ipAddress, p.neighbours.get(j).port));
                        } catch (IOException ex) {
                            Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }   
                try {
                    //Update hash tables
                    p.files = new ArrayList(nodeToJoin.takeovers.get(i).files);
                    //updateHashTables(nodeToJoin);
         
                    nodeToJoin.takeovers.remove(nodeToJoin.takeovers.get(i));
                    //Update nodetojoin
                    updateNodeToJoin(nodeToJoin);
                } catch (IOException ex) {
                    Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                return true;
            }
        }
        return false;
    }
    
    /**
     * First operation of join.
     */
    void joinSomeOne()
    {
        String message = new String(p.randomX+"|"+p.randomY+"|Join");
        try {
            ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.bootStrapIp, p.bootStrapPort));
            ds.receive(dp);
            String debugged = new String(buffer, dp.getOffset(), dp.getLength());
            Boolean takeOverResult=false;
            if(debugged.contains("None"))
            {
                p.loX = 0.0;
                p.loY = 0.0;
                p.hiX = 10.0;
                p.hiY = 10.0;
            }
            else
            {
                String[] arguments = new String(dp.getData(), dp.getOffset(), dp.getLength()).split("\\|");
                message = "Forward|"+p.randomX.toString()+"|"+p.randomY.toString()+"|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString();
                ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(arguments[arguments.length-3]), Integer.parseInt(arguments[arguments.length-2])));
                ds.receive(dp);
                ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer, dp.getOffset(), dp.getLength());
                ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
                nodeToJoin = (Peer) is.readObject();
                
                                
                //has the node to join
                if(nodeToJoin.takeovers.size()>0)
                    takeOverResult = checkAndModifyTakeOvers(nodeToJoin);
                    
                if(!takeOverResult)
                {
                    if(Math.abs(nodeToJoin.loX-nodeToJoin.hiX) == Math.abs(nodeToJoin.loY-nodeToJoin.hiY))  //square
                        updateNeighboursSquare(nodeToJoin);
                    else    //rectangle
                        updateNeighboursRectangle(nodeToJoin);
                }
                
                else
                    return;
            }
            //update bootstrap
            String updateMessage;
            if(nodeToJoin != null && (!takeOverResult))
            {
                updateMessage = "Removed|"+nodeToJoin.myAddress.toString().replace("/","")+"|added|"+nodeToJoin.name+"|"+nodeToJoin.myAddress.toString().replace("/","")+"|"+nodeToJoin.port.toString()+"|"+p.name+"|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString()+"|Success";
            }
            else
                 updateMessage = "Removed|null|added|null|null|null|"+p.name+"|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString()+"|Success";               
            ds.send(new DatagramPacket(updateMessage.getBytes(), updateMessage.getBytes().length, p.bootStrapIp, p.bootStrapPort)); 
            
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Notifies neighbour of leave.
     * @param neighbourIndex    The neighbour index which is taking over.
     * @param newLox            New low x.
     * @param newLoy            new low y.
     * @param newHix            new hi x.
     * @param newHiy            new hi y.
     */
    void notifyNeighbourOfLeave(Integer neighbourIndex, Double newLox, Double newLoy, Double newHix, Double newHiy)
    {
        for(Integer i=0; i<p.neighbours.size(); i++)
        {
            if(i.equals(neighbourIndex))
                continue;
            String message = "Leave|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString()+"|"+p.neighbours.get(neighbourIndex).ipAddress.toString().replace("/","")+"|"+p.neighbours.get(neighbourIndex).port.toString()+"|"+newLox+"|"+newLoy+"|"+newHix+"|"+newHiy;
            try {
                ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.neighbours.get(i).ipAddress, p.neighbours.get(i).port));
                ds.receive(dp);
                String resp = new String(buffer, dp.getOffset(), dp.getLength());
                if(resp.equalsIgnoreCase("RequestingTakeOvers"))
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
                    oos.flush();
                    oos.writeObject((Object)p);
                    oos.close();
                    ds.send(new DatagramPacket(bos.toByteArray(), bos.toByteArray().length, dp.getAddress(), dp.getPort()));
                }
            } catch (IOException ex) {
                Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * First operation of leave.
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    void LeaveNetwork() throws IOException, ClassNotFoundException
    {
        //Check neighbours to see if it can form a rectangle or a square
        
        Boolean foundRect = false;
        if(p.takeovers.size()<1)  //has no takenover nodes
        {   
            for(Integer i=0; i<p.neighbours.size(); i++)
            {
                if(((p.neighbours.get(i).takeovers.size()<1) && (((p.loX.equals(p.neighbours.get(i).loX))&&(p.hiX.equals(p.neighbours.get(i).hiX)))||((p.loY.equals(p.neighbours.get(i).loY))&&(p.hiY.equals(p.neighbours.get(i).hiY))))))  //is able to form a rectangle or a square
                {
                    foundRect = true;
                    Double newLox = Math.min(p.loX, p.neighbours.get(i).loX);
                    Double newLoY = Math.min(p.loY, p.neighbours.get(i).loY);
                    Double newHix = Math.max(p.hiX, p.neighbours.get(i).hiX);
                    Double newHiY = Math.max(p.hiY, p.neighbours.get(i).hiY);
                    
                    //notify neighbours of removed to remove itself and add the neighbour with expanded zone
                    notifyNeighbourOfLeave(i, newLox, newLoY, newHix, newHiY);
                    
                    //notify neighbour to update its zone and also its neighbours of the modified coordinates
                    String message = "UpdateZoneAndNeighbours|"+newLox.toString()+"|"+newLoY.toString()+"|"+newHix.toString()+"|"+newHiY.toString()+"|"+p.myAddress.toString().replace("/", "")+"|"+p.port.toString();
                    try {
                        ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.neighbours.get(i).ipAddress, p.neighbours.get(i).port));
                        while(true)
                        {
                            ds.receive(dp);
                            String resp = new String(buffer, dp.getOffset(), dp.getLength());
                            if(resp.contains("RequestingYourNeighbours") || resp.length()==24)
                            {
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
                                oos.flush();
                                oos.writeObject((Object)p);
                                oos.close();
                                ds.send(new DatagramPacket(bos.toByteArray(), bos.toByteArray().length, dp.getAddress(), dp.getPort()));
                            }
                            else if(resp.contains("UpdateYourNeighbours") || resp.length()==20)
                            {
                                String[] arguments = resp.split("|");
                                for(Integer j=0; j<p.neighbours.size(); j++)
                                {
                                    if(p.neighbours.get(j).ipAddress.toString().replace("/", "").equalsIgnoreCase(arguments[1].trim()) && p.neighbours.get(j).port.toString().equalsIgnoreCase(arguments[2].trim()) && p.neighbours.get(j).loX.toString().equalsIgnoreCase(arguments[3].trim()) && p.neighbours.get(j).loY.toString().equalsIgnoreCase(arguments[4].trim()) && p.neighbours.get(j).hiX.toString().equalsIgnoreCase(arguments[5].trim()) && p.neighbours.get(j).hiY.toString().equalsIgnoreCase(arguments[6].trim()))
                                    {
                                        p.neighbours.get(j).loX = Double.parseDouble(arguments[7]);
                                        p.neighbours.get(j).loY = Double.parseDouble(arguments[8]);
                                        p.neighbours.get(j).hiX = Double.parseDouble(arguments[9]);
                                        p.neighbours.get(j).hiY = Double.parseDouble(arguments[10]);
                                    }
                                }  
                                ds.send(new DatagramPacket("Ack".getBytes(), "Ack".getBytes().length, dp.getAddress(), dp.getPort()));
                            }
                            else
                                break;
                        }
                        
                    } catch (IOException ex) {
                        Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //update bootsrap
                    message = "Removed|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString();
                    ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.bootStrapIp, p.bootStrapPort));
                    return;
                }
            }
        }
        if((!foundRect || p.takeovers.size()>0) && p.neighbours.size()>0)
        {
            Integer randomNeighbourIndex = (int)Math.random()*(p.neighbours.size());
            neighbour randomNeighbour = p.neighbours.get(randomNeighbourIndex);
            
            String message = "TakeMeOver|"+p.myAddress.toString().replace("/", "")+"|"+p.port.toString();
            ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.neighbours.get(randomNeighbourIndex).ipAddress, p.neighbours.get(randomNeighbourIndex).port));
            
            while(true)
            {
                ds.receive(dp);
                String resp = new String(buffer, dp.getOffset(), dp.getLength());
                if(resp.length() == 3)
                    break;
                if((resp.trim().equalsIgnoreCase("RequestingTakeOvers") || resp.trim().equalsIgnoreCase("RequestingYou") || resp.length()==13 || resp.length()==19))
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
                    oos.flush();
                    oos.writeObject((Object)p);
                    oos.close();
                    ds.send(new DatagramPacket(bos.toByteArray(), bos.toByteArray().length, dp.getAddress(), dp.getPort()));
                }
                else if(resp.trim().equalsIgnoreCase("UpdateTakeOver") || resp.length()==14)
                {
                    ds.send(new DatagramPacket("Nack".getBytes(), "Nack".getBytes().length, dp.getAddress(), dp.getPort()));
                }
            }
            for(Integer i=0; i<p.neighbours.size(); i++)
            {
                if(i.equals(randomNeighbourIndex))
                    continue;
                message = "Update|"+p.myAddress.toString().replace("/","")+"|"+p.loX.toString()+"|"+p.loY.toString()+"|"+p.hiX.toString()+"|"+p.hiY.toString()+"|"+randomNeighbour.ipAddress.toString().replace("/","")+"|"+randomNeighbour.port.toString()+"|"+randomNeighbour.loX.toString()+"|"+randomNeighbour.loY.toString()+"|"+randomNeighbour.hiX.toString()+"|"+randomNeighbour.hiY.toString()+"|"+p.port.toString();
                ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.neighbours.get(i).ipAddress, p.neighbours.get(i).port));
            }
            //update bootstrap
            message = "Removed|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString();
            ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.bootStrapIp, p.bootStrapPort));
            return;
        }
        
        //update bootstrap
        String message = "Removed|"+p.myAddress.toString().replace("/","")+"|"+p.port.toString();
        ds.send(new DatagramPacket(message.getBytes(), message.getBytes().length, p.bootStrapIp, p.bootStrapPort));
    }
    
    /**
     * inserts or searches for the keyword according to the operation given
     * @param word          Word to be inserted or searched.
     * @param operation     Operation to be performed.
     */
    void insertOrSearchKeyword(String word, String operation)
    {
        String messageAndPath = p.myAddress.toString().replace("/", "")+"|"+p.port.toString();
        InetAddress routeNext = null;
        Integer portL = null;
        Boolean successFlag = false;
        if(attemptInsertOrSearchInCurrentZone(word, operation))
        {
            if(operation.equalsIgnoreCase("insert"))
            {
                successFlag = true;
                messageAndPath += "|Success";
            }
            else if(operation.equalsIgnoreCase("search"))
            {
                Boolean found = false;
                for(Integer i=0; i<p.files.size(); i++)
                {
                    if(word.equalsIgnoreCase(p.files.get(i)))
                    {
                        messageAndPath += "|Success";
                        successFlag = true;
                        found = true;
                        break;
                    }
                }
                if(!found)
                {
                    for(Integer i=0; i<p.takeovers.size(); i++)
                    {
                        Boolean innerFound = false;
                        for(Integer j=0; j<p.takeovers.get(i).files.size(); j++)
                        {
                            if(word.equalsIgnoreCase(p.takeovers.get(i).files.get(j)))
                            {
                                successFlag = true;
                                found = true;
                                messageAndPath += "|Success";
                                innerFound = true;
                                break;
                            }
                        }
                        if(innerFound)
                            break;
                    }    
                }    
                
                if(!found)
                {
                    messageAndPath += "|Failure";
                    successFlag = false;
                }
            }
        }
        else
        {
            Double x = computeXOrY(word, true);
            Double y = computeXOrY(word, false);
            Double minDistance = Double.MAX_VALUE;
            for(Integer i=0; i<p.neighbours.size(); i++)
            {
                if(p.neighbours.get(i).loX.doubleValue()<=x.doubleValue() && p.neighbours.get(i).hiX.doubleValue()>x.doubleValue() && p.neighbours.get(i).loY.doubleValue()<=y.doubleValue() && p.neighbours.get(i).hiY.doubleValue()>y.doubleValue())
                {
                    routeNext = p.neighbours.get(i).ipAddress;
                    portL = p.neighbours.get(i).port;
                    break;
                }
                Double midX = p.neighbours.get(i).loX+((p.neighbours.get(i).hiX - p.neighbours.get(i).loX)/2);
                Double midY = p.neighbours.get(i).loY+((p.neighbours.get(i).hiY - p.neighbours.get(i).loY)/2);
                Double currDistance = Math.sqrt(Math.pow((y.doubleValue()-midX.doubleValue()), 2)+Math.pow(y.doubleValue()-midY.doubleValue(), 2));
                if(currDistance.doubleValue()<minDistance.doubleValue())
                {
                    minDistance = currDistance;
                    routeNext = p.neighbours.get(i).ipAddress;
                    portL = p.neighbours.get(i).port;
                }
                Boolean isInTOver = false;
                for(Integer j=0; j<p.neighbours.get(i).takeovers.size(); j++)
                {
                    TakeOver tOver = p.neighbours.get(i).takeovers.get(j);
                    midX = tOver.loX+((tOver.hiX - tOver.loX)/2);
                    midY = tOver.loY+((tOver.hiY - tOver.loY)/2);
                    currDistance = Math.sqrt(Math.pow((x.doubleValue()-midX.doubleValue()), 2)+Math.pow(y.doubleValue()-midY.doubleValue(), 2));
                    if(tOver.loX.doubleValue()<=x.doubleValue() && tOver.hiX.doubleValue()>x.doubleValue() && tOver.loY.doubleValue()<=y.doubleValue() && tOver.hiY.doubleValue()>y.doubleValue())
                    {
                        routeNext = p.neighbours.get(i).ipAddress;
                        portL = p.neighbours.get(i).port;
                        isInTOver = true;
                        break;
                    }
                    if(currDistance.doubleValue() < minDistance.doubleValue())
                    {
                        minDistance = currDistance;
                        routeNext = p.neighbours.get(i).ipAddress;
                        portL = p.neighbours.get(i).port;
                    }
                }
                if(isInTOver)
                    break;
            }
            String mess = operation+"|"+word+"|"+messageAndPath;
            try {
                ds.send(new DatagramPacket(mess.getBytes(), mess.getBytes().length, routeNext, portL));
                ds.receive(dp);
                String Path = new String(buffer, dp.getOffset(), dp.getLength());
                if(Path.contains("Success"))
                    successFlag = true;
                String [] arguments = Path.split("\\|");
                messageAndPath = "";
                for(Integer i=2; i<arguments.length-2; i+=2)
                {
                    if(i==2)
                        messageAndPath += arguments[i]+":"+arguments[i+1];
                    else
                        messageAndPath += "->"+arguments[i]+":"+arguments[i+1];
                }
                messageAndPath += "->"+arguments[arguments.length-1];
            } catch (IOException ex) {
                Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(successFlag)
        {
            System.out.println("Success");
            System.out.println("Path: "+messageAndPath);
        }
        else
            System.out.println("Failure");
        //return messageAndPath;
    }
    
    /**
     * view operation
     */
    void viewPeer()
    {
        System.out.println("\nIP: "+p.myAddress+" Port: "+p.port);
        System.out.println("Zone");
        System.out.println("LoX: "+p.loX);
        System.out.println("LoY: "+p.loY);
        System.out.println("HiX: "+p.hiX);
        System.out.println("HiY: "+p.hiY);
        System.out.println("\nTake Overs are the following:");
        for(Integer i=0; i<p.takeovers.size(); i++)
        {
            System.out.println("Take Over: "+i+1);
            System.out.println("LoX: "+p.takeovers.get(i).loX);
            System.out.println("LoY: "+p.takeovers.get(i).loY);
            System.out.println("HiX: "+p.takeovers.get(i).hiX);
            System.out.println("HiY: "+p.takeovers.get(i).hiY);
        }
        System.out.println("\nNeighbours are the following:");
        Integer count = 1;
        for(Integer i=0; i<p.neighbours.size(); i++)
        {
            System.out.println("Neighbour: "+count);
            System.out.println("IP: "+p.neighbours.get(i).ipAddress+" Port: "+p.neighbours.get(i).port);
            System.out.println("LoX:"+p.neighbours.get(i).loX);
            System.out.println("LoY:"+p.neighbours.get(i).loY);
            System.out.println("HiX:"+p.neighbours.get(i).hiX);
            System.out.println("HiY:"+p.neighbours.get(i).hiY);
            System.out.println("\n");
            System.out.println("Neighbour "+count+"'s takeovers are:");
            for(Integer j=0; j<p.neighbours.get(i).takeovers.size(); j++)
            {
                System.out.println("TakeOver: "+(j+1));
                TakeOver tOver = p.neighbours.get(i).takeovers.get(j);
                System.out.println("LoX:"+tOver.loX);
                System.out.println("LoY:"+tOver.loY);
                System.out.println("HiX:"+tOver.hiX);
                System.out.println("HiY:"+tOver.hiY);
            }
            count++;
         }
         System.out.println("\nTakeOvers' neighbours are the following:");
         for(Integer i=0; i<p.takeovers.size(); i++)
         {
            TakeOver tOver = p.takeovers.get(i);
            for(Integer j=0; j<tOver.neighbours.size(); j++)
            {
                System.out.println("Neighbour: "+count++);
                System.out.println("IP: "+tOver.neighbours.get(j).ipAddress+" Port: "+tOver.neighbours.get(j).port);
                System.out.println("LoX:"+tOver.neighbours.get(j).loX);
                System.out.println("LoY:"+tOver.neighbours.get(j).loY);
                System.out.println("HiX:"+tOver.neighbours.get(j).hiX);
                System.out.println("HiY:"+tOver.neighbours.get(j).hiY);
                System.out.println("\n");
            }
        }
        System.out.println("\nData items stored are the following:");
        for(Integer i=0; i<p.files.size(); i++)
        {
            System.out.println(p.files.get(i));
        }
        for(Integer i=0; i<p.takeovers.size(); i++)
        {
            for(Integer j=0; j<p.takeovers.get(i).files.size(); j++)
                System.out.println(p.takeovers.get(i).files.get(j));
        }
    }
    
     void reinitialize()
     {
	p.neighbours = new ArrayList();
        p.files = new ArrayList();
        p.takeovers = new ArrayList();
	p.randomX = (Math.random()*10);
        p.randomY = (Math.random()*10);
     }
    /**
     * Starting point of execution.
     * @param args  not used.
     * @throws ClassNotFoundException 
     */
    public static void main(String[] args){
        // TODO code application logic here
        Boolean joinFlag = false;
        String message=null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        do
        {
            System.out.println("Enter command:");
            try {
                message = reader.readLine();
            } catch (IOException ex) {
                Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(message!=null && !message.equalsIgnoreCase("join"))
                System.out.println("First command should be join");
        }while(message==null || !(message.equalsIgnoreCase("join")));
        PeerOperations peer= new PeerOperations();
        peer.joinSomeOne();
        new Thread(peer).start();
        peer.viewPeer();
        Integer choice;
        joinFlag = true;
        while(true)
        {
            try {
                System.out.println("Enter Command:");
                String input = new String(reader.readLine());
                if(input.trim().equalsIgnoreCase("join") && !joinFlag)
                {
                    peer.reinitialize();
                    peer.joinSomeOne();
                    System.out.println("Join successful");
                    peer.viewPeer();
                    joinFlag = true;
                }
                else if(input.trim().equalsIgnoreCase("leave") && joinFlag)
                {
                    joinFlag = false;
                    peer.ds.send(new DatagramPacket("LeaveNetwork".getBytes(), "LeaveNetwork".getBytes().length, peer.p.myAddress, peer.p.port));
                }
                else if(input.trim().equalsIgnoreCase("view") && joinFlag)
                    peer.viewPeer();
                else if((input.contains("insert") || input.contains("search")) && joinFlag)
                {
                    System.out.println("Sent "+input+" "+peer.p.myAddress+" "+peer.p.port);
                    peer.ds.send(new DatagramPacket(input.getBytes(), input.getBytes().length, peer.p.myAddress, peer.p.port));
                }
                else
                    System.out.println("Invalid command");
            } 
            catch (IOException ex) 
            {
                Logger.getLogger(PeerOperations.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
