package com.gudy.counter;

import java.sql.*;

public class jdbctest {
    private Connection connection(){
        Connection conn = null;

        try{
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:D:/counter.db";
            conn = DriverManager.getConnection(url);
            System.out.println("数据库连接成功\n");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("数据库连接失败！"+e.getMessage());
        }

        return conn;

    }
    public void selectAll(){
        String sql="Select * from t_user";
        try {
            Connection conn = this.connection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()){
                System.out.println(rs.getString("password"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        jdbctest app = new jdbctest();
        app.selectAll();

    }
}
