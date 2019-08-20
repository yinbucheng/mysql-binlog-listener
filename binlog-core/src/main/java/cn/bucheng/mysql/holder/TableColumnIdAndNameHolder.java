package cn.bucheng.mysql.holder;


import cn.bucheng.mysql.annotation.ColumnName;
import cn.bucheng.mysql.annotation.TableName;
import cn.bucheng.mysql.aware.BeanFactoryUtils;
import cn.bucheng.mysql.callback.MysqlRowMapper;
import cn.bucheng.mysql.bo.TableBO;
import cn.bucheng.mysql.handle.FieldValueHandle;
import cn.bucheng.mysql.listener.IListener;
import cn.bucheng.mysql.util.BinLogUtils;
import cn.bucheng.mysql.util.JDBCUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.beans.Transient;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：yinchong
 * @create ：2019/7/24 19:13
 * @description：
 * @modified By：
 * @version:
 */
@Component
@Slf4j
@Order(-Integer.MAX_VALUE)
public class TableColumnIdAndNameHolder implements CommandLineRunner {

    public static final String ORDINAL_POSITION = "ordinal_position";
    public static final String TABLE_NAME = "table_name";
    public static final String TABLE_SCHEMA = "table_schema";
    public static final String COLUMN_NAME = "column_name";
    public static final char UPPER_A = 'A';
    public static final char UPPER_Z = 'Z';
    private Object listenerLock = new Object();

    private Map<String, FieldValueHandle> handleMap = new ConcurrentHashMap<>();

    private Map<String, LinkedList<IListener>> listeners = new ConcurrentHashMap<>(40);

    private Map<String, TableBO> cache = new HashMap<>(40);

    public static final String SQL = "select table_schema, table_name, column_name, ordinal_position from information_schema.columns";


    @Value("${mysql.binlog.host}")
    private String host;
    @Value("${mysql.binlog.port}")
    private Integer port;
    @Value("${mysql.binlog.username}")
    private String username;
    @Value("${mysql.binlog.password}")
    private String password;


    /**
     * 从spring的ioc容器中获取监听器以及特殊处理器并设置上去
     */
    private void register() {
        String[] beanNamesForTypes = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(IListener.class);
        if (beanNamesForTypes != null) {
            for (String beanName : beanNamesForTypes) {
                IListener bean = BeanFactoryUtils.getBeanFactory().getBean(beanName, IListener.class);
                Class<Object> classType = bean.getClassType();
                registerListener(classType, bean);
                log.info("finish register listener clazz:{}, listener:{}", classType.getName(), bean.getClass().getName());
            }
        }

        String[] handleNamesForTypes = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(FieldValueHandle.class);
        if (handleNamesForTypes != null) {
            for (String beanName : handleNamesForTypes) {
                FieldValueHandle bean = BeanFactoryUtils.getBeanFactory().getBean(beanName, FieldValueHandle.class);
                Class classType = bean.getClassType();
                String key = getKey(classType);
                handleMap.put(key, bean);
            }
        }

    }

    private TableName getTableName(Class clazz) {
        TableName annotation = (TableName) clazz.getAnnotation(TableName.class);
        if (annotation == null) {
            log.error("please mark TableName annotation to DO");
            throw new RuntimeException("please mark TableName annotation to DO");
        }
        return annotation;
    }

    private String getKey(Class clazz) {
        TableName annotation = getTableName(clazz);
        String key = BinLogUtils.createKey(annotation.schema().toLowerCase(), annotation.table().toLowerCase());
        return key;
    }


    private void registerListener(Class clazz, IListener iListener) {
        String key = getKey(clazz);
        if (!checkExist(key)) {
            return;
        }
        applyMysqlColumnToJavaColumn(clazz, key);
        addListeners(key, iListener);
    }

    private boolean checkExist(String key) {
        TableBO tableBO = cache.get(key);
        if (tableBO == null) {
            log.info("{} not register ,because not find mysql record", key);
            return false;
        }
        return true;
    }


    private void addListeners(String key, IListener listener) {
        LinkedList<IListener> iListeners = listeners.get(key);
        if (iListeners == null) {
            synchronized (listenerLock) {
                if (iListeners == null) {
                    iListeners = new LinkedList<>();
                    listeners.put(key, iListeners);
                }
            }
        }
        iListeners.add(listener);
    }

    //回设映射关系
    private void applyMysqlColumnToJavaColumn(Class clazz, String key) {
        TableBO tableBO = cache.get(key);
        tableBO.setClazz(clazz);
        Field[] fields = clazz.getDeclaredFields();
        if (fields != null) {
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getAnnotation(Transient.class) != null) {
                    continue;
                }
                ColumnName column = field.getAnnotation(ColumnName.class);
                String javaColumn = field.getName();
                String sqlColumn = javaFieldName2SqlColumn(javaColumn);
                if (column != null) {
                    if (!StringUtils.isEmpty(column.javaColumn())) {
                        javaColumn = column.javaColumn();
                    }
                    if (!StringUtils.isEmpty(column.sqlColumn())) {
                        sqlColumn = column.sqlColumn();
                    }
                }

                tableBO.addJavaTypeName(sqlColumn, javaColumn);
            }
        }
    }

    private String javaFieldName2SqlColumn(String fieldName) {
        if (Strings.isBlank(fieldName)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        char[] chars = fieldName.toCharArray();
        for (char ch : chars) {
            if (ch >= UPPER_A && ch <= UPPER_Z) {
                sb.append("_");
                sb.append((char) (ch + 32));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }


    /**
     * 进行初始表中索引和字段对应关系
     */
    private void initTableIdAndColumn() {
        log.info("begin load table column id and name from mysql");
        //完成id到名称初始化
        JDBCUtils.mysqlQuery("jdbc:mysql://" + host + ":" + port + "/mysql?serverTimezone=GMT%2B8", username, password, SQL, new MysqlRowMapper() {
            @Override
            public void mapRow(ResultSet rs) throws SQLException {
                int position = rs.getInt(ORDINAL_POSITION);
                String tableName = rs.getString(TABLE_NAME).toLowerCase();
                String dbName = rs.getString(TABLE_SCHEMA).toLowerCase();
                String columnName = rs.getString(COLUMN_NAME);
                String key = BinLogUtils.createKey(dbName, tableName);
                TableBO tableBO = cache.get(key);
                if (tableBO == null) {
                    tableBO = new TableBO();
                    tableBO.setDbName(dbName);
                    tableBO.setTableName(tableName);
                    cache.put(key, tableBO);
                }
                tableBO.addColumnIdName(position - 1, columnName);
            }
        });
    }


    public TableBO getTableBO(String key) {
        return cache.get(key);
    }

    @Override
    public void run(String... args) throws Exception {
        initTableIdAndColumn();
        register();
    }

    public List<IListener> getListenerByKey(String key) {
        return listeners.get(key);
    }

    public FieldValueHandle getFieldValueHandle(String key) {
        return handleMap.get(key);
    }
}
