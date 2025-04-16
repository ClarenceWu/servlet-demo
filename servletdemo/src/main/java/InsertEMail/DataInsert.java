package InsertEMail;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import jakarta.servlet.annotation.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet("/DataInsert")
public class DataInsert extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LOG_FILE = "D:/log.txt";

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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        JSONObject jsonResponse = new JSONObject();
        String username = request.getParameter("username");
        String email = request.getParameter("email");

        // **1. 基本檢查: 確保 username & email 非空**
        if (username == null || username.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "請輸入有效的名字與電子郵件！");
            logToFile("[錯誤] 無效的輸入: username=" + username + ", email=" + email);
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        Connection connection = null;

        try {
            logToFile("[開始] 嘗試連接到資料庫...");

            Context initContext = new InitialContext();
            DataSource dataSource = (DataSource) initContext.lookup("java:comp/env/jdbc/ires");
            connection = dataSource.getConnection();

            logToFile("[成功] 資料庫連線成功！");

            String sql = "INSERT INTO users (username, email) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            statement.setString(2, email);

            logToFile("[SQL] 準備執行: " + sql + " (username=" + username + ", email=" + email + ")");
            
            int rowsInserted = statement.executeUpdate();
            logToFile("[結果] 影響的行數: " + rowsInserted);

            if (rowsInserted > 0) {
                jsonResponse.put("success", true);
                jsonResponse.put("message", "資料已成功插入！");
                logToFile("[成功] 資料插入成功！");
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "資料插入失敗！");
                logToFile("[錯誤] 資料插入失敗！");
            }

        } catch (NamingException e) {
            logToFile("[錯誤] JNDI 查找失敗：" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "JNDI 錯誤：" + e.getMessage());
        } catch (Exception e) {
            logToFile("[錯誤] 伺服器錯誤：" + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "伺服器錯誤：" + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                    logToFile("[關閉] 資料庫連線已關閉。");
                } catch (Exception e) {
                    logToFile("[錯誤] 關閉連線時發生錯誤：" + e.getMessage());
                }
            }
        }

        response.getWriter().write(jsonResponse.toString());
    }
}