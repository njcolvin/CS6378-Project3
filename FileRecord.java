import java.io.Serializable;

public class FileRecord implements Serializable {
    int VN;
    int RU;
    Integer DS;

    public FileRecord(int vn, int ru, Integer ds) {
        this.VN = vn;
        this.RU = ru;
        this.DS = ds;
    }

    public String toString() {
        return this.VN + " " + this.RU + " " + (char) (this.DS + 'a' - 1);
    }

}
