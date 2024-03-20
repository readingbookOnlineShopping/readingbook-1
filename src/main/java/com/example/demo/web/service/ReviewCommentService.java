package com.example.demo.web.service;

import com.example.demo.web.domain.entity.Member;
import com.example.demo.web.domain.entity.Review;
import com.example.demo.web.domain.entity.ReviewComment;
import com.example.demo.web.repository.ReviewCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewCommentService {

    private final ReviewCommentRepository reviewCommentRepository;
    /**
     * 댓글 작성 메소드
     * @param member
     * @param review
     * @param content
     */
    public Long comment(Member member, Review review, String content) {
        validateForm(content);

        ReviewComment reviewComment = ReviewComment.createReviewComment(member, review, content);

        return reviewCommentRepository.save(reviewComment).getId();
    }

    private void validateForm(String content) {
        if (content == null || content.trim().equals("")) {
            throw new IllegalArgumentException("댓글을 입력해주세요");
        }

        if (content.length() > 2000) {
            throw new IllegalArgumentException("2000자 미만의 댓글을 남겨주세요.");
        }
    }
}

