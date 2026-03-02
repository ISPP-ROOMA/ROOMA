package com.example.demo.Review;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReviewScheduler {

    private final ReviewService reviewService;

    public ReviewScheduler(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void autoPublishOldReviews() {
        reviewService.publishOldReviews();
    }
}
