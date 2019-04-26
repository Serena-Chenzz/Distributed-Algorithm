package activitystreamer.server.commands;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.Command;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;

import java.sql.*;


public class PurchasingMessage {

    private boolean closeConnection=false;
    private final Logger log = LogManager.getLogger();
    private static java.sql.Connection sqlConnection;
    private static final String sqlUrl =
            "jdbc:sqlite:/Users/luchen/Documents/Documents/Melb_Uni_Life/Semester4/Distributed Algorithm/Project/sqliteDB";


    public PurchasingMessage(Connection con, String msg) {

        JSONParser parser = new JSONParser();
        JSONObject message;
        try {
            sqlConnection = DriverManager.getConnection(sqlUrl);

            message = (JSONObject) parser.parse(msg);
            String username = message.get("username").toString();
//            JSONObject activity = (JSONObject) message.get("activity");
//            activity.put("authenticated_user", username);
            String secret = message.get("secret").toString();

            log.debug("check :"+username+"/"+secret);

            String sqlQuery = "SELECT * FROM User WHERE UserName = "+ username + " AND UserPassword =  " + secret +
                    " AND LoggedInOrNot = 1;";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);

            if(result.next()) {
                // To be written: broadcast the msgs to the acceptors
                closeConnection = false;
                return;

            }else {
                //If this username and secret are not correct, we send an authentication failed
                con.writeMsg(Command.createAuthFailedUserNotLoggedIn(username));
                closeConnection = true;
                return;
            }


        } catch (ParseException e) {
            Command.createInvalidMessage("JSON parse error while parsing message");
            closeConnection=true;
            return;
        } catch (SQLException e){
            log.debug(e);
        }
    }

    public boolean getResponse() {
        return closeConnection;
    }

}
