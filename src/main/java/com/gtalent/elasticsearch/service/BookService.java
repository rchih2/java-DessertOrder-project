package com.gtalent.elasticsearch.service;

import com.gtalent.elasticsearch.model.Book;

import java.util.List;

public interface BookService {

    Book save(Book book);

    Book update(String id, Book book);

    List<Book> search(String keyword);

    List<Book> searchCustom(String keyword);

    void deleteById(String id);
}