package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.Command;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;

public class Logout {
    
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public Logout(Connection con, String msg) {
        Logout.conn = con;
        closeConnection = true;
        Login.logoutUser(con);
    }

    public boolean getResponse() {
        return closeConnection;
    }

}