package com.sh.payments.wallet.service.impl;

import com.sh.payments.wallet.exception.DuplicateResourceException;
import com.sh.payments.wallet.model.User;
import com.sh.payments.wallet.repository.UserRepository;
import com.sh.payments.wallet.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional           // ← ADD THIS
    public User createUser(User user) {

        if (userRepository.existsByUsername(user.getUsername())){
            throw new DuplicateResourceException(
                    "Username already taken: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())){
            throw new DuplicateResourceException(
                    "Email already registered: " + user.getEmail());

        }
        return userRepository.save(user);
    }
}