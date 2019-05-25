package be.kuleuven.ee5.eliasstalpaert.sosarlink;

public class RecyclerItem {
    private String satelliteName, timeDate;

    public RecyclerItem(String satelliteName, String timeDate) {
        this.satelliteName = satelliteName;
        this.timeDate = timeDate;
    }

    public String getSatelliteName() {
        return satelliteName;
    }

    public String getTimeDate() {
        return timeDate;
    }
}
