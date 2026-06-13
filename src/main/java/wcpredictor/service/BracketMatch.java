package wcpredictor.service;

import java.time.LocalDateTime;

public class BracketMatch {
    private final int matchNumber;
    private final LocalDateTime matchDate;
    private final String venue;
    private final String team1Name;
    private final String team2Name;

    public BracketMatch(int matchNumber, LocalDateTime matchDate, String venue,
                         String team1Name, String team2Name) {
        this.matchNumber = matchNumber;
        this.matchDate = matchDate;
        this.venue = venue;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
    }

    public int getMatchNumber() { return matchNumber; }
    public LocalDateTime getMatchDate() { return matchDate; }
    public String getVenue() { return venue; }
    public String getTeam1Name() { return team1Name; }
    public String getTeam2Name() { return team2Name; }
}
