package wcpredictor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.Tournament;
import wcpredictor.repository.TournamentRepository;

import java.time.Instant;
import java.util.*;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> findById(UUID id) {
        return tournamentRepository.findById(id);
    }

    @Transactional
    public Tournament create(String name, String description) {
        Tournament t = new Tournament();
        t.setName(name);
        t.setDescription(description);
        t.setCreatedAt(Instant.now());
        return tournamentRepository.save(t);
    }

    @Transactional
    public void delete(UUID id) {
        tournamentRepository.deleteById(id);
    }
}
