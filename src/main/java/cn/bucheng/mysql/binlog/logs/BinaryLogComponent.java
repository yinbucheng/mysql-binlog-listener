package cn.bucheng.mysql.binlog.logs;

import cn.bucheng.mysql.callback.MysqlRowMapper;
import cn.bucheng.mysql.entity.BinLogBO;
import cn.bucheng.mysql.entity.TableBO;
import cn.bucheng.mysql.utils.BinLogUtils;
import cn.bucheng.mysql.utils.JDBCUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author ：yinchong
 * @create ：2019/7/29 12:40
 * @description： 列出所有的binlog及提交量
 * @modified By：
 * @version:
 */
@Component
public class BinaryLogComponent {
    @Value("${mysql.binlog.host}")
    private String host;
    @Value("${mysql.binlog.port}")
    private Integer port;
    @Value("${mysql.binlog.username}")
    private String username;
    @Value("${mysql.binlog.password}")
    private String password;
    final static String ALL_BINLOG_SQL = "show binary logs";
    final static String CURRENT_BINLOG_SQL = "show master status";

    //列出所有的binlog文件
    public List<BinLogBO> listBinaryLogs() {
        List<BinLogBO> result = new LinkedList<>();
        JDBCUtils.mysqlQuery("jdbc:mysql://" + host + ":" + port + "/mysql?serverTimezone=GMT%2B8", username, password, ALL_BINLOG_SQL, new MysqlRowMapper() {
            @Override
            public void mapRow(ResultSet rs) throws SQLException {
                String logName = rs.getString("Log_name");
                Long fileSize = rs.getLong("File_size");
                BinLogBO binLogBO = new BinLogBO(logName, fileSize);
                result.add(binLogBO);
            }
        });

        return result;
    }

    //获取当前正在生效的binlog文件
    public BinLogBO getCurrentBinaryLog() {
        BinLogBO binLogBO = new BinLogBO();
        JDBCUtils.mysqlQuery("jdbc:mysql://" + host + ":" + port + "/mysql?serverTimezone=GMT%2B8", username, password, CURRENT_BINLOG_SQL, new MysqlRowMapper() {
            @Override
            public void mapRow(ResultSet rs) throws SQLException {
                String logName = rs.getString("File");
                Long fileSize = rs.getLong("Position");
                binLogBO.setLogFile(logName);
                binLogBO.setFileSize(fileSize);
            }
        });

        return binLogBO;
    }
}
