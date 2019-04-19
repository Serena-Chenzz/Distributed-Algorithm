package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.Command;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.ControlBroadcast;

public class ActivityBroadcast {

    private boolean closeConnection=false;
    private final Logger log = LogManager.getLogger();

    public ActivityBroadcast(Connection con, String msg) {
        JSONParser parser = new JSONParser();
        JSONObject message;
        try {
            //check that JSON format is valid
            message = (JSONObject) parser.parse(msg);
            //Check that message is received from an Authenticated server
            if (!Control.getInstance().containsServer(con.getRemoteId())) {
                String info = "Lock_Request is not from an authenticated server";
                con.writeMsg(Command.createInvalidMessage(info));
                closeConnection = true;
            } else {
                ControlBroadcast.broadcastClients(msg);
                Control.getInstance().broadcast(msg, con.getRemoteId());
                closeConnection=false;
            }
        } catch (ParseException e) {
            Command.createInvalidMessage("JSON parse error while parsing message");
            closeConnection=true;
        }
    }

    public boolean getResponse() {
        return closeConnection;
    }

}
