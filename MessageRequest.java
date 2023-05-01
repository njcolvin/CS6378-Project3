import java.io.Serializable;
import java.time.LocalDateTime;

public class MessageRequest extends Message implements Comparable<MessageRequest>, Serializable {

    final int id;
    final LocalDateTime timestamp;

    public MessageRequest(int id, LocalDateTime timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(MessageRequest other) {
        return this.timestamp.compareTo(other.timestamp);
    }
    
}
