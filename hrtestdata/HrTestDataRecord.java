
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.google.appengine.api.datastore.Blob;

/**
 * Created by fsd017 on 12/22/14.
 */
@Entity
public class HrTestDataRecord{

    @Id
    Long id;
    @Index
    String fileName;
    
    String swVersion;
    Blob moto360Data;

    public Long getId() {
        return id;
    }

    public String getSwVersion() {
        return swVersion;
    }

    public Blob getMoto360Data() {
        return moto360Data;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    public void setMoto360Data(Blob data) {
        this.moto360Data = data;
    }

}

