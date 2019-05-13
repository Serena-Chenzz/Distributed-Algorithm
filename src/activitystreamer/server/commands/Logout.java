package activitystreamer.server.commands;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.server.Connection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.*;

public class Logout {
    
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static java.sql.Connection sqlConnection;
    private static final String sqlUrl =Settings.getSqlUrl();

    public Logout(Connection con, String msg) {
        Logout.conn = con;
        closeConnection = true;

        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            String username = message.get("username").toString();
            String secret = message.get("secret").toString();
        } catch (ParseException e) {
            log.debug(e);
        }

    }

    public boolean getResponse() {
        return closeConnection;
    }

}