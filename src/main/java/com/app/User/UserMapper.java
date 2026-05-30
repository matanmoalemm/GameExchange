package com.app.User;

import com.app.Post.Post;
import com.app.Post.PostMapper;
import com.app.Post.PostResponse;
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
