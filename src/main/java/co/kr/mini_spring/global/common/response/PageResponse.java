package co.kr.mini_spring.global.common.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean isFirst;
    private final boolean isLast;

    public PageResponse(List<T> content, int pageNumber, int pageSize, long totalElements, int totalPages, boolean hasNext) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.isFirst = (pageNumber == 0);
        this.isLast = !hasNext;
    }

    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.hasNext = page.hasNext();
        this.isFirst = page.isFirst();
        this.isLast = page.isLast();
    }
}