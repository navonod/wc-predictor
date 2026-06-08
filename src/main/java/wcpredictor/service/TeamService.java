package wcpredictor.service;

import org.springframework.stereotype.Service;
import wcpredictor.entity.Team;
import wcpredictor.repository.TeamRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAllByOrderByGroupLetterAscSortOrderAsc();
    }

    public List<Team> getGroupedTeams() {
        return teamRepository.findByGroupLetterIsNotNullOrderByGroupLetterAscSortOrderAsc();
    }

    public List<Team> getTeamsByGroup(String groupLetter) {
        return teamRepository.findByGroupLetterOrderBySortOrderAsc(groupLetter);
    }

    public Optional<Team> findById(UUID id) {
        return teamRepository.findById(id);
    }

    public Team save(Team team) {
        return teamRepository.save(team);
    }

    public void delete(UUID id) {
        teamRepository.deleteById(id);
    }
}
