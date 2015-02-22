import com.googlecode.objectify.ObjectifyService;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import static com.googlecode.objectify.ObjectifyService.ofy;
import java.io.*;
import com.google.appengine.api.datastore.Blob;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class HrTestData {
    static {
        ObjectifyService.register(HrTestDataRecord.class);
    }

    static String readFile(String path, Charset encoding) 
     throws IOException 
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void main(String[] args) throws IOException {



	    String username = "motoalgotest@gmail.com";
	    String password = "jjass123";
        RemoteApiOptions options = new RemoteApiOptions()
         //   .server("xenon-broker-853.appspot.com", 443)
            .server("xenon-coast-802.appspot.com", 443)
            .credentials(username, password);
        RemoteApiInstaller installer = new RemoteApiInstaller();
        installer.install(options);
        try {

            /* Upload 

            String swVersionInput = System.console().readLine("sw version: ");
            String fileNameInput = System.console().readLine("moto360 file:");
            String moto360Data = readFile(fileNameInput, StandardCharsets.UTF_8);

	       HrTestDataRecord rec = new HrTestDataRecord();
           rec.setSwVersion(swVersionInput);
           
           byte[] b = moto360Data.getBytes();
           Blob blob = new Blob(b);

           rec.setMoto360Data(blob);
           ofy().save().entity(rec).now();
            */

           /* Download */
            String idStrInput = System.console().readLine("id: ");
            PrintStream out = new PrintStream(new FileOutputStream(idStrInput));
            System.setOut(out); 

           Long id = Long.parseLong(idStrInput, 10);
           HrTestDataRecord h = ofy().load().type(HrTestDataRecord.class).id(id).now();
	       Blob blb = h.getMoto360Data();

           String str = new String(blb.getBytes(), "UTF-8"); 
           System.out.println(str);
            /* */
        } finally {
            installer.uninstall();
        }
    }
}


/*
    private static final int DEFAULT_LIST_LIMIT = 20;

limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<HrTestDataRecord> query = ofy().load().type(HrTestDataRecord.class).filter("fileName", fileNameInput).limit(limit);

        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
            logger.info("cursor not null");
        }
        QueryResultIterator<HrTestDataRecord> queryIterator = query.iterator();
        List<HrTestDataRecord> dataList = new ArrayList<HrTestDataRecord>(limit);
        while (queryIterator.hasNext()) {
            dataList.add(queryIterator.next());
        }
        */

