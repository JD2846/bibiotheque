package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

@Data
public class JwtRequest {

    private String username;
    private String password;
}
