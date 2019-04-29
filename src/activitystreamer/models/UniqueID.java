package activitystreamer.models;

public class UniqueID {
    private int lamportTimeStamp;
    private String serverID;

    public UniqueID(int lamportTimeStamp, String serverID){
        this.lamportTimeStamp = lamportTimeStamp;
        this.serverID = serverID;
    }

    public int getLamportTimeStamp(){
        return lamportTimeStamp;
    }

    public String getServerID(){
        return serverID;
    }

    // added in the evening of 04-28
    public void setServerID(String serverID){
        this.serverID = serverID;
    }

    public void setLamportTimeStamp(int lamportTimeStamp){
        this.lamportTimeStamp = lamportTimeStamp;
    }

    public void addLamportTimeStamp()
    {
        this.lamportTimeStamp += 1;
    }

    public boolean largerThan(UniqueID id2){
        if(this.getLamportTimeStamp() > id2.getLamportTimeStamp()){
            return true;
        }
        else if (this.getLamportTimeStamp() == id2.getLamportTimeStamp() && this.getServerID().compareTo(id2.getServerID()) > 0 ){
            return true;
        }
        return false;
    }

    public boolean equals(UniqueID id2){
        return (this.lamportTimeStamp == id2.getLamportTimeStamp() && this.serverID.equals(id2.getServerID()));
    }
}
