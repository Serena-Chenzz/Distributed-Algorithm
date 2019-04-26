package activitystreamer.server.commands;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.server.Connection;

import java.sql.*;

public class Logout {
    
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static java.sql.Connection sqlConnection;
    private static final String sqlUrl =
            "jdbc:sqlite:/Users/luchen/Documents/Documents/Melb_Uni_Life/Semester4/Distributed Algorithm/Project/sqliteDB";


    public Logout(Connection con, String msg) {
        Logout.conn = con;
        closeConnection = true;

        try{
            String username = Settings.getUsername();
            String secret = Settings.getSecret();

            sqlConnection = DriverManager.getConnection(sqlUrl);
            String sqlUpdate = "UPDATE User SET LoggedInOrNot = 0 WHERE UserName = ? AND UserPassword = ?;";
            PreparedStatement pstmt = sqlConnection.prepareStatement(sqlUpdate);
            pstmt.setString(1, username);
            pstmt.setString(2, secret);
            // update
            pstmt.executeUpdate();
        }
        catch (SQLException e){
            log.debug(e);
        }

    }

    public boolean getResponse() {
        return closeConnection;
    }

}