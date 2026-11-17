package com.mizan.support;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="tickets")
public class Ticket {
    public enum Status { OPEN, IN_PROGRESS, RESOLVED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @Column(nullable=false, length=160) public String title;
    @Column(nullable=false, length=6000) public String description;
    @Column(nullable=false, length=120) public String environment;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) public Status status = Status.OPEN;
    @Column(nullable=false, length=30) public String category;
    @Column(length=16000) public String analysis;
    @Column(length=30) public String analysisSource;
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false) public Instant updatedAt;
    protected Ticket() {}
    Ticket(String title, String description, String environment, String category) {
        this.title=title; this.description=description; this.environment=environment; this.category=category;
        this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
}
