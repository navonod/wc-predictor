package wcpredictor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.Game;
import wcpredictor.entity.User;
import wcpredictor.repository.GameRepository;

import java.time.Instant;
import java.util.*;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public List<Game> findAll() {
        return gameRepository.findAll();
    }

    public Optional<Game> findById(UUID id) {
        return gameRepository.findById(id);
    }

    @Transactional
    public Game create(String name, String description) {
        Game game = new Game();
        game.setName(name);
        game.setDescription(description);
        game.setCreatedAt(Instant.now());
        return gameRepository.save(game);
    }

    @Transactional
    public Game save(Game game) {
        return gameRepository.save(game);
    }

    @Transactional
    public void delete(UUID id) {
        gameRepository.deleteById(id);
    }

    @Transactional
    public void addUser(Game game, User user) {
        if (!game.getUsers().contains(user)) {
            game.getUsers().add(user);
            gameRepository.save(game);
        }
    }

    @Transactional
    public void removeUser(Game game, User user) {
        game.getUsers().remove(user);
        gameRepository.save(game);
    }
}
