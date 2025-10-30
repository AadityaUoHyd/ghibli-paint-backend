package org.aadi.ghibli_paint.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.aadi.ghibli_paint.entity.User;
import org.aadi.ghibli_paint.repository.GeneratedImageRepository;
import org.aadi.ghibli_paint.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GeneratedImageRepository generatedImageRepository;

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify current user is deleting their own account
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!user.getUsername().equals(currentUsername)) {
            throw new RuntimeException("Unauthorized to delete this account");
        }

        generatedImageRepository.deleteByUser(user);
        userRepository.delete(user);
    }
}
