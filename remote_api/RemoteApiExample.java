import com.googlecode.objectify.ObjectifyService;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import static com.googlecode.objectify.ObjectifyService.ofy;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.*;

public class RemoteApiExample {
    static {
        ObjectifyService.register(SensorData.class);
    }

    public static void main(String[] args) throws IOException {
//        String username = System.console().readLine("username: ");
//        String password =
//            new String(System.console().readPassword("password: "));

	String idStr = System.console().readLine("id: ");
	String username = "motoalgotest@gmail.com";
	String password = "jjass123";
        RemoteApiOptions options = new RemoteApiOptions()
            .server("xenon-coast-802.appspot.com", 443)
            .credentials(username, password);
        RemoteApiInstaller installer = new RemoteApiInstaller();
        installer.install(options);
        try {

	    PrintStream out = new PrintStream(new FileOutputStream(idStr));
	    System.setOut(out);

	    Long id = Long.parseLong(idStr, 10);
	    SensorData sd = ofy().load().type(SensorData.class).id(id).now();
	    String r = sd.getResult();
    	    String d = sd.getSensorData();
	 
	    JSONParser parser = new JSONParser();
	    try {
            	Object obj = parser.parse(d);
                JSONObject jsonObject = (JSONObject) obj;
            	String jsonId = (String) jsonObject.get("regId");

            	JSONArray jsonDataArray = (JSONArray) jsonObject.get("sensor_data_buffer");
            	Iterator<JSONObject> iterator = jsonDataArray.iterator();

            	while (iterator.hasNext()) {
                	JSONObject jsonData = iterator.next();
                	System.out.println(jsonData.get("x"));
            	}

	        System.out.println(r);

            } catch (ParseException e) {
            	e.printStackTrace();
            }

        } finally {
            installer.uninstall();
        }
    }
}
