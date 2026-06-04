package wcpredictor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.User;
import wcpredictor.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmailAddress(String emailAddress) {
        return userRepository.findByEmailAddress(emailAddress);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public boolean isActive(User user) {
        return user.getConfirmed() == null || user.getConfirmed();
    }

    public Optional<User> findByPasswordResetToken(String token) {
        return userRepository.findByPasswordResetToken(token);
    }
}
