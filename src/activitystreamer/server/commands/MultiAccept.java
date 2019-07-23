package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;


public class MultiAccept {
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    // When the leader receives a request from client sequentially, it will send Multi_Accept msg to all other servers
    // to notify all participants in distributed server system. Whenever the server receives the msg, it will append
    // the msg to its own unchosen logs and return accept msg to the leader.

    public MultiAccept(String msg, Connection con){
        conn = con;

        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            long leaderFirstUnchosenIndexLong = (long)message.get("firstUnchosenIndex");
            int leaderFirstUnchosenIndex = (int)leaderFirstUnchosenIndexLong;
            String value = message.get("value").toString();
            long indexLong = (long)message.get("index");
            int index = (int) indexLong;

            // First, check if its firstUnchosenIndex < leader's firstUnchosenInde, if so, abort the server since it
            // is in an inconsistent state against the global system.

            int myFirstUnchosenIndex = Control.getFirstUnchosenIndex();
            if(myFirstUnchosenIndex < leaderFirstUnchosenIndex){
                // Notify the user about the error
                log.error("Please restart the system, the system is in an inconsistent state...");
                // Abort the server.
                System.exit(1);
            }
            else{
                //Append the msg to the UnChosenLogList
                Control.appendUnChosenLogs(value,con);

                //Return accepted msg
                conn.writeMsg(Command.createMultiAccepted(index));
                closeConnection = false;
            }

        }catch (ParseException e) {
            log.debug(e);
        }
    }

    public boolean getCloseCon() {
        return closeConnection;
    }

}
