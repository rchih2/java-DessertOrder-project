package com.gtalent.elasticsearch.repository;

import com.gtalent.elasticsearch.model.Book;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface BookRepository extends ElasticsearchRepository<Book, String> {

    // 對應 GET /book/search?keyword=Spring -> Spring Data 自動產生的模糊查詢
    List<Book> findByTitleContaining(String title);
}