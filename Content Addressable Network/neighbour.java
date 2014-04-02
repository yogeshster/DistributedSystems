import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Neighbour information.
 * @author Yogesh Jagadeesan
 */
public class neighbour implements Serializable{
    
    /**
     * Default constructor.
     */
    public neighbour()
    {}
    
    /**
     * Parameterized constructor for neighbour
     * @param lx    lower x coordinate
     * @param ly    lower y coordinate
     * @param hx    higher x coordinate
     * @param hy    higher y coordinate
     * @param addr  address of neighbour
     * @param port  port of neighbour
     * @param nme   Name of neighbour
     */
    public neighbour(Double lx, Double ly, Double hx, Double hy, InetAddress addr, Integer port, String nme)
    {
        this.loX = lx;
        this.loY = ly;
        this.hiX = hx;
        this.hiY = hy;
        this.ipAddress = addr;
        this.name = nme;
        this.port = port;
    }
    Double loX;
    Double loY;
    Double hiX;
    Double hiY;
    InetAddress ipAddress;
    String name;
    Integer port;
    Boolean isTemporary = false;
    ArrayList<TakeOver> takeovers = new ArrayList();
}
