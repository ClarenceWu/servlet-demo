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
    private static final String LOG_FILE = "D:/log.txt"; // 🔹 修改為你的真實路徑

    private void logToFile(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pw.println("[" + timestamp + "] " + message);
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

            // 根據搜尋條件建立 SQL 查詢
            String sql = "SELECT username, email FROM users";
            PreparedStatement statement;
            
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                sql += " WHERE username LIKE ? OR email LIKE ?";
                logToFile("[SQL] 執行搜尋 SQL：" + sql);
                statement = connection.prepareStatement(sql);
                statement.setString(1, "%" + searchQuery + "%");
                statement.setString(2, "%" + searchQuery + "%");
            } else {
                logToFile("[SQL] 執行查詢所有用戶 SQL：" + sql);
                statement = connection.prepareStatement(sql);
            }

            // 執行查詢
            ResultSet resultSet = statement.executeQuery();
            JSONArray jsonArray = new JSONArray();

            logToFile("[INFO] 開始解析 SQL 結果...");
            while (resultSet.next()) {
                JSONObject userObj = new JSONObject();
                userObj.put("username", resultSet.getString("username"));
                userObj.put("email", resultSet.getString("email"));
                jsonArray.put(userObj);
                logToFile("[INFO] 讀取到用戶：" + resultSet.getString("username") + " - " + resultSet.getString("email"));
            }

            // JSON 轉換與回應
            if (jsonArray.length() == 0) {
                logToFile("[INFO] 沒有找到任何用戶資料");
                response.getWriter().write("{\"error\": \"沒有找到任何用戶資料\"}");
            } else {
                logToFile("[成功] 成功回傳 JSON 數據：" + jsonArray.toString());
                response.getWriter().write(jsonArray.toString());
            }
            
        } catch (NamingException e) {
            logToFile("[ERROR] JNDI 查找失敗：" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"JNDI 查找失敗：" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logToFile("[ERROR] 伺服器錯誤：" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"伺服器錯誤：" + e.getMessage() + "\"}");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                    logToFile("[INFO] 資料庫連線已關閉。");
                } catch (Exception e) {
                    logToFile("[ERROR] 資料庫連線關閉失敗：" + e.getMessage());
                }
            }
        }
    }
}