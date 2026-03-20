package com.cloudrc.server.service;

import com.cloudrc.server.enums.UserRoles;
import com.cloudrc.server.exception.DuplicateResourceException;
import com.cloudrc.server.exception.InvalidCredentialsException;
import com.cloudrc.server.exception.ResourceNotFoundException;
import com.cloudrc.server.model.User;
import com.cloudrc.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private JwtService jwtService;

    public User register(String email, String password) {
        boolean doesExists = userRepository.existsByEmail(email);
        if (doesExists) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));

        user.setRole(UserRoles.USER);

        return userRepository.save(user);
    }

    public String login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);

        if(user.isEmpty()) {
            throw new ResourceNotFoundException("User not found: " + email);
        }

        if(!bCryptPasswordEncoder.matches(password,user.get().getPassword())) {
            throw new InvalidCredentialsException("Wrong password");
        }

        return  jwtService.generateToken(user.get());
    }
}
