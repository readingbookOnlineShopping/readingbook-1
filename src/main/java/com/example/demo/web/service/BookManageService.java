package com.example.demo.web.service;

import com.example.demo.utils.ImageUploadUtil;
import com.example.demo.web.domain.entity.*;
import com.example.demo.web.dto.request.BookRegisterRequest;
import com.example.demo.web.dto.request.BookUpdateRequest;
import com.example.demo.web.dto.response.BookManageSearchResponse;
import com.example.demo.web.dto.response.BookUpdateResponse;
import com.example.demo.web.exception.BaseException;
import com.example.demo.web.exception.BaseResponseCode;
import com.example.demo.web.repository.*;
import com.example.demo.web.service.search.BookSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookManageService {

    private final BookService bookService;
    private final BookRepository bookRepository;
    private final CategoryService categoryService;
    private final ImageUploadUtil imageUploadUtil;
    private final BookGroupManageService bookGroupManagementService;
    private final BookContentRepository bookContentRepository;
    private final BookAuthorListRepository bookAuthorListRepository;
    private final AuthorRepository authorRepository;
    private final MemberRepository memberRepository;


    /**
     * 도서 등록 메소드
     *
     * @return bookId
     */

    public Page<BookManageSearchResponse> getAllBooks(Pageable pageable, Member seller) {

        return bookRepository.searchAllBooks(pageable, seller);
    }

    @Transactional
    public Long registerBook(BookRegisterRequest request, MultipartFile file, Long memberId) {

        Member seller = memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(BaseResponseCode.MEMBER_NOT_FOUND));

        validateForm(
                request.getTitle(), request.getIsbn(), request.getPublisher(),
                request.getPublishingDate(), request.getEbookPrice(), request.getCategoryId(),
                request.getBookGroupId()
        );

        String savedImageName = imageUploadUtil.uploadImage(file);

        Category category = getCategory(request.getCategoryId());
        BookGroup bookGroup = getBookGroup(request.getBookGroupId());

        Book book = Book.createBook(request, category, bookGroup, savedImageName, seller);
        bookRepository.save(book);

        request.getAuthors().stream().forEach(
                authorOrdinal -> {
                    Author author = authorRepository.findById(authorOrdinal.getAuthorId()).orElseThrow(() -> new BaseException(BaseResponseCode.NO_ID_EXCEPTION));
                    int ordinal = authorOrdinal.getOrdinal();

                    validateDuplicate(book, author, ordinal);

                    BookAuthorList bookAuthorList = BookAuthorList.createBookAuthorList(book, author, ordinal);
                    bookAuthorListRepository.save(bookAuthorList);
                }
        );
        return book.getId();
    }

    private void validateDuplicate(Book book, Author author, int ordinal) {
        if (bookAuthorListRepository.existsByBookAndAuthor(book, author)) {
            throw new BaseException(BaseResponseCode.BOOK_AUTHOR_ALREADY_EXIST);
        }
        if (bookAuthorListRepository.existsByBookAndOrdinal(book, ordinal)) {
            throw new BaseException(BaseResponseCode.BOOK_AUTHOR_ALREADY_EXIST);
        }
    }

    @Transactional
    public void updateOnSale(Member seller, Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BaseException(BaseResponseCode.NO_ID_EXCEPTION));
        if (!book.getSeller().equals(seller)) {
            throw new BaseException(BaseResponseCode.NO_AUTHORIZATION_FOR_BOOK);
        }
        book.updateOnSale();
    }

    @Transactional
    public void updateOffSale(Member seller, Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BaseException(BaseResponseCode.NO_ID_EXCEPTION));
        if (!book.getSeller().equals(seller)) {
            throw new BaseException(BaseResponseCode.NO_AUTHORIZATION_FOR_BOOK);
        }
        book.updateOffSale();
    }

    @Transactional(readOnly = true)
    public BookUpdateResponse getBook(Long bookId, Long sellerId) {
        Optional<Book> book = bookRepository.findById(bookId);

        if (book.isEmpty()) {
            return null;
        }
        if (!book.get().getSeller().getId().equals(sellerId)) {
            throw new BaseException(BaseResponseCode.NO_AUTHORIZATION_FOR_BOOK);
        }
        return book.map(BookUpdateResponse::new).get();
    }

    @Transactional(readOnly = true)
    private Category getCategory(Long categoryId) {
        Category category = categoryService.findCategory(categoryId);
        return category;
    }

    @Transactional(readOnly = true)
    private BookGroup getBookGroup(Long bookGroupId) {
        BookGroup bookGroup = null;
        if (bookGroupId != 0) {
            bookGroup = bookGroupManagementService.findBookGroupById(bookGroupId);
        }
        return bookGroup;
    }

    @Transactional(readOnly = true)
    public Page<BookManageSearchResponse> searchBook(String searchQuery, Pageable pageable, BookSearchCondition condition, Member seller) {
        Page<BookManageSearchResponse> bookManageSearchResponses = bookRepository.searchRegisteredBook(searchQuery, pageable, condition, seller);

        return bookManageSearchResponses;
    }

    @Transactional(readOnly = true)
    public List<BookManageSearchResponse> searchBookQuery(String searchQuery, Member seller) {
        return bookRepository.searchBookQuery(searchQuery, seller);
    }

    private void validateForm(String title, String isbn, String publisher, String publishingDate,
                              int ebookPrice, Long categoryId, Long bookGroupId) {

        if (title.trim().equals("") || title == null) {
            throw new IllegalArgumentException("제목을 입력해주세요");
        }

        if (isbn.trim().equals("") || isbn == null) {
            throw new IllegalArgumentException("isbn을 입력해주세요");
        }

        if (publisher.trim().equals("") || publisher == null) {
            throw new IllegalArgumentException("출판사를 입력해주세요");
        }

        if (publishingDate.trim().equals("") || publishingDate == null) {
            throw new IllegalArgumentException("출판일을 입력해주세요");
        }


        if (ebookPrice == 0) {
            throw new IllegalArgumentException("e-book 판매 가격을 입력해주세요");
        }

        if (categoryId == null) {
            throw new IllegalArgumentException("카테고리 아이디를 입력해주세요");
        }

    }

    /**
     * 도서의 이미지 수정 메소드
     *
     * @param file
     * @param bookId
     */
    public void updateBookImage(MultipartFile file, Long bookId) {
        Book book = bookService.findBook(bookId);
        String existingImageName = book.getSavedImageName();
        String updatedImageName = imageUploadUtil.updateImage(file, existingImageName);

        book.updateImage(updatedImageName);
    }

    /**
     * 도서의 내용 수정 메소드
     *
     * @param request
     * @param bookId
     */
    public void updateBookContent(BookUpdateRequest request, Long bookId) {
        validateForm(
                request.getTitle(), request.getIsbn(), request.getPublisher(),
                request.getPublishingDate(), request.getEbookPrice(), request.getCategoryId(),
                request.getBookGroupId()
        );

        Book book = bookService.findBook(bookId);

        Category category = getCategory(request.getCategoryId());
        BookGroup bookGroup = getBookGroup(request.getBookGroupId());

        book.updateContent(request, category, bookGroup);
    }

    public boolean deleteBook(Long bookId) {
        boolean hasBookContent = bookContentRepository.existsByBookId(bookId);
        if (hasBookContent == true) {
            throw new BaseException(BaseResponseCode.BOOK_CONTENT_EXIST);
        } // 북 컨텐츠 cascade 전이 고려.

        Book book = bookService.findBook(bookId);
        bookRepository.delete(book);
        return true;
    }
}
