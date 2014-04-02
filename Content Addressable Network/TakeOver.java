import java.io.Serializable;
import java.util.ArrayList;

/**
 * Take over information
 *
 * @author Yogesh Jagadeesan
 */
public class TakeOver implements Serializable{
    Double loX;
    Double loY;
    Double hiX;
    Double hiY;
    ArrayList<String> files = new ArrayList();
    ArrayList<neighbour> neighbours = new ArrayList();    
}
