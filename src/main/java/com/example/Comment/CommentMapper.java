package com.example.Comment;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CommentMapper {

    @Mapping(source = "post.id", target = "postId")
    @Mapping(source = "user.id", target = "authorId")
    CommentResponse toResponse(Comment comment);
}
