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

public class BuyTicket {
    private static Connection conn;
    private static java.sql.Connection sqlConnection;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl = Settings.getSqlUrl();

    // This class is used when the client wants to buy the ticket from the system.
    // If this message is received directly by a leader, the leader will reply to the client directly.
    // If this message is relayed by another server to the leaser, the leader will reply to the server and the server
    // will relay the response back to the client.
    // If this message is received by a learner server, it will perform the operations to its DB and keep up with the
    // global database state.

    public BuyTicket(String msg, Connection con, int flag){
        //Flag 1: From leader directly; Flag 2: From relayMsg; Flag 3: From other server's operation
        conn = con;
        try{

            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            String username = "";
            long trainLongId = 0;
            int trainId= 0;
            String purchaseTime = "";

            if(flag == 1 || flag == 3){
                username = message.get("username").toString();
                trainLongId = (long)message.get("trainNum");
                trainId = (int) trainLongId;
                purchaseTime = message.get("purchaseTime").toString();
            }
            else if(flag == 2){
                JSONObject relayMsg = (JSONObject) parser.parse(message.get("message").toString());
                username = relayMsg.get("username").toString();
                trainLongId = (long)(relayMsg.get("trainNum"));
                trainId = (int) trainLongId;
                purchaseTime = relayMsg.get("purchaseTime").toString();
            }

            int userId = 0;
            String sqlUserQuery = "SELECT * FROM User WHERE UserName = '"+ username + "';";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet resultUser = stmt.executeQuery(sqlUserQuery);

            while(resultUser.next()){
                userId = resultUser.getInt("UserId");
            }

            //First check if the user has already bought the ticket for this train
            String sqlCheck = "SELECT * FROM Ticket WHERE UserId ="+userId+" AND TrainId = "+trainId+";";
            ResultSet checkResult = stmt.executeQuery(sqlCheck);
            if(checkResult.next()){
                // Sending back Purchase_Fail Info. The user has already bought the ticket
                String purchaseFail = Command.createPurchaseFail(trainId, username, purchaseTime);
                if(flag == 1){
                    conn.writeMsg(purchaseFail);
                    log.debug(purchaseFail);
                    closeConnection = false;
                }
                else if(flag == 2){
                    String clientConnection = message.get("clientConnection").toString();
                    String relayMsg = Command.createRelayMsg(clientConnection,purchaseFail);
                    conn.writeMsg(relayMsg);
                    log.debug(relayMsg);
                    closeConnection = false;
                }

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
                    String purchaseSuccess = Command.createPurchaseSuccess(trainId, username, purchaseTime);
                    if(flag == 1){
                        conn.writeMsg(purchaseSuccess);
                        log.debug(purchaseSuccess);
                        closeConnection = false;
                    }
                    else if(flag == 2){
                        String clientConnection = message.get("clientConnection").toString();
                        String relayMsg = Command.createRelayMsg(clientConnection,purchaseSuccess);
                        conn.writeMsg(relayMsg);
                        log.debug(relayMsg);
                        closeConnection = false;
                    }

                }
                else{
                    // Sending back Purchase_Fail Info
                    String purchaseFail = Command.createPurchaseFail(trainId, username, purchaseTime);
                    if(flag == 1){
                        conn.writeMsg(purchaseFail);
                        log.debug(purchaseFail);
                        closeConnection = false;
                    }
                    else if(flag == 2){
                        String clientConnection = message.get("clientConnection").toString();
                        String relayMsg = Command.createRelayMsg(clientConnection,purchaseFail);
                        conn.writeMsg(relayMsg);
                        log.debug(relayMsg);
                        closeConnection = false;
                    }
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
