public class WakaHandler {
    public static String logicalTime(int currentTime, int departureTime) {
        int timeAway = departureTime - currentTime; // in seconds
        if (timeAway < 60) {
            return "less than 1m";
        } else {
            return (timeAway / 60) + "m";
        }
    }
}
