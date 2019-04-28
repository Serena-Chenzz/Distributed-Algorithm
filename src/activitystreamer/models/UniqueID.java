package activitystreamer.models;

public class UniqueID {
    private int lamportTimeStamp;
    private int serverID;

    public UniqueID(int lamportTimeStamp, int serverID){
        this.lamportTimeStamp = lamportTimeStamp;
        this.serverID = serverID;
    }

    public int getLamportTimeStamp(){
        return lamportTimeStamp;
    }

    public int getServerID(){
        return serverID;
    }

    public boolean largerThan(UniqueID id2){
        if(this.getLamportTimeStamp() > id2.getLamportTimeStamp()){
            return true;
        }
        else if (this.getLamportTimeStamp() == id2.getLamportTimeStamp() && this.getServerID() > id2.getServerID()){
            return true;
        }
        return false;
    }

    public boolean equals(UniqueID id2){
        return (this.lamportTimeStamp == id2.getLamportTimeStamp() && this.serverID == id2.getServerID());
    }
}
