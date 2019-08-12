package cn.bucheng.mysql.callback;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author buchengyin
 * @create 2019/7/27 18:04
 * @describe
 */
public interface MysqlRowMapper {
    void mapRow(ResultSet rs) throws SQLException;
}
