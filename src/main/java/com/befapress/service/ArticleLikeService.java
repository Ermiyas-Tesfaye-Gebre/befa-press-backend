package com.befapress.service;

import com.befapress.entity.ArticleLike;
import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.entity.User;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.ArticleLikeRepository;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.OpinionRepository;
import com.befapress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleLikeService {

    private final ArticleLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final OpinionRepository opinionRepository;

    // ==================== NEWS LIKES ====================

    @Transactional
    public void likeNews(Long newsId, String userEmail) {
        User user = getUserByEmail(userEmail);
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));

        if (likeRepository.existsByUserAndNews(user, news)) {
            throw new IllegalStateException("Already liked");
        }

        ArticleLike like = ArticleLike.builder()
                .user(user)
                .news(news)
                .build();

        likeRepository.save(like);
    }

    @Transactional
    public void unlikeNews(Long newsId, String userEmail) {
        User user = getUserByEmail(userEmail);
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));

        likeRepository.deleteByUserAndNews(user, news);
    }

    public boolean isNewsLiked(Long newsId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null)
            return false;
        News news = newsRepository.findById(newsId).orElse(null);
        if (news == null)
            return false;
        return likeRepository.existsByUserAndNews(user, news);
    }

    public long getNewsLikeCount(Long newsId) {
        return likeRepository.countByNewsId(newsId);
    }

    // ==================== OPINION LIKES ====================

    @Transactional
    public void likeOpinion(Long opinionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", opinionId));

        if (likeRepository.existsByUserAndOpinion(user, opinion)) {
            throw new IllegalStateException("Already liked");
        }

        ArticleLike like = ArticleLike.builder()
                .user(user)
                .opinion(opinion)
                .build();

        likeRepository.save(like);
    }

    @Transactional
    public void unlikeOpinion(Long opinionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", opinionId));

        likeRepository.deleteByUserAndOpinion(user, opinion);
    }

    public boolean isOpinionLiked(Long opinionId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null)
            return false;
        Opinion opinion = opinionRepository.findById(opinionId).orElse(null);
        if (opinion == null)
            return false;
        return likeRepository.existsByUserAndOpinion(user, opinion);
    }

    public long getOpinionLikeCount(Long opinionId) {
        return likeRepository.countByOpinionId(opinionId);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
