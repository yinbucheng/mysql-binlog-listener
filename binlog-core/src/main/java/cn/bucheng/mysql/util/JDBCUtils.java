package cn.bucheng.mysql.util;

import cn.bucheng.mysql.callback.MysqlRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * @author buchengyin
 * @create 2019/7/27 17:57
 * @describe
 */
public class JDBCUtils {

    private static Logger logger = LoggerFactory.getLogger(JDBCUtils.class);

    public static void mysqlQuery(String url, String username, String password, String sql, MysqlRowMapper rowMapper) {
        Connection con = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            //getConnection()方法，连接MySQL数据库
            con = DriverManager.getConnection(url, username, password);
            if (con.isClosed())
                throw new RuntimeException("Succeeded connecting to the Database!");
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rowMapper.mapRow(rs);
            }
            rs.close();
        } catch (Exception e) {
            logger.error(e.toString());
            throw new RuntimeException(e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
