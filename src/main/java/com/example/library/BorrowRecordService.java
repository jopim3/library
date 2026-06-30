package com.example.library;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class BorrowRecordService {

    @Autowired
    private RedissonClient redissonClient;
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

    public boolean borrowBookWithLock(Integer bookId, String borrowerName) {
        String lockKey = "lock:book:" + bookId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试加锁，等待3秒，锁有效期10秒
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                // 加锁成功，执行借书逻辑
                Book book = bookService.getById(bookId);
                if (book == null || book.getStatus() != 0) {
                    return false;
                }
                book.setStatus(1);
                bookService.updateBook(book);

                BorrowRecord record = new BorrowRecord();
                record.setBookId(bookId);
                record.setBorrowerName(borrowerName);
                record.setBorrowTime(LocalDateTime.now());
                borrowRecordMapper.insert(record);
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }
}
