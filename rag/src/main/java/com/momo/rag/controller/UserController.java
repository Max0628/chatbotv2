package com.momo.rag.controller;

import com.momo.rag.dto.ApiResponse;
import com.momo.rag.dto.UserDto;
import com.momo.rag.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@CrossOrigin
@RestController
@RequestMapping("api/1.0")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto, HttpSession session) {
        String result = userService.register(userDto);
        if (result.equals("Registered successfully")) {
            session.setAttribute("user", userDto.getEmail());
            return ResponseEntity.ok(new ApiResponse("註冊成功並自動登入"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse("註冊失敗"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDto userDto, HttpSession session) {
        UserDto user = userService.login(userDto);
        if (user != null) {
            session.setAttribute("user", user.getEmail());
            session.setAttribute("username", user.getUsername());

            return ResponseEntity.ok(new ApiResponse("登入成功"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse("登入失敗"));
    }


    @PostMapping("logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(new ApiResponse("登出成功"));
    }

    @GetMapping("userinfo")
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        String userEmail = (String) session.getAttribute("user");
        String username = (String) session.getAttribute("username");
        if (userEmail != null && username != null) {
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("email", userEmail);
            userInfo.put("username", username);
            return ResponseEntity.ok(userInfo);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse("未登入"));
    }
}

