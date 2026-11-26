SpendWise Java Servlet starter

Files:
- ExpenseServlet.java : lists expenses, calculates dashboard totals, adds/updates/deletes expenses
- schema.sql          : MySQL tables for users and expenses

Integration assumptions:
1. Your DB helper is com.spendwise.util.DB and exposes DB.get() returning java.sql.Connection.
2. Login stores the signed-in user's numeric ID in session as "userId".
3. The dashboard JSP is /expenses/dashboard.jsp and reads request attributes:
   totalExpenses, expenseCount, monthlyExpenses, expenses.
4. Form posts to /expenses with action=add, update, or delete.
5. Use Jakarta Servlet API and a MySQL JDBC driver.

This is a starter backend based on the AdminServlet pattern you uploaded. It is not a complete deployable project until the DB helper, login servlet, JSP pages, and project configuration are connected.
