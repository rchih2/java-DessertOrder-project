package com.gtalent.dao;

import com.gtalent.db.DBConnectionPool;
import com.gtalent.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDAOImpl implements UserDAO {
    @Override
    public void save(User user) {
        String sql = "INSERT INTO orders (customer_name, phone, line_account, qty_cookie, qty_brownie, qty_basque, qty_lemon_tart, qty_lemon_cake, subtotal, shipping, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getCustomerName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getLineAccount());
            stmt.setInt(4, user.getQtyCookie());
            stmt.setInt(5, user.getQtyBrownie());
            stmt.setInt(6, user.getQtyBasque());
            stmt.setInt(7, user.getQtyLemonTart());
            stmt.setInt(8, user.getQtyLemonCake());
            stmt.setInt(9, user.getSubtotal());
            stmt.setInt(10, user.getShipping());
            stmt.setInt(11, user.getTotal());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("寫入失敗", e);
        }
    }
}
