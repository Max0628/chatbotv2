package com.momo.rag.repository;

import com.momo.rag.dto.UserDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Log4j2
@Repository
public class UserDao {

    @Autowired
    private NamedParameterJdbcTemplate template;

    public UserDto getUserByEmail(String email) {
        String sql = "SELECT * FROM `user` WHERE email = :email";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("email", email);
        try {
            UserDto userDto = template.queryForObject(sql, params, new UserRowMapper());
            log.debug("User found for email: {}", email);
            return userDto;
        } catch (EmptyResultDataAccessException e) {
            log.debug("No user found for email: {}", email);
            return null;
        } catch (DataAccessException e) {
            log.error("Database access error when retrieving user by email: {}", email, e);
            throw new RuntimeException("Error retrieving user by email", e);
        }
    }


    public String register(UserDto userDto) {
        String sql = "INSERT INTO `user` (username,email,password) VALUES (:username,:email,:password)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", userDto.getUsername());
        params.addValue("email", userDto.getEmail());
        params.addValue("password", userDto.getPassword());

        int affectedRow = template.update(sql, params);
        return affectedRow > 0 ? "Registered successfully" : "Registration failed";
    }

}
