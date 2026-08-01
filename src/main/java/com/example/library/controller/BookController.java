package com.example.library.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import com.example.library.*;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.service.BookService;
import com.example.library.service.BorrowRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/book")
public class BookController {

    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private BookService bookService;
    @Autowired
    private BorrowRecordService borrowRecordService;


    @GetMapping("/list")
    public Result<List<Book>> listBooks() {
        return Result.success(bookService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Book> getBook(@PathVariable Integer id) {
        Jedis jedis = null;
        try {
            jedis = new Jedis("localhost", 6379);
            String key = "book:" + id;

            // 1. 先查缓存
            String cached = jedis.get(key);
            if (cached != null) {
                Book book = objectMapper.readValue(cached, Book.class);
                jedis.close();
                return Result.success(book);
            }

            // 2. 缓存没有，查数据库
            Book book = bookService.getById(id);
            if (book == null) {
                jedis.close();
                return Result.error("图书不存在");
            }

            // 3. 存入缓存（JSON 格式），60秒过期
            jedis.set(key, objectMapper.writeValueAsString(book));
            jedis.expire(key, 60);
            jedis.close();

            return Result.success(book);

        } catch (Exception e) {
            if (jedis != null) {
                jedis.close();
            }
            // Redis 出问题时降级到直接查数据库
            Book book = bookService.getById(id);
            if (book == null) {
                return Result.error("图书不存在");
            }
            return Result.success(book);
        }
    }

    @PostMapping("/add")
    public Result<String> addBook(@RequestBody Book book) {
        boolean success = bookService.addBook(book);
        if (success) {
            return Result.success("添加成功，ID为：" + book.getId());
        }
        return Result.error("添加失败");
    }

    @PutMapping("/update")
    public Result<String> updateBook(@RequestBody Book book) {
        boolean success = bookService.updateBook(book);
        if (success) {
            // 更新成功后删除缓存
            try (Jedis jedis = new Jedis("localhost", 6379)) {
                jedis.del("book:" + book.getId());
            } catch (Exception e) {
                // Redis 异常不影响主流程
            }
            return Result.success("修改成功");
        }
        return Result.error("修改失败，图书不存在");
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteBook(@PathVariable Integer id) {
        boolean success = bookService.deleteBook(id);
        if (success) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败，图书不存在");
    }

    @PostMapping("/borrow")
    public Result<String> borrowBook(@RequestParam Integer bookId, @RequestParam String borrowerName) {
        boolean success = borrowRecordService.borrowBook(bookId, borrowerName);
        if (success) {
            return Result.success("借书成功");
        }
        return Result.error("借书失败，图书不存在或已被借出");
    }

    @PostMapping("/return")
    public Result<String> returnBook(@RequestParam Integer bookId, @RequestParam String borrowerName) {
        boolean success = borrowRecordService.returnBook(bookId, borrowerName);
        if (success) {
            return Result.success("还书成功");
        }
        return Result.error("还书失败，没有找到对应的借阅记录");
    }

    @GetMapping("/records/{bookId}")
    public Result<List<BorrowRecord>> getRecords(@PathVariable Integer bookId) {
        return Result.success(borrowRecordService.getRecordsByBookId(bookId));
    }

    @GetMapping("/cache-test/{id}")
    public Result<String> cacheTest(@PathVariable Integer id) {
        // 1. 连接 Redis
        Jedis jedis = new Jedis("localhost", 6379);

        String key = "book:" + id;

        // 2. 先查缓存
        String cached = jedis.get(key);
        if (cached != null) {
            jedis.close();
            return Result.success("从缓存读取: " + cached);
        }

        // 3. 缓存没有，查数据库
        Book book = bookService.getById(id);
        if (book == null) {
            jedis.close();
            return Result.error("图书不存在");
        }

        // 4. 存入缓存，设置60秒过期
        jedis.set(key, book.toString());
        jedis.expire(key, 60);
        jedis.close();

        return Result.success("从数据库读取并缓存: " + book.toString());
    }



}
