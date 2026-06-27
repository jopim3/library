package com.example.library;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/book")
public class BookController {

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
        Book book = bookService.getById(id);
        if (book == null) {
            return Result.error("图书不存在");
        }
        return Result.success(book);
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
}
