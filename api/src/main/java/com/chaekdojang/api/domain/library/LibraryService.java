package com.chaekdojang.api.domain.library;

import com.chaekdojang.api.domain.book.Book;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

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
        return LibraryResponse.from(libraryRepository.save(library));
    }

    public List<LibraryResponse> getMyLibrary(LibraryStatus status) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Library> result = (status != null)
                ? libraryRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(userId, status)
                : libraryRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
        return result.stream().limit(200).map(LibraryResponse::from).toList();
    }

    public List<LibraryResponse> getPublicFinishedLibrary(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return libraryRepository.findPublicFinishedByUserId(userId)
                .stream()
                .limit(200)
                .map(LibraryResponse::from)
                .toList();
    }

    @Transactional
    public LibraryResponse updateStatus(Long id, LibraryUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Library library = libraryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.LIBRARY_NOT_FOUND));
        library.updateStatus(request.status(), request.completedAt());
        return LibraryResponse.from(library);
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
