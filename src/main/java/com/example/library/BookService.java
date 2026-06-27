package com.example.library;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookMapper bookMapper;

    public List<Book> listAll() {
        return bookMapper.selectList(null);
    }

    public Book getById(Integer id) {
        return bookMapper.selectById(id);
    }

    public boolean addBook(Book book) {
        return bookMapper.insert(book) > 0;
    }

    public boolean updateBook(Book book) {
        return bookMapper.updateById(book) > 0;
    }

    public boolean deleteBook(Integer id) {
        return bookMapper.deleteById(id) > 0;
    }
}