package com.app.Post;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PostMapper {
    @Mapping(source = "soldPrice", target = "soldValue")
    void updatePostFromDto(PostUpdateDto dto, @MappingTarget Post post);
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "soldValue", target = "soldPrice")
    PostResponse toResponse(Post post);
    Post toEntity(PostRequest request);


}
