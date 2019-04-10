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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;

public class Connection extends Thread {

    private static final Logger log = LogManager.getLogger();
    private static final long DISCONNECTION_TIME_LIMIT = 60000;  //60000 milliseconds
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
                // already closed?
                log.error("received exception closing the connection " + Settings.socketAddress(socket) + ": " + e);
            }
        }
    }

    public void run() {
        while (open){
            String data = "";
            try {
                //Start sending buffered messages
                Control.getInstance().activateMessageQueue(this.getRemoteId());
                //If Control.process() returns true, then while loop finishes
                while (!term && (data = inreader.readLine()) != null) {
                    //reset the starting time
                    this.timerStart = 0;
                    term = Control.getInstance().process(this, data);               
                }
//                if (getRemoteId().equals("10.0.0.42 5000") || getRemoteId().equals("10.0.0.42 3000")){
//                    log.debug("Sleep");
//                    try {
//                        Thread.sleep(120000);
//                        log.debug("Thread awake:" + getRemoteId());
//                        term = false;
//                        while (!term && (data = inreader.readLine()) != null) {
//                            //reset the starting time
//                            this.timerStart = 0;
//                            term = Control.getInstance().process(this, data);               
//                        }
//                    } catch (InterruptedException ex) {
//                        java.util.logging.Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
//                    } 
//                }
                Control.getInstance().connectionClosed(this);
                closeCon();
            }
            catch (SocketException e){
                if (term){
                    open=false;
                }
                //log.error("connection error: " + e.toString());
                //Start the timer
                if (this.timerStart == 0){
                    this.timerStart = System.currentTimeMillis();
                }
                else{
                    long timerEnd = System.currentTimeMillis();
                    if ((timerEnd - this.timerStart) > DISCONNECTION_TIME_LIMIT){
                        //close the connection, regard the server is crashed
                        open = false;
                    }
                }
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

