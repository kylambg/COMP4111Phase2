package Object;

import com.fasterxml.jackson.annotation.JsonSetter;

public class Book {
    private String title;
    private String author;
    private String publisher;
    private int year;

    public Book(String title, String author, String publisher, int year) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.year = year;
    }

    ;

    public Book() {
    }

    ;

    public String getTitle() {
        return title;
    }

    @JsonSetter("Title")
    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    @JsonSetter("Author")
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    @JsonSetter("Publisher")
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getYear() {
        return year;
    }

    @JsonSetter("Year")
    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return ("title: " + title + " author: " + author + " publisher: " + publisher + " year: " + year);
    }
}