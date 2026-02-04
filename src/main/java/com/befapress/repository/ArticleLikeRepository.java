package com.befapress.repository;

import com.befapress.entity.ArticleLike;
import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    // Check if user has liked a news article
    boolean existsByUserAndNews(User user, News news);

    // Check if user has liked an opinion
    boolean existsByUserAndOpinion(User user, Opinion opinion);

    // Find like by user and news
    Optional<ArticleLike> findByUserAndNews(User user, News news);

    // Find like by user and opinion
    Optional<ArticleLike> findByUserAndOpinion(User user, Opinion opinion);

    // Count likes for a news article
    long countByNews(News news);

    // Count likes for an opinion
    long countByOpinion(Opinion opinion);

    // Delete by user and news
    void deleteByUserAndNews(User user, News news);

    // Delete by user and opinion
    void deleteByUserAndOpinion(User user, Opinion opinion);

    // Count likes for news by ID
    @Query("SELECT COUNT(l) FROM ArticleLike l WHERE l.news.id = :newsId")
    long countByNewsId(@Param("newsId") Long newsId);

    // Count likes for opinion by ID
    @Query("SELECT COUNT(l) FROM ArticleLike l WHERE l.opinion.id = :opinionId")
    long countByOpinionId(@Param("opinionId") Long opinionId);
}
