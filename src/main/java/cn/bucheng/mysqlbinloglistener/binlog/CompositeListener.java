package cn.bucheng.mysqlbinloglistener.binlog;

import cn.bucheng.mysqlbinloglistener.entity.TableBO;
import cn.bucheng.mysqlbinloglistener.handle.FieldValueHandle;
import cn.bucheng.mysqlbinloglistener.holder.TableColumnIdAndNameHolder;
import cn.bucheng.mysqlbinloglistener.listener.IListener;
import cn.bucheng.mysqlbinloglistener.utils.BinLogUtils;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@Slf4j
public class CompositeListener implements BinaryLogClient.EventListener {
    //这里可以将dbName,tableName设置为成员变量主要和binlog有关，binlog能够保证事务插入的正确性
    private String dbName;
    private String tableName;
    @Autowired
    private TableColumnIdAndNameHolder holder;


    @Override
    public void onEvent(Event event) {
        EventType eventType = event.getHeader().getEventType();
        if (eventType == EventType.TABLE_MAP) {
            TableMapEventData mapData = event.getData();
            dbName = mapData.getDatabase().toLowerCase();
            tableName = mapData.getTable().toLowerCase();
        }
        String key = BinLogUtils.createKey(dbName.toLowerCase(), tableName.toLowerCase());
        if (eventType == EventType.EXT_WRITE_ROWS) {
            handleSave(key, event.getData());
        } else if (eventType == EventType.EXT_DELETE_ROWS) {
            handleUpdate(key, event.getData());
        } else if (eventType == EventType.EXT_UPDATE_ROWS) {
            handleDelete(key, event.getData());
        }
    }

    @SuppressWarnings("all")
    private void handleSave(String key, WriteRowsEventData data) {
        List<IListener> listeners = holder.getListenerByKey(key);
        if (listeners == null || listeners.size() == 0) {
            log.debug("not find save handle by key :" + key);
            return;
        }
        List<Serializable[]> rows = data.getRows();
        if (rows != null) {
            for (Serializable[] row : rows) {
                int length = row.length;
                Map<String, Serializable> result = new HashMap<>();
                TableBO tableBO = holder.getTableBO(key);
                for (int i = 0; i < length; i++) {
                    String name = tableBO.getJavaName(i);
                    result.put(name, row[i]);
                }
                Object entity = null;
                FieldValueHandle fieldValueHandle = holder.getFieldValueHandle(key);
                if (fieldValueHandle != null) {
                    entity = fieldValueHandle.handle(result);
                } else {
                    entity = BinLogUtils.decode(tableBO.getClazz(), result);
                    resetValueToEntity(entity, result);
                }
                for (IListener listener : listeners) {
                    listener.saveEvent(entity);
                }
            }
        }

    }

    @SuppressWarnings("all")
    private void handleUpdate(String key, UpdateRowsEventData data) {
        List<IListener> listeners = holder.getListenerByKey(key);
        if (listeners == null || listeners.size() == 0) {
            log.debug("not find update handle by key :" + key);
            return;
        }
        List<Map.Entry<Serializable[], Serializable[]>> rows = data.getRows();
        for (Map.Entry<Serializable[], Serializable[]> row : rows) {
            Serializable[] value = row.getValue();
            int length = value.length;
            Map<String, Serializable> result = new HashMap<>();
            TableBO tableBO = holder.getTableBO(key);
            for (int i = 0; i < length; i++) {
                String name = tableBO.getJavaName(i);
                result.put(name, value[i]);
            }
            Object entity = null;
            FieldValueHandle fieldValueHandle = holder.getFieldValueHandle(key);
            if (fieldValueHandle != null) {
                entity = fieldValueHandle.handle(result);
            } else {
                entity = BinLogUtils.decode(tableBO.getClazz(), result);
                resetValueToEntity(entity, result);
            }
            for (IListener listener : listeners) {
                listener.updateEvent(entity);
            }
        }
    }

    private void handleDelete(String key, DeleteRowsEventData data) {
        List<IListener> listeners = holder.getListenerByKey(key);
        if (listeners == null || listeners.size() == 0) {
            log.debug("not find delete handle by key :" + key);
            return;
        }

        List<Serializable[]> rows = data.getRows();
        if (rows != null) {
            for (Serializable[] row : rows) {
                for (IListener listener : listeners) {
                    listener.deleteEvent(row[0]);
                }
            }
        }
    }

    //埋点方法，用于提供用户实现自定义设置,如果想用继承这个类并实现这个方法
    public void resetValueToEntity(Object entity, Map<String, Serializable> values) {

    }


}
