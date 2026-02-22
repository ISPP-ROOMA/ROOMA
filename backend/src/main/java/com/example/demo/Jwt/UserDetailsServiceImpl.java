package com.example.demo.Jwt;

import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userService.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return UserDetailsImpl.fromUserEntity(user);
    }
}



