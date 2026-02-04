package com.befapress.repository;

import com.befapress.entity.Bookmark;
import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // Find bookmark by user and news
    Optional<Bookmark> findByUserAndNews(User user, News news);

    // Find bookmark by user and opinion
    Optional<Bookmark> findByUserAndOpinion(User user, Opinion opinion);

    // Check if user has bookmarked a news article
    boolean existsByUserAndNews(User user, News news);

    // Check if user has bookmarked an opinion
    boolean existsByUserAndOpinion(User user, Opinion opinion);

    // Get all bookmarks for a user
    Page<Bookmark> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Get news bookmarks for a user
    @Query("SELECT b FROM Bookmark b WHERE b.user = :user AND b.news IS NOT NULL ORDER BY b.createdAt DESC")
    Page<Bookmark> findNewsBookmarksByUser(@Param("user") User user, Pageable pageable);

    // Get opinion bookmarks for a user
    @Query("SELECT b FROM Bookmark b WHERE b.user = :user AND b.opinion IS NOT NULL ORDER BY b.createdAt DESC")
    Page<Bookmark> findOpinionBookmarksByUser(@Param("user") User user, Pageable pageable);

    // Delete by user and news
    void deleteByUserAndNews(User user, News news);

    // Delete by user and opinion
    void deleteByUserAndOpinion(User user, Opinion opinion);

    // Count bookmarks by user
    long countByUser(User user);

    // Get all bookmark IDs for a user (for quick lookup)
    @Query("SELECT b.news.id FROM Bookmark b WHERE b.user = :user AND b.news IS NOT NULL")
    List<Long> findBookmarkedNewsIdsByUser(@Param("user") User user);

    @Query("SELECT b.opinion.id FROM Bookmark b WHERE b.user = :user AND b.opinion IS NOT NULL")
    List<Long> findBookmarkedOpinionIdsByUser(@Param("user") User user);
}
