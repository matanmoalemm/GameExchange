package com.example.user;


import com.example.Post.Post;
import com.example.Post.PostMapper;
import com.example.Post.PostRequest;
import com.example.Post.PostResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    void updateUserFromDto(UserUpdateDTO dto, @MappingTarget User user);
    @Mapping(source = "posts", target = "postResponses")
    UserResponse toResponse(User user);
    User toEntity(UserRequest request);
    List<PostResponse> toPostResponses(List<Post> posts);
    @Mapping(source = "user.id", target = "userId")
    PostResponse postToPostResponse(Post post);

}
