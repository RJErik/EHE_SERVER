package com.example.ehe_server.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "line")
public class Line {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Integer lineId;

    @ManyToOne
    @JoinColumn(name = "canvas_id", nullable = false)
    private Canvas canvas;

    @Column(name = "x1", nullable = false)
    private LocalDateTime x1;

    @Digits(integer = 10, fraction = 8)
    @Column(name = "y1", nullable = false, precision = 18, scale = 8)
    private BigDecimal y1;

    @Column(name = "x2", nullable = false)
    private LocalDateTime x2;

    @Digits(integer = 10, fraction = 8)
    @Column(name = "y2", nullable = false, precision = 18, scale = 8)
    private BigDecimal y2;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Min(1)
    @Max(10)
    @Column(name = "thickness", nullable = false)
    private Integer thickness;

    @Column(name = "creation_date", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime creationDate;

    // Getters and setters
    public Integer getLineId() {
        return lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public LocalDateTime getX1() {
        return x1;
    }

    public void setX1(LocalDateTime x1) {
        this.x1 = x1;
    }

    public BigDecimal getY1() {
        return y1;
    }

    public void setY1(BigDecimal y1) {
        this.y1 = y1;
    }

    public LocalDateTime getX2() {
        return x2;
    }

    public void setX2(LocalDateTime x2) {
        this.x2 = x2;
    }

    public BigDecimal getY2() {
        return y2;
    }

    public void setY2(BigDecimal y2) {
        this.y2 = y2;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getThickness() {
        return thickness;
    }

    public void setThickness(Integer thickness) {
        this.thickness = thickness;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
