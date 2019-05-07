package activitystreamer.server.commands;

import activitystreamer.server.Connection;

import java.util.HashMap;
import java.util.Map;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Control;

import java.sql.*;

public class AskDBIndex {
    private Connection conn;
    private static java.sql.Connection sqlConnection;
    private String msg;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlLogUrl = Settings.getSqlLogUrl();
    private static int myMaxDBIndex = 0;

    public AskDBIndex(String msg, Connection con, int flag){
        // flag - 1: broadcasted Ask, -2: ask for leader's index
        conn = con;
        try{
            sqlConnection = DriverManager.getConnection(sqlLogUrl);

            //Checking the username and password
            String sqlQuery = "SELECT MAX(LogId) AS MAX FROM Log;";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);

            int maxId = -1;

            while(result.next()){
                maxId = result.getInt("MAX");
            }

            String replyMsg;
            if(flag == 1){
                replyMsg = Command.createReplyDBIndex(maxId);
                con.writeMsg(replyMsg);
                log.info(replyMsg);
            }
            else if(flag == 2){
                replyMsg = Command.createReplyLeaderDBIndex(maxId);
                con.writeMsg(replyMsg);
                log.info(replyMsg);
            }
            else if(flag == 3){
                myMaxDBIndex = maxId;
                log.info("the max id in the current db is: " + maxId);
            }

        }
        catch (SQLException e){
            log.debug(e);
        }
    }

    public boolean getCloseCon() {
        return closeConnection;
    }

    public int getMyLargestDBIndex(){
        return myMaxDBIndex;
    }
}
