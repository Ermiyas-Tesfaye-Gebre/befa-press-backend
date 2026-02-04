package com.befapress.service;

import com.befapress.dto.response.BookmarkResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.entity.Bookmark;
import com.befapress.entity.News;
import com.befapress.entity.Opinion;
import com.befapress.entity.User;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.BookmarkRepository;
import com.befapress.repository.NewsRepository;
import com.befapress.repository.OpinionRepository;
import com.befapress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final OpinionRepository opinionRepository;

    /**
     * Add a news article to bookmarks
     */
    @Transactional
    public BookmarkResponse bookmarkNews(Long newsId, String userEmail) {
        User user = getUserByEmail(userEmail);
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));

        // Check if already bookmarked
        if (bookmarkRepository.existsByUserAndNews(user, news)) {
            throw new IllegalStateException("News already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .news(news)
                .build();

        Bookmark saved = bookmarkRepository.save(bookmark);
        return mapToResponse(saved);
    }

    /**
     * Remove a news article from bookmarks
     */
    @Transactional
    public void unbookmarkNews(Long newsId, String userEmail) {
        User user = getUserByEmail(userEmail);
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", newsId));

        bookmarkRepository.deleteByUserAndNews(user, news);
    }

    /**
     * Add an opinion to bookmarks
     */
    @Transactional
    public BookmarkResponse bookmarkOpinion(Long opinionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", opinionId));

        // Check if already bookmarked
        if (bookmarkRepository.existsByUserAndOpinion(user, opinion)) {
            throw new IllegalStateException("Opinion already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .opinion(opinion)
                .build();

        Bookmark saved = bookmarkRepository.save(bookmark);
        return mapToResponse(saved);
    }

    /**
     * Remove an opinion from bookmarks
     */
    @Transactional
    public void unbookmarkOpinion(Long opinionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Opinion opinion = opinionRepository.findById(opinionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinion", "id", opinionId));

        bookmarkRepository.deleteByUserAndOpinion(user, opinion);
    }

    /**
     * Check if a news article is bookmarked
     */
    public boolean isNewsBookmarked(Long newsId, String userEmail) {
        User user = getUserByEmail(userEmail);
        News news = newsRepository.findById(newsId).orElse(null);
        if (news == null)
            return false;
        return bookmarkRepository.existsByUserAndNews(user, news);
    }

    /**
     * Check if an opinion is bookmarked
     */
    public boolean isOpinionBookmarked(Long opinionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Opinion opinion = opinionRepository.findById(opinionId).orElse(null);
        if (opinion == null)
            return false;
        return bookmarkRepository.existsByUserAndOpinion(user, opinion);
    }

    /**
     * Get all bookmarks for a user (paginated)
     */
    public PageResponse<BookmarkResponse> getMyBookmarks(String userEmail, int page, int size) {
        User user = getUserByEmail(userEmail);
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        List<BookmarkResponse> bookmarks = bookmarkPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<BookmarkResponse>builder()
                .content(bookmarks)
                .page(bookmarkPage.getNumber())
                .size(bookmarkPage.getSize())
                .totalElements(bookmarkPage.getTotalElements())
                .totalPages(bookmarkPage.getTotalPages())
                .first(bookmarkPage.isFirst())
                .last(bookmarkPage.isLast())
                .build();
    }

    /**
     * Get news bookmarks only
     */
    public PageResponse<BookmarkResponse> getMyNewsBookmarks(String userEmail, int page, int size) {
        User user = getUserByEmail(userEmail);
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkRepository.findNewsBookmarksByUser(user, pageable);

        List<BookmarkResponse> bookmarks = bookmarkPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<BookmarkResponse>builder()
                .content(bookmarks)
                .page(bookmarkPage.getNumber())
                .size(bookmarkPage.getSize())
                .totalElements(bookmarkPage.getTotalElements())
                .totalPages(bookmarkPage.getTotalPages())
                .first(bookmarkPage.isFirst())
                .last(bookmarkPage.isLast())
                .build();
    }

    /**
     * Get opinion bookmarks only
     */
    public PageResponse<BookmarkResponse> getMyOpinionBookmarks(String userEmail, int page, int size) {
        User user = getUserByEmail(userEmail);
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkRepository.findOpinionBookmarksByUser(user, pageable);

        List<BookmarkResponse> bookmarks = bookmarkPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<BookmarkResponse>builder()
                .content(bookmarks)
                .page(bookmarkPage.getNumber())
                .size(bookmarkPage.getSize())
                .totalElements(bookmarkPage.getTotalElements())
                .totalPages(bookmarkPage.getTotalPages())
                .first(bookmarkPage.isFirst())
                .last(bookmarkPage.isLast())
                .build();
    }

    /**
     * Get IDs of bookmarked news for quick lookup
     */
    public List<Long> getBookmarkedNewsIds(String userEmail) {
        User user = getUserByEmail(userEmail);
        return bookmarkRepository.findBookmarkedNewsIdsByUser(user);
    }

    /**
     * Get IDs of bookmarked opinions for quick lookup
     */
    public List<Long> getBookmarkedOpinionIds(String userEmail) {
        User user = getUserByEmail(userEmail);
        return bookmarkRepository.findBookmarkedOpinionIdsByUser(user);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private BookmarkResponse mapToResponse(Bookmark bookmark) {
        BookmarkResponse.BookmarkResponseBuilder builder = BookmarkResponse.builder()
                .id(bookmark.getId())
                .createdAt(bookmark.getCreatedAt());

        if (bookmark.isNewsBookmark()) {
            News news = bookmark.getNews();
            builder.type("NEWS")
                    .itemId(news.getId())
                    .title(news.getTitle())
                    .excerpt(news.getExcerpt())
                    .slug(news.getSlug())
                    .imageUrl(news.getCoverImage())
                    .authorName(news.getAuthor() != null ? news.getAuthor().getFullName() : null)
                    .publishedAt(news.getPublishedAt());
        } else if (bookmark.isOpinionBookmark()) {
            Opinion opinion = bookmark.getOpinion();
            builder.type("OPINION")
                    .itemId(opinion.getId())
                    .title(opinion.getTitle())
                    .excerpt(opinion.getExcerpt())
                    .slug(opinion.getSlug())
                    .imageUrl(opinion.getCoverImage())
                    .authorName(opinion.getAuthor() != null ? opinion.getAuthor().getFullName() : null)
                    .publishedAt(opinion.getPublishedAt());
        }

        return builder.build();
    }
}
