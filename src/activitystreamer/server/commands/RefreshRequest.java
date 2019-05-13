package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;

import java.sql.*;

public class RefreshRequest {

    private static Connection conn;
    private static java.sql.Connection sqlConnection;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl = Settings.getSqlUrl();

    // This class is used when the client wants to refresh the panel about the latest ticket information
    // Or when the client click on any button and the panel info needs to be refreshed.

    public RefreshRequest(String msg, Connection con){
        conn = con;
        try{
            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            //Read in the username and secret
            String username = message.get("username").toString();

            int userId = 0;
            String sqlQuery = "SELECT * FROM User WHERE UserName = '"+ username + "';";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);

            while(result.next()){
                userId = result.getInt("UserId");
            }

            // Retrieve the train info (including left ticket number)
            JSONObject trainInfo = new JSONObject();
            String sqlTrainQuery = "SELECT * FROM Train;";
            ResultSet trainRes = stmt.executeQuery(sqlTrainQuery);

            while(trainRes.next()){
                trainInfo.put(trainRes.getInt("TrainId"), trainRes.getInt("LeftTickets"));
            }

            //Retrieve the user's purchase records.
            JSONArray ticketInfo = new JSONArray();
            String sqlTicketQuery = "SELECT * FROM Ticket WHERE UserId = '"+ userId + "';";
            ResultSet ticketResult = stmt.executeQuery(sqlTicketQuery);

            while(ticketResult.next()){
                ticketInfo.add(ticketResult.getInt("TrainId"));
            }

            // Return back the refreshed ticket info
            conn.writeMsg(Command.createRefreshInfo(username, trainInfo, ticketInfo));
            sqlConnection.close();
            closeConnection = false;

        }
        catch (ParseException e) {
            log.debug(e);
        }
        catch (SQLException e){
            log.debug(e);
        }

    }
    public boolean getCloseCon() {
        return closeConnection;
    }
}
