package activitystreamer.server.commands;

import activitystreamer.models.Command;
import activitystreamer.models.UniqueID;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.Load;
import static activitystreamer.server.commands.ServerAnnounce.doActivity;
import activitystreamer.util.Settings;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

// Just set the local value to what is attached in this DECIDE message,
// no matter under what circumstance.
// A DECIDE message only contains the final agreed value.
public class Decide{
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private String msg;

    public Decide(String decideMsg, Connection con) {
        msg = decideMsg;
        try {
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            String value = message.get("value").toString();
            Control.getInstance().setAcceptedValue(value);
            Decide.conn = con;
            //start();
            log.info("Learned from leader " + value);
            Control.setLeaderHasBeenDecided(true);
            Control.setLeaderConnection(value);
            Control.getInstance().clearAcceptor();
        }catch (ParseException e) {
            log.debug(e);
        }

    }

//    @Override
//    public void run() {
//        Control.getInstance().broadcast(msg);
//    }

    public boolean getCloseCon() {
        return closeConnection;
    }
}
