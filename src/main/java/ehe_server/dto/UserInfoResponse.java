package ehe_server.dto;

import java.util.Objects;

public class UserInfoResponse {
    private String userName;
    private String email;

    public UserInfoResponse() {}

    public UserInfoResponse(String userName, String email) {
        this.userName = userName;
        this.email = email;
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfoResponse that = (UserInfoResponse) o;
        return Objects.equals(userName, that.userName) &&
                Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, email);
    }
}