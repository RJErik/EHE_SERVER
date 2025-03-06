package com.example.ehe_server.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "text_annotation")
public class TextAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "text_id")
    private Integer textId;

    @ManyToOne
    @JoinColumn(name = "canvas_id", nullable = false)
    private Canvas canvas;

    @Column(name = "x", nullable = false)
    private LocalDateTime x;

    @Digits(integer = 10, fraction = 8)
    @Column(name = "y", nullable = false, precision = 18, scale = 8)
    private BigDecimal y;

    @Size(min = 1, max = 500)
    @NotBlank
    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Min(8)
    @Max(72)
    @Column(name = "font_size", nullable = false)
    private Integer fontSize;

    @Column(name = "creation_date", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime creationDate;

    // Getters and setters
    public Integer getTextId() {
        return textId;
    }

    public void setTextId(Integer textId) {
        this.textId = textId;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public LocalDateTime getX() {
        return x;
    }

    public void setX(LocalDateTime x) {
        this.x = x;
    }

    public BigDecimal getY() {
        return y;
    }

    public void setY(BigDecimal y) {
        this.y = y;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
