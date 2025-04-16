package InsertEMail;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import jakarta.servlet.annotation.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet("/DisplayDataServlet")
public class DisplayDataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LOG_DIR = System.getProperty("user.dir") + "/logs";
    private static final String LOG_FILE = LOG_DIR + "/log.txt";

    private void logToFile(String message) {
        try {
            java.nio.file.Path logPath = java.nio.file.Paths.get(LOG_DIR);
            if (!java.nio.file.Files.exists(logPath)) {
                java.nio.file.Files.createDirectories(logPath);
            }

            try (FileWriter fw = new FileWriter(LOG_FILE, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                pw.println("[" + timestamp + "] " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String searchQuery = request.getParameter("search");

        logToFile("[INFO] 接收到 GET 請求，搜尋條件：" + (searchQuery != null ? searchQuery : "無"));

        Connection connection = null;

        try {
            logToFile("[INFO] 嘗試連接到資料庫...");
            Context initContext = new InitialContext();
            DataSource dataSource = (DataSource) initContext.lookup("java:comp/env/jdbc/ires");
            connection = dataSource.getConnection();
            logToFile("[成功] 資料庫連線成功！");

            String sql = "SELECT id, username, email FROM users";
            PreparedStatement statement;

            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                sql += " WHERE username LIKE ? OR email LIKE ?";
                statement = connection.prepareStatement(sql);
                statement.setString(1, "%" + searchQuery + "%");
                statement.setString(2, "%" + searchQuery + "%");
            } else {
                statement = connection.prepareStatement(sql);
            }

            ResultSet resultSet = statement.executeQuery();
            JSONArray jsonArray = new JSONArray();

            while (resultSet.next()) {
                JSONObject userObj = new JSONObject();
                userObj.put("id", resultSet.getString("id"));
                userObj.put("username", resultSet.getString("username"));
                userObj.put("email", resultSet.getString("email"));
                jsonArray.put(userObj);
            }

            response.getWriter().write(jsonArray.length() > 0 ? jsonArray.toString() : "{\"error\": \"沒有找到任何用戶資料\"}");

        } catch (NamingException e) {
            logToFile("[ERROR] JNDI 查找失敗：" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"JNDI 查找失敗：" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logToFile("[ERROR] 伺服器錯誤：" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"伺服器錯誤：" + e.getMessage() + "\"}");
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                    logToFile("[INFO] 資料庫連線已關閉。");
                }
            } catch (Exception e) {
                logToFile("[ERROR] 資料庫連線關閉失敗：" + e.getMessage());
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        String action = request.getParameter("action");
        String id = request.getParameter("id");
        String username = request.getParameter("username");
        String email = request.getParameter("email");

        logToFile("[INFO] 接收到 POST 請求，動作：" + action);

        Connection connection = null;

        try {
            Context initContext = new InitialContext();
            DataSource dataSource = (DataSource) initContext.lookup("java:comp/env/jdbc/ires");
            connection = dataSource.getConnection();

            if ("update".equals(action)) {
                String updateSQL = "UPDATE users SET email = ? WHERE id = ?";
                PreparedStatement stmt = connection.prepareStatement(updateSQL);
                stmt.setString(1, email);
                stmt.setString(2, id);

                int rowsAffected = stmt.executeUpdate();
                response.getWriter().write(rowsAffected > 0 ? "{\"message\": \"更新成功\"}" : "{\"error\": \"更新失敗\"}");
            } else if ("delete".equals(action)) {
                String deleteSQL = "DELETE FROM users WHERE id = ?";
                PreparedStatement stmt = connection.prepareStatement(deleteSQL);
                stmt.setString(1, id);

                int rowsAffected = stmt.executeUpdate();
                response.getWriter().write(rowsAffected > 0 ? "{\"message\": \"刪除成功\"}" : "{\"error\": \"刪除失敗\"}");
            }

        } catch (Exception e) {
            logToFile("[ERROR] 操作失敗：" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"操作失敗：" + e.getMessage() + "\"}");
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                    logToFile("[INFO] 資料庫連線已關閉。");
                }
            } catch (Exception e) {
                logToFile("[ERROR] 資料庫連線關閉失敗：" + e.getMessage());
            }
        }
    }
}