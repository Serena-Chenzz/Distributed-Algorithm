package activitystreamer.server.commands;

import activitystreamer.server.Connection;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.models.*;

import java.sql.*;

// This class is used when another server wants to know the current server's max log index in the database
public class AskDBIndex {
    private Connection conn;
    private static java.sql.Connection sqlConnection;
    private String msg;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlLogUrl = Settings.getSqlLogUrl();
    private static int myMaxDBIndex = 0;

    public AskDBIndex(String msg, Connection con, int flag){
        // flag meaning: 1: Broadcast asked, 2: ask for leader's index, 3: check its max DB index by itself.
        conn = con;
        try{
            // Connect to the local database
            sqlConnection = DriverManager.getConnection(sqlLogUrl);

            // Select the largest ID from Log database
            String sqlQuery = "SELECT MAX(LogId) AS MAX FROM Log;";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);

            int maxId = 0;

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
