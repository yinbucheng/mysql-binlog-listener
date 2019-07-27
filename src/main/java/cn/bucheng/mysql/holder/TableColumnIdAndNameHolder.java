package cn.bucheng.mysql.holder;


import cn.bucheng.mysql.annotation.ColumnName;
import cn.bucheng.mysql.annotation.TableName;
import cn.bucheng.mysql.aware.BeanFactoryUtils;
import cn.bucheng.mysql.entity.TableBO;
import cn.bucheng.mysql.handle.FieldValueHandle;
import cn.bucheng.mysql.listener.IListener;
import cn.bucheng.mysql.utils.BinLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
public class TableColumnIdAndNameHolder implements CommandLineRunner {

    private Object listenerLock = new Object();

    private Map<String, FieldValueHandle> handleMap = new ConcurrentHashMap<>();

    private Map<String, LinkedList<IListener>> listeners = new ConcurrentHashMap<>(40);

    private Map<String, TableBO> cache = new HashMap<>(40);

    public static final String SQL = "select table_schema, table_name, column_name, ordinal_position from information_schema.columns";

    @Autowired
    private JdbcTemplate jdbcTemplate;


    private void register() {
        String[] beanNamesForTypes = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(IListener.class);
        if (beanNamesForTypes != null) {
            for (String beanName : beanNamesForTypes) {
                IListener bean = BeanFactoryUtils.getBeanFactory().getBean(beanName, IListener.class);
                Class<Object> classType = bean.getClassType();
                registerListener(classType, bean);
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
            log.error("please mark cn.bucheng.mysql.binloglistener.annotation.TableName annotation to entity");
            throw new RuntimeException("please mark cn.bucheng.mysql.binloglistener.annotation.TableName annotation to entity");
        }
        return annotation;
    }

    private String getKey(Class clazz) {
        TableName annotation = getTableName(clazz);
        String key = BinLogUtils.createKey(annotation.schema().toLowerCase(), annotation.table().toLowerCase());
        return key;
    }


    private void registerListener(Class clazz, IListener iListener) {
        log.info("begin apply sqlColumn to javaColumn and register listener");
        String key = getKey(clazz);
        applyMysqlColumnToJavaColumn(clazz, key);
        addListeners(key, iListener);
    }


    private void addListeners(String key, IListener listener) {
        LinkedList<IListener> iListeners = listeners.get(key);
        if (iListeners == null) {
            synchronized (listenerLock) {
                if (iListeners == null) {
                    iListeners = new LinkedList<>();
                    listeners.put(key,iListeners);
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
                ColumnName column = field.getAnnotation(ColumnName.class);
                String javaColumn = field.getName();
                String sqlColumn = javaColumn;
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
        log.info("finish apply " + key + " sqlColumn to javaColumn");
    }


    private void initTableIdAndColumn() {
        log.info("begin load table column id and name from mysql");
        //完成id到名称初始化
        jdbcTemplate.query(SQL, new RowMapper<Object>() {

            @Override
            public Object mapRow(ResultSet rs, int i) throws SQLException {
                int position = rs.getInt("ordinal_position");
                String tableName = rs.getString("table_name").toLowerCase();
                String dbName = rs.getString("table_schema").toLowerCase();
                String columnName = rs.getString("column_name");
                String key = BinLogUtils.createKey(dbName, tableName);
                TableBO tableBO = cache.get(key);
                if (tableBO == null) {
                    tableBO = new TableBO();
                    tableBO.setDbName(dbName);
                    tableBO.setTableName(tableName);
                    cache.put(key, tableBO);
                }
                tableBO.addColumnIdName(position - 1, columnName);
                return null;
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
