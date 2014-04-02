
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Peer information
 * @author Yogesh Jagadeesan
 */
public class Peer implements Serializable
{
    Double loX;
    Double loY;
    Double hiX;
    Double hiY;
    Double randomX;
    Double randomY;
    String name;
    InetAddress bootStrapIp;
    Integer bootStrapPort;
    InetAddress myAddress;
    ArrayList<neighbour> neighbours;
    Integer port;
    ArrayList<String> files;    
    ArrayList<TakeOver> takeovers;
}
