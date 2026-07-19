package com.gtalent.elasticsearch.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.gtalent.elasticsearch.model.Book;
import com.gtalent.elasticsearch.repository.BookRepository;
import com.gtalent.elasticsearch.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    /**
     * PUT /book/{id}
     * 依 id 更新書籍內容，若該 id 不存在則拋出例外
     */
    @Override
    public Book update(String id, Book book) {
        if (!bookRepository.existsById(id)) {
            throw new IllegalArgumentException("找不到 id 為 " + id + " 的書籍，無法更新");
        }
        book.setId(id);
        return bookRepository.save(book);
    }

    /**
     * GET /book/search?keyword=xxx
     * 使用 Spring Data Elasticsearch 自動產生的方法查詢（title 模糊比對）
     */
    @Override
    public List<Book> search(String keyword) {
        return bookRepository.findByTitleContaining(keyword);
    }

    /**
     * GET /book/search/custom?keyword=xxx
     * 使用 ElasticsearchOperations 自訂 multi-match 查詢，
     * 同時比對 title / author / publisher / category 多個欄位
     */
    @Override
    public List<Book> searchCustom(String keyword) {
        Query multiMatchQuery = Query.of(q -> q
                .multiMatch(m -> m
                        .fields("title", "author", "publisher", "category")
                        .query(keyword)
                )
        );

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(multiMatchQuery)
                .build();

        SearchHits<Book> searchHits = elasticsearchOperations.search(nativeQuery, Book.class);

        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * DELETE /book/{id}
     */
    @Override
    public void deleteById(String id) {
        bookRepository.deleteById(id);
    }
}