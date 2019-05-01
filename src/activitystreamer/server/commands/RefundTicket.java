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
    private static final String sqlUrl =Settings.getSqlUrl();

    public RefundTicket(String msg, Connection con){
        conn = con;
        try{
            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            String username = message.get("username").toString();
            long trainLongId = (long) message.get("trainNum");
            int trainId = (int) trainLongId;
            String refundTime = message.get("refundTime").toString();

            int userId = 0;
            String sqlUserQuery = "SELECT * FROM User WHERE UserName = '"+ username + "';";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet resultUser = stmt.executeQuery(sqlUserQuery);

            while(resultUser.next()){
                userId = resultUser.getInt("UserId");
            }

            String sqlDelete = "DELETE FROM Ticket WHERE UserId = ? AND TrainId = ?";
            PreparedStatement pstmt = sqlConnection.prepareStatement(sqlDelete);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, trainId);
            pstmt.executeUpdate();
            log.info("Deleted the ticket from the ticket table");

            //Change the leftTicket in the Ticket Table
            String sqlQuery = "SELECT LeftTickets FROM Train WHERE TrainId = "+ trainId + ";";
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
            String refundSuccess = Command.createRefundSuccess(trainId, username, refundTime);
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
