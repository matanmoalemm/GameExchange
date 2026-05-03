package com.example.Post;

import com.example.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PostMapper {
    void updatePostFromDto(PostUpdateDto dto, @MappingTarget Post post);
    @Mapping(source = "user.id", target = "userId")
    PostResponse toResponse(Post post);
    Post toEntity(PostRequest request);


}
