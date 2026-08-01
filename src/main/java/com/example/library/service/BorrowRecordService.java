package com.example.library.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.mapper.BorrowRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;


@Service
public class BorrowRecordService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired private BorrowTransactionService borrowTransactionService;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    public boolean borrowBook(Integer bookId, String borrowerName) {
        String lockKey = "lock:book:" + bookId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return borrowTransactionService.doBorrowTransaction(bookId, borrowerName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }

    public boolean returnBook(Integer bookId, String borrowerName) {
        // 还书逻辑并发冲突概率低，暂不加锁
        return borrowTransactionService.returnBook(bookId, borrowerName);
    }

    public List<BorrowRecord> getRecordsByBookId(Integer bookId) {
        QueryWrapper<BorrowRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", bookId)
                .orderByDesc("borrow_time");
        return borrowRecordMapper.selectList(wrapper);
    }
}