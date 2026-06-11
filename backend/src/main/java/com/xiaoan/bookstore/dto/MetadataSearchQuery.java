package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class MetadataSearchQuery {
    private String title;
    private String author;
    private String isbn;

    public boolean isEmpty() {
        return (title == null || title.isBlank())
                && (author == null || author.isBlank())
                && (isbn == null || isbn.isBlank());
    }

    public static MetadataSearchQuery ofTitle(String title) {
        MetadataSearchQuery q = new MetadataSearchQuery();
        q.setTitle(title);
        return q;
    }

    public static MetadataSearchQuery ofIsbn(String isbn) {
        MetadataSearchQuery q = new MetadataSearchQuery();
        q.setIsbn(isbn);
        return q;
    }

    public static MetadataSearchQuery of(String title, String author, String isbn) {
        MetadataSearchQuery q = new MetadataSearchQuery();
        q.setTitle(title);
        q.setAuthor(author);
        q.setIsbn(isbn);
        return q;
    }
}
