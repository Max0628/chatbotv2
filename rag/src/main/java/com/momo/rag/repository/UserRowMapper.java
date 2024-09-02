package com.momo.rag.repository;

import com.momo.rag.dto.UserDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<UserDto> {

    @Override
    public UserDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserDto userDto = new UserDto();
        userDto.setId(rs.getInt("id"));
        userDto.setUsername(rs.getString("username"));
        userDto.setEmail(rs.getString("email"));
        userDto.setPassword(rs.getString("password"));
        return userDto;
    }
}
