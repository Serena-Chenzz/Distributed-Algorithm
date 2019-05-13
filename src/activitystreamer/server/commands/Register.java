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


public class Register {
    
    private static Connection conn;
    private static java.sql.Connection sqlConnection;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl =Settings.getSqlUrl();

    // Similar to BuyTicket Class, this class is used when a client wants to register into the system.
    public Register(String msg, Connection con, int flag, int index) {
        //Flag 1: From leader directly; Flag 2: From relayMsg; Flag 3: From other server's operation
        Register.conn = con;
        
         try {
            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            String username;
            String secret;
            if(flag ==1 || flag ==3){
                //Read in the username and secret
                username = message.get("username").toString();
                secret =  message.get("secret").toString();
            }
            else{
                //Read in the username and secret
                JSONObject relayMsg = (JSONObject) parser.parse(message.get("message").toString());
                username = relayMsg.get("username").toString();
                secret = relayMsg.get("secret").toString();
            }
            //Check whether this user has been registered in the sqlite database
            String sqlQuery = "SELECT * FROM User WHERE UserName = '"+ username + "';";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);

            if(result.next()) {
                // If this user has been registered, we directly send a register_failed because we don't allow
                // Duplicate username registration
                log.info("This username has already been registered. Please try another username");
                JSONObject registerFailed = Command.createRegisterFailed(username);
                if(flag == 1){
                    conn.writeMsg(registerFailed.toJSONString());
                    log.debug(registerFailed.toJSONString());
                    //Then the connection will be closed
                    closeConnection = true;
                }
                else if(flag == 2){
                    String clientConnection = message.get("clientConnection").toString();
                    String relayMsg = Command.createRelayMsg(clientConnection,registerFailed.toJSONString());
                    conn.writeMsg(relayMsg);
                    log.debug(relayMsg);
                    closeConnection = false;
                }

            }
            else {
                //Add this user to the local database and return register_success
                String sqlInsert = "INSERT INTO User(UserId, UserName, UserPassword) VALUES(?,?,?)";
                PreparedStatement pstmt = sqlConnection.prepareStatement(sqlInsert);
                pstmt.setInt(1, index);
                pstmt.setString(2, username);
                pstmt.setString(3, secret);
                pstmt.executeUpdate();
                log.info("Adding the user to the database");

                JSONObject registerSuccess = Command.createRegisterSuccess(username);
                if (flag == 1){
                    conn.writeMsg(registerSuccess.toJSONString());
                    log.debug(registerSuccess.toJSONString());
                    closeConnection = false;
                }
                else if(flag == 2){
                    String clientConnection = message.get("clientConnection").toString();
                    String relayMsg = Command.createRelayMsg(clientConnection,registerSuccess.toJSONString());
                    conn.writeMsg(relayMsg);
                    log.debug(relayMsg);
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
