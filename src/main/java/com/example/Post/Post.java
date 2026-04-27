package com.example.Post;

import com.example.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
import java.util.Objects;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Product name is required")
    @Column(name = "product_name")
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer price;


    @NotBlank(message = "Picture URL is required")
    @Column(name = "pic_url")
    @URL
    private String picUrl;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false) //one user can hold many posts, but post can be held only by one user, we fetch user by its id.
    private User user;

    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.ACTIVE;

    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(id, post.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

