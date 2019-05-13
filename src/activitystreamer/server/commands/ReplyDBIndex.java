package activitystreamer.server.commands;

import activitystreamer.server.Connection;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Control;

// This class is used when the leader fails and the other servers compete for the leader role.
// We need to ensure that only the server with global highest log index in Log DB is eligible for election.
// If the server finds that some other servers have higher log index in Log DB, it will abandon the election.
// So the server needs to examine the highest Log Index from other servers.

public class ReplyDBIndex {
    private Connection conn;
    private String msg;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public ReplyDBIndex(String msg, Connection con){
        conn = con;
        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            long addIndexLong = (long)message.get("index");
            int addIndex = (int)addIndexLong;
            Control.appendDBIndexList(addIndex);

            // If it has received DB index from all other servers.
            if(Control.checkDBIndexListSize()){
                // If all of them are <= its own log index
                if(Control.checkDBIndexList()){
                    if (Control.getInstance().getNeighbors().size() > 0) {
                        // Start election....
                        log.info("Now making new selection.");
                        Control.getInstance().sendSelection(Control.getInstance().getLamportTimeStamp());
                    }
                }
                Control.cleanDBIndexList();
            }
        }
        catch (ParseException e) {
            log.debug(e);
        }
    }

    public boolean getCloseCon() {
        return closeConnection;
    }
}
