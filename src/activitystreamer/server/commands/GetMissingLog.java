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

import java.sql.*;

public class GetMissingLog {
    private static Connection conn;
    private static java.sql.Connection sqlConnection;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl =Settings.getSqlLogUrl();

    // When a server receives "GetMissingLog" request, it will return the log info from its own database between
    // the requested minId and maxId. So the receiver will learn about its missed log.

    public GetMissingLog(String msg, Connection con){
        conn = con;

        try{
            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);

            //Read in the missed start index and end index
            long startIndexLong = (long)message.get("startIndex");
            long endIndexLong = (long)message.get("endIndex");
            int startIndex = (int)startIndexLong;
            int endIndex = (int) endIndexLong;

            String sqlQuery = "SELECT * FROM Log WHERE LogId >= "+ startIndex + " AND LogId <= "+endIndex+";";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);
            JSONObject values = new JSONObject();

            while(result.next()){
                values.put(result.getInt("LogId"), result.getString("Value"));
            }
            //If it does not have any value, return "null" string
            con.writeMsg(Command.createMissingLogInfo(values, startIndex, endIndex));

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
