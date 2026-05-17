package com.app.Post;

import com.app.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer price;

    @Column(name = "pic_url")
    private String picUrl;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    private String status = "ACTIVE";

    @CreationTimestamp
    private LocalDateTime createdAt;
}
