package com.example.library.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.mapper.BorrowRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BorrowTransactionService {

    @Autowired
    private BookService bookService;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Transactional
    public boolean doBorrowTransaction(Integer bookId, String borrowerName) {
        Book book = bookService.getById(bookId);
        if (book == null || book.getStatus() != 0) {
            return false;
        }
        book.setStatus(1);
        bookService.updateBook(book);

        BorrowRecord record = new BorrowRecord();
        record.setBookId(bookId);
        record.setBorrowerName(borrowerName);
        int rows = borrowRecordMapper.insert(record);
        if (rows <= 0) {
            throw new RuntimeException("插入借阅记录失败");
        }

        return true;
    }

    @Transactional
    public boolean returnBook(Integer bookId, String borrowerName) {
        QueryWrapper<BorrowRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", bookId)
                .eq("borrower_name", borrowerName)
                .isNull("return_time");
        BorrowRecord record = borrowRecordMapper.selectOne(wrapper);
        if (record == null) {
            return false;
        }
        record.setReturnTime(LocalDateTime.now());
        borrowRecordMapper.updateById(record);

        Book book = bookService.getById(bookId);
        book.setStatus(0);
        bookService.updateBook(book);

        return true;
    }
}