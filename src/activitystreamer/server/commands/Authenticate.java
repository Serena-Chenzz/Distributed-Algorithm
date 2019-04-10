package activitystreamer.server.commands;

import activitystreamer.models.Command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;

public class Authenticate {

    private boolean closeConnection = false;
    private final Logger log = LogManager.getLogger();

    public Authenticate(String msg, Connection con) {
        JSONParser parser = new JSONParser();
        JSONObject message;
        try {
            // Check that the server has not been authenticated
            if (Control.getInstance().containsServer(con.getRemoteId())){
                con.writeMsg(Command.createInvalidMessage("The server has already "
                        + "been authenticated."));
                closeConnection = true;
                return;
            }         
            // Check that the server has correct secret, if not, send 
            // authentication fail
            message = (JSONObject) parser.parse(msg);
            String secret = (String) message.get("secret");
            if(!Settings.getSecret().equals(secret)){                
                con.writeMsg(Command.createAuthenticateFailed(secret, Control.getInstance().getUniqueId()));
                closeConnection = true;
                return;
            }
            log.debug("authentication success for secret: " + secret);
            
        } catch (ParseException e) {
            log.fatal(e);
        }

    }

    public boolean getResponse() {
        return closeConnection;
    }

}