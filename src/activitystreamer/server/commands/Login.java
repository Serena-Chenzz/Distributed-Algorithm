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

public class Login {
    
	 
    private Connection conn;
    private static java.sql.Connection sqlConnection;
	private String msg;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl = Settings.getSqlUrl();

    public Login(Connection con, String msg) {
    	this.conn = con;
    	this.msg = msg;
    	
    	try {
            sqlConnection = DriverManager.getConnection(sqlUrl);

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            String username = message.get("username").toString();
            String secret = message.get("secret").toString();

            //Checking the username and password
            String sqlQuery = "SELECT * FROM User WHERE UserName = '"+ username + "' AND UserPassword =  '" + secret +
                    "' AND LoggedInOrNot = 0;";
            Statement stmt  = sqlConnection.createStatement();
            ResultSet result = stmt.executeQuery(sqlQuery);

            if(result.next()) {
                //If this username and secret are correct,we send back a login_success
                JSONObject loginSuccess = Command.createLoginSuccess(username);
                conn.writeMsg(loginSuccess.toJSONString());
                closeConnection = false;
                Control.setConnectionClients(conn);

                //also change the login status for the user in the database
                int userId = result.getInt("UserId");
                String sqlUpdate = "UPDATE User SET LoggedInOrNot = 1 WHERE UserId = ? ;";
                PreparedStatement pstmt = sqlConnection.prepareStatement(sqlUpdate);
                pstmt.setInt(1, userId);
                // update
                pstmt.executeUpdate();

            }else { 
                //If this username and secret are not correct, we send a login_failed
                JSONObject loginFailed = Command.createLoginFailed(username);
                conn.writeMsg(loginFailed.toJSONString());
                closeConnection = true;
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



	public boolean getResponse() {
        return closeConnection;
    }

	public Connection getConnection() {
		return conn;
	}
}
