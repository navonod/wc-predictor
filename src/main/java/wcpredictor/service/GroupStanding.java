package wcpredictor.service;

import wcpredictor.entity.Team;

public class GroupStanding implements Comparable<GroupStanding> {

    private final Team team;
    private final int played;
    private final int won;
    private final int drawn;
    private final int lost;
    private final int goalsFor;
    private final int goalsAgainst;
    private final int goalDiff;
    private final int points;
    private final int position;

    public GroupStanding(Team team, int played, int won, int drawn, int lost,
                         int goalsFor, int goalsAgainst, int position) {
        this.team = team;
        this.played = played;
        this.won = won;
        this.drawn = drawn;
        this.lost = lost;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.goalDiff = goalsFor - goalsAgainst;
        this.points = won * 3 + drawn;
        this.position = position;
    }

    public Team getTeam() { return team; }
    public int getPlayed() { return played; }
    public int getWon() { return won; }
    public int getDrawn() { return drawn; }
    public int getLost() { return lost; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public int getGoalDiff() { return goalDiff; }
    public int getPoints() { return points; }
    public int getPosition() { return position; }

    @Override
    public int compareTo(GroupStanding o) {
        if (this.points != o.points) return Integer.compare(o.points, this.points);
        if (this.goalDiff != o.goalDiff) return Integer.compare(o.goalDiff, this.goalDiff);
        if (this.goalsFor != o.goalsFor) return Integer.compare(o.goalsFor, this.goalsFor);
        return 0;
    }

    @Override
    public String toString() {
        return team.getName() + " (" + points + "pts, GD:" + goalDiff + ")";
    }
}
