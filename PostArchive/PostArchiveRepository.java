package com.example.PostArchive;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostArchiveRepository
        extends MongoRepository<PostArchive,String> {
}
