package com.gtalent.elasticsearch.controller;

import com.gtalent.elasticsearch.model.Book;
import com.gtalent.elasticsearch.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/book")
public class BookController {

    private final BookService bookService;

    /**
     * POST /book
     * 新增一本書
     */
    @PostMapping
    public ResponseEntity<Book> addBook(@RequestBody Book book) {
        Book saved = bookService.save(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /book/{id}
     * 依 id 更新一本書
     */
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable String id, @RequestBody Book book) {
        Book updated = bookService.update(id, book);
        return ResponseEntity.ok(updated);
    }

    /**
     * GET /book/search?keyword=Spring
     * 使用 Spring Data 自動查詢方法
     */
    @GetMapping("/search")
    public ResponseEntity<List<Book>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(bookService.search(keyword));
    }

    /**
     * GET /book/search/custom?keyword=Spring5
     * 使用自訂 multi-match 查詢
     */
    @GetMapping("/search/custom")
    public ResponseEntity<List<Book>> searchCustom(@RequestParam String keyword) {
        return ResponseEntity.ok(bookService.searchCustom(keyword));
    }

    /**
     * DELETE /book/{id}
     * 依 id 刪除一本書
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable String id) {
        bookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}