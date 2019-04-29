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

public class BuyTicket {
    private static Connection conn;
    private static java.sql.Connection sqlConnection;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl =
            "jdbc:sqlite:/Users/luchen/Documents/Documents/Melb_Uni_Life/Semester4/Distributed Algorithm/Project/sqliteDB/UserTest.db";

    public BuyTicket(String msg, Connection con){
        conn = con;
        try{

            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            int userId = (int)message.get("userId");
            int trainId = (int) message.get("trainNum");
            String purchaseTime = message.get("purchaseTime").toString();

            //First check if the user has already bought the ticket for this train
            String sqlCheck = "SELECT * FROM Ticket WHERE UserId ="+userId+" AND TrainId = "+trainId+";";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet checkResult = stmt.executeQuery(sqlCheck);
            if(checkResult.next()){
                // Sending back Purchase_Fail Info. The user has already bought the ticket
                String purchaseFail = Command.createPurchaseFail(trainId, userId, purchaseTime);
                conn.writeMsg(purchaseFail);
                log.debug(purchaseFail);
                closeConnection = false;
            }

            else{
                String sqlQuery = "SELECT LeftTickets FROM Train WHERE TrainId = "+ trainId + ";";
                ResultSet result = stmt.executeQuery(sqlQuery);

                int leftTicketsNum = 0;
                while(result.next()){
                    leftTicketsNum = result.getInt("LeftTickets");
                }
                if(leftTicketsNum > 0){
                    // Update the left tickets
                    String sqlUpdate = "UPDATE Train SET LeftTickets = ? WHERE TrainId = ? ;";
                    PreparedStatement pstmt = sqlConnection.prepareStatement(sqlUpdate);
                    pstmt.setInt(1, leftTicketsNum-1);
                    pstmt.setInt(2, trainId);
                    pstmt.executeUpdate();

                    // Update the Ticket Table
                    String sqlInsert = "INSERT INTO Ticket(UserId, TrainId, PurchaseTime) VALUES(?,?,?)";
                    PreparedStatement pstmt_2 = sqlConnection.prepareStatement(sqlInsert);
                    pstmt_2.setInt(1, userId);
                    pstmt_2.setInt(2, trainId);
                    pstmt_2.setString(3, purchaseTime);
                    pstmt_2.executeUpdate();
                    log.info("Adding the ticket to the ticket table");

                    // Sending back Purchase_Success Info
                    String purchaseSuccess = Command.createPurchaseSuccess(trainId, userId, purchaseTime);
                    conn.writeMsg(purchaseSuccess);
                    log.debug(purchaseSuccess);
                    closeConnection = false;
                }
                else{
                    // Sending back Purchase_Fail Info
                    String purchaseFail = Command.createPurchaseFail(trainId, userId, purchaseTime);
                    conn.writeMsg(purchaseFail);
                    log.debug(purchaseFail);
                    closeConnection = false;
                }
            }
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
