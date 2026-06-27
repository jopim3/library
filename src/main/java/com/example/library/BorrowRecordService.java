package com.example.library;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowRecordService {

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private BookService bookService;

    // 借书（必须用事务）
    @Transactional
    public boolean borrowBook(Integer bookId, String borrowerName) {
        // 1. 查询图书是否存在且可借
        Book book = bookService.getById(bookId);
        if (book == null) {
            return false;
        }
        if (book.getStatus() != 0) {
            return false;  // 图书已借出
        }

        // 2. 修改图书状态为已借出
        book.setStatus(1);
        bookService.updateBook(book);

        // 3. 创建借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setBookId(bookId);
        record.setBorrowerName(borrowerName);
        record.setBorrowTime(LocalDateTime.now());
        record.setReturnTime(null);

        borrowRecordMapper.insert(record);
        int i = 1 / 0;  // 手动制造异常，触发事务回滚
        return true;

    }

    // 还书（必须用事务）
    @Transactional
    public boolean returnBook(Integer bookId, String borrowerName) {
        // 1. 查询借阅记录（未还的）
        QueryWrapper<BorrowRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", bookId)
                .eq("borrower_name", borrowerName)
                .isNull("return_time");
        BorrowRecord record = borrowRecordMapper.selectOne(wrapper);

        if (record == null) {
            return false;  // 没有借阅记录
        }

        // 2. 更新借阅记录：设置还书时间
        record.setReturnTime(LocalDateTime.now());
        borrowRecordMapper.updateById(record);

        // 3. 修改图书状态为可借
        Book book = bookService.getById(bookId);
        book.setStatus(0);
        bookService.updateBook(book);

        return true;
    }

    // 查询某本书的借阅记录
    public List<BorrowRecord> getRecordsByBookId(Integer bookId) {
        QueryWrapper<BorrowRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", bookId);
        return borrowRecordMapper.selectList(wrapper);
    }
}
