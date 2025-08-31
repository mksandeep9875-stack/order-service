package com.menon;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Principal {

    String state;
    String username;
    String type; // CUTOMER, VENDOR, ADMIN
}
