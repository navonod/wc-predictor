package wcpredictor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.Pool;
import wcpredictor.entity.User;
import wcpredictor.repository.PoolRepository;

import java.time.Instant;
import java.util.*;

@Service
public class PoolService {

    private final PoolRepository poolRepository;

    public PoolService(PoolRepository poolRepository) {
        this.poolRepository = poolRepository;
    }

    public List<Pool> findAll() {
        return poolRepository.findAll();
    }

    public Optional<Pool> findById(UUID id) {
        return poolRepository.findById(id);
    }

    @Transactional
    public Pool create(String name, String description) {
        Pool pool = new Pool();
        pool.setName(name);
        pool.setDescription(description);
        pool.setCreatedAt(Instant.now());
        return poolRepository.save(pool);
    }

    @Transactional
    public Pool save(Pool pool) {
        return poolRepository.save(pool);
    }

    @Transactional
    public void delete(UUID id) {
        poolRepository.deleteById(id);
    }

    @Transactional
    public void addUser(Pool pool, User user) {
        if (!pool.getUsers().contains(user)) {
            pool.getUsers().add(user);
            poolRepository.save(pool);
        }
    }

    @Transactional
    public void removeUser(Pool pool, User user) {
        pool.getUsers().remove(user);
        poolRepository.save(pool);
    }
}
