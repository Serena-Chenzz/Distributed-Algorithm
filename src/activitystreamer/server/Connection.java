package activitystreamer.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.logging.Level;

import activitystreamer.models.Command;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;

public class Connection extends Thread {

    private static final Logger log = LogManager.getLogger();
    //private static final long DISCONNECTION_TIME_LIMIT = 1000;  //1000 milliseconds
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedReader inreader;
    private PrintWriter outwriter;
    private boolean open = false;
    private Socket socket;
    private boolean term = false;
    private String remoteId = "";
    //We record starting time to calculate the disconnection time
    private long timerStart = 0;
    

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    Connection(Socket socket) throws IOException {
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        inreader = new BufferedReader(new InputStreamReader(in));
        outwriter = new PrintWriter(out, true);
        this.socket = socket;
        open = true;

        //remoteId = socket.getInetAddress() + ":" + socket.getPort();
        start();
    }

    public String getRemoteId() {
        return remoteId;
    }
    
    public static String generateRemoteId(String inetAddress, String port) {
            return inetAddress + ":" + port;
    }
    
    // if the remote identification is the same, then we think the two conncetion
    // is the same
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof Connection) {
            Connection remoteCon = (Connection) obj;
            return this.getRemoteId().equals(remoteCon.getRemoteId());
        }
        return false;
    }

    /*
	 * returns true if the message was written, otherwise false
     */
    public boolean writeMsg(String msg) {
        if (open) {
            outwriter.println(msg);
            outwriter.flush();
            if (this.getRemoteId()!=null){
                System.out.println("\n--Sending "+msg);
            }
            else{
                System.out.println("\n--Sending "+msg+" to: "+this.getRemoteId());
            }
            return true;
        }
        return false;
    }

    public void closeCon() {
        if (open) {
            //log.info("closing connection " + Settings.socketAddress(socket));
            try {
                term = true;
                inreader.close();
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                log.error("received exception closing the connection " + Settings.socketAddress(socket) + ": " + e);
            }
        }
    }

    public void run() {
        while (open){
            String data = "";
            try {
                //If Control.process() returns true, then while loop finishes
                while (!term && (data = inreader.readLine()) != null) {
                    term = Control.getInstance().process(this, data);               
                }
                Control.getInstance().connectionClosed(this);
                closeCon();
            }
            catch (SocketException e){
                log.debug("socketException"+e.getMessage());
                if (term){
                    open=false;
                }

                if (remoteId.equals(Control.getInstance().getLeaderAddress()))
                {
                    log.info("Leader gets crushed.");
                    for (Connection connection:Control.getInstance().getNeighbors())
                    {
                        if(connection.remoteId.equals(Control.getInstance().getLeaderAddress()))
                        {
                            Control.getInstance().getNeighbors().remove(connection);
                            Control.getInstance().setAcceptedValue(null);
                            break;
                        }
                    }
                    Control.getInstance().clearAcceptor();
                    Control.setLeaderHasBeenDecided(false);
                    int myLargestIndexInDB = Control.getMyLargestDBIndex();
                    if (Control.getInstance().getNeighbors().size() > 0) {
                        log.info("Now making new selection.");
                        Control.getInstance().sendSelection(myLargestIndexInDB);
                    }
                    Control.cleanUnChosenLogs();
                }
                open = false;
            }
            
            catch (IOException e) {
                //log.error("connection " + Settings.socketAddress(socket) + " closed with exception: " + e);
                Control.getInstance().connectionClosed(this);
                open = false;
            }
            
        }
        log.error("connection " + Settings.socketAddress(socket) + " closed...");
        Control.getInstance().connectionClosed(this);
        closeCon();
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isOpen() {
        return open;
    }
}

