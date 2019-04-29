package activitystreamer.server.commands;

import java.util.ArrayList;
import java.util.List;

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

public class RefundTicket {
    private static Connection conn;
    private static java.sql.Connection sqlConnection;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl =
            "jdbc:sqlite:/Users/luchen/Documents/Documents/Melb_Uni_Life/Semester4/Distributed Algorithm/Project/sqliteDB/UserTest.db";

    public RefundTicket(String msg, Connection con){
        conn = con;
        try{
            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            int userId = (int)message.get("userId");
            int trainId = (int) message.get("trainNum");
            String refundTime = message.get("refundTime").toString();

            String sqlDelete = "DELETE FROM Ticket WHERE UserId = ? AND TrainId = ?";
            PreparedStatement pstmt = sqlConnection.prepareStatement(sqlDelete);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, trainId);
            pstmt.executeUpdate();
            log.info("Deleted the ticket from the ticket table");

            //Change the leftTicket in the Ticket Table
            String sqlQuery = "SELECT LeftTickets FROM Train WHERE TrainId = "+ trainId + ";";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);
            int leftTicketsNum = 0;
            while(result.next()){
                leftTicketsNum = result.getInt("LeftTickets");
            }

            // Update the left tickets
            String sqlUpdate = "UPDATE Train SET LeftTickets = ? WHERE TrainId = ? ;";
            PreparedStatement pstmt_2 = sqlConnection.prepareStatement(sqlUpdate);
            pstmt_2.setInt(1, leftTicketsNum+1);
            pstmt_2.setInt(2, trainId);
            pstmt_2.executeUpdate();

            // Return Refund_Success Msg
            String refundSuccess = Command.createRefundSuccess(trainId, userId, refundTime);
            conn.writeMsg(refundSuccess);
            log.debug(refundSuccess);
            closeConnection = false;

            sqlConnection.close();
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
