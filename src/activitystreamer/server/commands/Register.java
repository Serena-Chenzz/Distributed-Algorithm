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


public class Register {
    
    private static Connection conn;
    private static java.sql.Connection sqlConnection;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl =Settings.getSqlUrl();
    
    public Register(String msg, Connection con) {
        Register.conn = con;
        
         try {
            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            //Read in the username and secret
            String username = message.get("username").toString();
            String secret =  message.get("secret").toString();
            
            //Check whether this user has been registered in the sqlite database
            String sqlQuery = "SELECT * FROM User WHERE UserName = '"+ username + "';";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);


            if(result.next()) {
                //If this user has been registered, we directly send a register_failed
                log.info("This username has already been registered. Please try another username");
                JSONObject registerFailed = Command.createRegisterFailed(username);
                conn.writeMsg(registerFailed.toJSONString());
                log.debug(registerFailed.toJSONString());
                //The the connetion will be closed
                closeConnection = true;
            }
            //If this server has no connected servers, it will skip the broadcast part. And return the register success method
            else {
                //Add this user to registerList
                String sqlInsert = "INSERT INTO User(UserName, UserPassword) VALUES(?,?)";
                PreparedStatement pstmt = sqlConnection.prepareStatement(sqlInsert);
                pstmt.setString(1, username);
                pstmt.setString(2, secret);
                pstmt.executeUpdate();
                log.info("Adding the user to the database");

                JSONObject registerSuccess = Command.createRegisterSuccess(username);
                conn.writeMsg(registerSuccess.toJSONString());
                log.debug(registerSuccess.toJSONString());
                //Adding user to list of users logged in
                closeConnection = false;
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
