package com.spendwise.controller;

import com.spendwise.util.DB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;

@WebServlet(urlPatterns = {"/expenses", "/expenses/add", "/expenses/update", "/expenses/delete"})
public class ExpenseServlet extends HttpServlet {

    private long signedInUser(HttpServletRequest q, HttpServletResponse p) throws IOException {
        HttpSession session = q.getSession(false);
        Object value = session == null ? null : session.getAttribute("userId");
        if (value == null) {
            p.sendRedirect(q.getContextPath() + "/login.jsp");
            return -1;
        }
        return ((Number) value).longValue();
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p)
            throws IOException, ServletException {
        long userId = signedInUser(q, p);
        if (userId < 0) return;

        try (Connection c = DB.get()) {
            try (PreparedStatement s = c.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0), COUNT(*) FROM Expenses WHERE user_id=?")) {
                s.setLong(1, userId);
                try (ResultSet r = s.executeQuery()) {
                    r.next();
                    q.setAttribute("totalExpenses", r.getBigDecimal(1));
                    q.setAttribute("expenseCount", r.getInt(2));
                }
            }

            try (PreparedStatement s = c.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) FROM Expenses " +
                    "WHERE user_id=? AND YEAR(expense_date)=YEAR(CURRENT_DATE()) " +
                    "AND MONTH(expense_date)=MONTH(CURRENT_DATE())")) {
                s.setLong(1, userId);
                try (ResultSet r = s.executeQuery()) {
                    r.next();
                    q.setAttribute("monthlyExpenses", r.getBigDecimal(1));
                }
            }

            PreparedStatement s = c.prepareStatement(
                    "SELECT expense_id,title,category,amount,expense_date,description " +
                    "FROM Expenses WHERE user_id=? ORDER BY expense_date DESC, expense_id DESC");
            s.setLong(1, userId);
            q.setAttribute("expenses", s.executeQuery());
            q.getRequestDispatcher("/expenses/dashboard.jsp").forward(q, p);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p)
            throws IOException, ServletException {
        long userId = signedInUser(q, p);
        if (userId < 0) return;

        String action = q.getParameter("action");
        try (Connection c = DB.get()) {
            if ("add".equals(action)) {
                String title = q.getParameter("title");
                String category = q.getParameter("category");
                BigDecimal amount = new BigDecimal(q.getParameter("amount"));
                Date date = Date.valueOf(q.getParameter("expense_date"));
                String description = q.getParameter("description");

                if (title == null || title.isBlank() || amount.signum() <= 0)
                    throw new IllegalArgumentException("Enter a title and a valid amount");

                try (PreparedStatement s = c.prepareStatement(
                        "INSERT INTO Expenses(user_id,title,category,amount,expense_date,description) " +
                        "VALUES(?,?,?,?,?,?)")) {
                    s.setLong(1, userId);
                    s.setString(2, title.trim());
                    s.setString(3, category);
                    s.setBigDecimal(4, amount);
                    s.setDate(5, date);
                    s.setString(6, description);
                    s.executeUpdate();
                }
            } else if ("update".equals(action)) {
                long id = Long.parseLong(q.getParameter("expense_id"));
                BigDecimal amount = new BigDecimal(q.getParameter("amount"));
                if (amount.signum() <= 0) throw new IllegalArgumentException("Invalid amount");
                try (PreparedStatement s = c.prepareStatement(
                        "UPDATE Expenses SET title=?,category=?,amount=?,expense_date=?,description=? " +
                        "WHERE expense_id=? AND user_id=?")) {
                    s.setString(1, q.getParameter("title"));
                    s.setString(2, q.getParameter("category"));
                    s.setBigDecimal(3, amount);
                    s.setDate(4, Date.valueOf(q.getParameter("expense_date")));
                    s.setString(5, q.getParameter("description"));
                    s.setLong(6, id);
                    s.setLong(7, userId);
                    s.executeUpdate();
                }
            } else if ("delete".equals(action)) {
                long id = Long.parseLong(q.getParameter("expense_id"));
                try (PreparedStatement s = c.prepareStatement(
                        "DELETE FROM Expenses WHERE expense_id=? AND user_id=?")) {
                    s.setLong(1, id);
                    s.setLong(2, userId);
                    s.executeUpdate();
                }
            } else {
                throw new IllegalArgumentException("Invalid action");
            }
            p.sendRedirect(q.getContextPath() + "/expenses");
        } catch (SQLException | IllegalArgumentException e) {
            throw new ServletException(e);
        }
    }
}
