package cn.bucheng.mysql.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ：yinchong
 * @create ：2019/7/24 19:17
 * @description：
 * @modified By：
 * @version:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableBO {
    private String dbName;
    //对应java中的类型
    private Class clazz;
    private String tableName;
    //数据的下标和列名对应关系
    private Map<Integer, String> columnIdNameMaps = new HashMap<>(30);
    //数库的列名和java的字段名称对应关系
    private Map<String, String> columnTypeNameMaps = new HashMap<>(30);


    public void addColumnIdName(Integer id, String name) {
        columnIdNameMaps.put(id, name);
    }

    public String getColumnName(Integer id) {
        return columnIdNameMaps.get(id);
    }

    public String getJavaTypeName(String columnName) {
        return columnTypeNameMaps.get(columnName);
    }

    public void addJavaTypeName(String columnName, String typeName) {
        columnTypeNameMaps.put(columnName, typeName);
    }

    public String getJavaName(int id) {
        String s = columnIdNameMaps.get(id);
        return columnTypeNameMaps.get(s);
    }

}
