package com.example.PostArchive;

import com.example.Post.Post;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Objects;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "PostArchive")
public class PostArchive {

    @Id
    private String id; // ב-Mongo נהוג להשתמש ב-String עבור ה-ObjectID

    private Integer originalPostId;
    private String productName;
    private Integer price;
    private String picUrl;

    private Integer sellerId;
    private String sellerName;
    private Integer buyerId;
    private String buyerName;

    private LocalDateTime archivedAt;
    private Reason reason;


    public PostArchive(Post post, Reason reason, Integer buyerId, String buyerName) {
        this.originalPostId = post.getId();
        this.productName = post.getProductName();
        this.price = post.getPrice();
        this.picUrl = post.getPicUrl();
        this.sellerId = post.getUser().getId();
        this.sellerName = post.getUser().getUsername();
        this.archivedAt = LocalDateTime.now();
        this.reason = reason;

        this.buyerId = buyerId;
        this.buyerName = buyerName;

    }



    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PostArchive that = (PostArchive) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
