package com.momo.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private int id;
    private String username;
    private String email;
    private String password;
}


