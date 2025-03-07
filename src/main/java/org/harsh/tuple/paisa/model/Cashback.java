package org.harsh.tuple.paisa.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "cashbacks")
@Builder
public class Cashback {

    @Id
    private String id;
    private String userId;
    private double amount;
    private LocalDateTime timestamp;
}
