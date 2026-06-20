package com.rominiki.waytohome.repository;
import com.rominiki.waytohome.entity.Favorite;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserAndListing(User user, Listing listing);
    void deleteByUserAndListing(User user, Listing listing);
    List<Favorite> findByUser(User user);
}