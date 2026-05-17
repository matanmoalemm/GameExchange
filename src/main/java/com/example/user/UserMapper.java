package com.example.user;

import com.example.Post.Post;
import com.example.Post.PostMapper;
import com.example.Post.PostResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {PostMapper.class})
public interface UserMapper {

    @Mapping(source = "username", target = "name")
    UserResponse toResponse(User user);

    @Mapping(source = "name", target = "username")
    User toEntity(UserRequest request);

    @Mapping(source = "name", target = "username")
    void updateUserFromDto(UserUpdateDTO dto, @MappingTarget User user);

    List<PostResponse> toPostResponses(List<Post> posts);
}
