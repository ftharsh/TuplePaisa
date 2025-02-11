package org.harsh.tuple.paisa.repository;

import org.harsh.tuple.paisa.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    @Query(value = "{ 'username': { '$regex': ?0, '$options': 'i' } }", fields = "{ 'username' : 1, '_id' : 0 }")
    List<String> findUsernamesByQuery(String query);
}
