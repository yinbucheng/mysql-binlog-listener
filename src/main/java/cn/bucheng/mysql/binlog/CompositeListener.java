package cn.bucheng.mysql.binlog;

import cn.bucheng.mysql.aware.BeanFactoryUtils;
import cn.bucheng.mysql.callback.BinLogCommitPosition;
import cn.bucheng.mysql.entity.TableBO;
import cn.bucheng.mysql.handle.FieldValueHandle;
import cn.bucheng.mysql.holder.TableColumnIdAndNameHolder;
import cn.bucheng.mysql.listener.IBinLogFileListener;
import cn.bucheng.mysql.listener.IListener;
import cn.bucheng.mysql.utils.BinLogUtils;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
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
        if (eventType == EventType.ROTATE) {
            RotateEventData data = event.getData();
            IBinLogFileListener binLogFileListener = getBinLogFileListener();
            if (binLogFileListener != null) {
                binLogFileListener.handleBinLogFile(data.getBinlogFilename());
            }
            return;
        }
        if (eventType == EventType.TABLE_MAP) {
            TableMapEventData mapData = event.getData();
            dbName = mapData.getDatabase().toLowerCase();
            tableName = mapData.getTable().toLowerCase();
            return;
        }
        if (Strings.isBlank(dbName) || Strings.isBlank(tableName)) {
            return;
        }
        if (eventType == EventType.XID) {
            handleCommitPosition(event.getHeader());
            return;
        }
        String key = BinLogUtils.createKey(dbName.toLowerCase(), tableName.toLowerCase());
        if (eventType == EventType.EXT_WRITE_ROWS) {
            handleSave(key, event.getData());
        } else if (eventType == EventType.EXT_DELETE_ROWS) {
            handleDelete(key, event.getData());
        } else if (eventType == EventType.EXT_UPDATE_ROWS) {
            handleUpdate(key, event.getData());
        }
    }

    //这里用于处理事务提交一般结合启动加载position实现异常恢复
    private void handleCommitPosition(EventHeaderV4 headerV4) {
        long position = headerV4.getNextPosition();
        String[] beanNamesForType = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(BinLogCommitPosition.class);
        if (beanNamesForType != null && beanNamesForType.length > 0) {
            BinLogCommitPosition bean = BeanFactoryUtils.getBeanFactory().getBean(BinLogCommitPosition.class);
            bean.commitBinLogPosition(position);
        }
    }

    private IBinLogFileListener getBinLogFileListener() {
        String[] beanNamesForType = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(IBinLogFileListener.class);
        if (beanNamesForType == null || beanNamesForType.length == 0) {
            return null;
        }
        return BeanFactoryUtils.getBeanFactory().getBean(IBinLogFileListener.class);
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


}
