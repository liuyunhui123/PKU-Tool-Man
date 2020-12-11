package edu.pku.PKUToolMan.Entity;

import org.springframework.context.annotation.Configuration;

@Configuration
public class User {
    private int id;
    private String nickname;
    private String password;
    private String email;
    private String phoneNum;
    private int coin;

    public User() {
    }

    public int getid() { return id; }
    public void setid(int id) { this.id = id; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNum() { return phoneNum; }
    public void setPhoneNum(String phoneNum) { this.phoneNum = phoneNum; }

    public int getCoin() { return coin; }
    public void setCoin(int coin) { this.coin = coin; }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + "\'" +
                ", nickname='" + nickname + "\'" +
                ", password='" + password + "\'" +
                ", email='" + email + "\'" +
                ", phoneNum='" + phoneNum + "\'" +
                ", coin='" + coin + "\'" +
                "}";
    }
}
