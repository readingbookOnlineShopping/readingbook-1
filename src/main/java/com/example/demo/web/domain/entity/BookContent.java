package com.example.demo.web.domain.entity;

import jakarta.persistence.*;

@Entity
public class BookContent extends BaseEntity {

    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;
    private String content;
}