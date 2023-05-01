import java.io.Serializable;

public class MessageRelease extends Message implements Serializable {

    final int id;
    int sent;

    public MessageRelease(int id) {
        this.id = id;
        this.sent = 0;
    }
    
}
