package org.harsh.tuple.paisa.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users")
@Builder
public class User {

    @Id
    private String id;
    private String username;
    private String email;
    private String password;
}
