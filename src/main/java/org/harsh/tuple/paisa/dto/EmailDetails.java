package org.harsh.tuple.paisa.dto;


import lombok.Data;


@Data
public class EmailDetails {
    private final String email;
    private final String subject;
    private final String body;
}
