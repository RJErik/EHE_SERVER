package com.example.ehe_server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "email_change_request")
public class EmailChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_change_request_id")
    private Integer emailChangeRequestId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_token_id", nullable = false, unique = true)
    private VerificationToken verificationToken;

    @NotBlank
    @Email
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    @Column(name = "new_email", nullable = false)
    private String newEmail;

    public EmailChangeRequest() {
    }

    public EmailChangeRequest(VerificationToken verificationToken, String newEmail) {
        this.verificationToken = verificationToken;
        this.newEmail = newEmail;
    }

    public Integer getEmailChangeRequestId() {
        return emailChangeRequestId;
    }

    public void setEmailChangeRequestId(Integer emailChangeRequestId) {
        this.emailChangeRequestId = emailChangeRequestId;
    }

    public VerificationToken getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(VerificationToken verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    @Override
    public String toString() {
        return "EmailChangeRequest{" +
                "emailChangeRequestId=" + emailChangeRequestId +
                ", verificationTokenId=" + (verificationToken != null ? verificationToken.getVerificationTokenId() : null) +
                ", newEmail='" + newEmail + '\'' +
                '}';
    }
}
