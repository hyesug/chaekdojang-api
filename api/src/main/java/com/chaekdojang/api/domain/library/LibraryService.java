package com.chaekdojang.api.domain.library;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookCategoryResolver;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.library.dto.LibraryAddRequest;
import com.chaekdojang.api.domain.library.dto.LibraryBookStatusResponse;
import com.chaekdojang.api.domain.library.dto.LibraryResponse;
import com.chaekdojang.api.domain.library.dto.LibraryUpdateRequest;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCategoryResolver bookCategoryResolver;

    @Transactional
    public LibraryResponse add(LibraryAddRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (libraryRepository.existsByUserIdAndBookId(userId, request.bookId())) {
            throw new CustomException(ErrorCode.LIBRARY_ALREADY_EXISTS);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        Library library = Library.builder()
                .user(user).book(book).status(request.status()).completedAt(request.completedAt())
                .build();
        Library saved = libraryRepository.save(library);
        return LibraryResponse.from(saved, bookCategoryResolver.resolve(saved.getBook()));
    }

    public List<LibraryResponse> getMyLibrary(LibraryStatus status) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Library> result = (status != null)
                ? libraryRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(userId, status)
                : libraryRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
        List<Library> limited = result.stream().limit(200).toList();
        Map<Book, String> categories = bookCategoryResolver.resolveAll(
                limited.stream().map(Library::getBook).toList());
        return limited.stream()
                .map(library -> LibraryResponse.from(library, categories.get(library.getBook())))
                .toList();
    }

    public List<LibraryResponse> getPublicFinishedLibrary(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        List<Library> libraries = libraryRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
        List<Book> reviewBooks = libraryRepository.findPublicReviewBooksByUserId(userId);
        List<Book> books = new java.util.ArrayList<>(libraries.stream().map(Library::getBook).toList());
        books.addAll(reviewBooks);
        Map<Book, String> categories = bookCategoryResolver.resolveAll(books);
        Map<Long, LibraryResponse> responses = new LinkedHashMap<>();
        libraries.stream()
                .map(library -> LibraryResponse.from(library, categories.get(library.getBook())))
                .forEach(response -> responses.putIfAbsent(response.book().id(), response));
        reviewBooks.stream()
                .map(book -> LibraryResponse.fromPublicReviewBook(book, categories.get(book)))
                .forEach(response -> responses.putIfAbsent(response.book().id(), response));
        return responses.values().stream().limit(200).toList();
    }

    @Transactional
    public LibraryResponse updateStatus(Long id, LibraryUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.LIBRARY_NOT_FOUND));
        library.updateStatus(request.status(), request.completedAt());
        return LibraryResponse.from(library, bookCategoryResolver.resolve(library.getBook()));
    }

    @Transactional
    public void remove(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.LIBRARY_NOT_FOUND));
        libraryRepository.delete(library);
    }

    public LibraryBookStatusResponse getBookStatus(Long bookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return libraryRepository.findByUserIdAndBookId(userId, bookId)
                .map(lib -> new LibraryBookStatusResponse(true, lib.getStatus(), lib.getId()))
                .orElse(new LibraryBookStatusResponse(false, null, null));
    }
}
