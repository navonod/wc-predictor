package wcpredictor.service;

import org.springframework.stereotype.Service;
import wcpredictor.entity.Team;
import wcpredictor.entity.Tournament;
import wcpredictor.entity.TournamentTeam;
import wcpredictor.repository.TeamRepository;
import wcpredictor.repository.TournamentTeamRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TournamentTeamRepository tournamentTeamRepo;

    public TeamService(TeamRepository teamRepository, TournamentTeamRepository tournamentTeamRepo) {
        this.teamRepository = teamRepository;
        this.tournamentTeamRepo = tournamentTeamRepo;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Map<Character, List<Team>> getGroupedTeams(UUID tournamentId) {
        Map<Character, List<Team>> groups = new LinkedHashMap<>();
        var entries = tournamentTeamRepo
                .findByTournamentIdAndGroupLetterNotNullOrderByGroupLetterAscSortOrderAsc(tournamentId);
        for (var tt : entries) {
            char g = tt.getGroupLetter().charAt(0);
            groups.computeIfAbsent(g, k -> new ArrayList<>()).add(tt.getTeam());
        }
        return groups;
    }

    public List<Team> getTeamsByGroup(UUID tournamentId, String groupLetter) {
        return tournamentTeamRepo
                .findByTournamentIdAndGroupLetterOrderBySortOrderAsc(tournamentId, groupLetter)
                .stream().map(TournamentTeam::getTeam).collect(Collectors.toList());
    }

    public Optional<TournamentTeam> getTournamentTeam(UUID tournamentId, UUID teamId) {
        return tournamentTeamRepo.findByTournamentIdAndTeamId(tournamentId, teamId);
    }

    public Optional<Team> findById(UUID id) {
        return teamRepository.findById(id);
    }

    public Team save(Team team) {
        return teamRepository.save(team);
    }

    public TournamentTeam saveTournamentTeam(TournamentTeam tt) {
        return tournamentTeamRepo.save(tt);
    }

    public void delete(UUID id) {
        teamRepository.deleteById(id);
    }

    public List<Team> getTeamsByTournament(UUID tournamentId) {
        return tournamentTeamRepo.findByTournamentId(tournamentId).stream()
                .map(TournamentTeam::getTeam).collect(Collectors.toList());
    }
}
