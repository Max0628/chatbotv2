package com.momo.rag.service;

import com.momo.rag.dto.UserDto;
import com.momo.rag.repository.UserDao;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class UserService {


    private final UserDao userDao;
    private final PasswordUtil passwordUtil;

    public UserService(UserDao userDao,PasswordUtil passwordUtil){
        this.userDao=userDao;
        this.passwordUtil=passwordUtil;
    }

    public String register(UserDto userDto) {

        if (userDto.getEmail() == null || userDto.getEmail().isEmpty()) {
            return "Email is required";
        }

        if (userDto.getUsername() == null || userDto.getUsername().isEmpty()) {
            return "Username is required";
        }

        if (userDto.getPassword() == null || userDto.getPassword().isEmpty()) {
            return "Password is required";
        }

        UserDto existingUser = userDao.getUserByEmail(userDto.getEmail());
        if (existingUser != null) {
            return "This email has already been registered";
        }
        userDto.setPassword(passwordUtil.hashPassword(userDto.getPassword()));
        return userDao.register(userDto);
    }

    public UserDto login(UserDto userDto) {
        UserDto user = userDao.getUserByEmail(userDto.getEmail());
        if (user == null) {
            return null;
        }

        boolean isPasswordValid = passwordUtil.checkPassword(userDto.getPassword(), user.getPassword());

        if (isPasswordValid) {
            return user;
        } else {
            return null;
        }
    }

}
