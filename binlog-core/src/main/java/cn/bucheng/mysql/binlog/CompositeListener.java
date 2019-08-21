package cn.bucheng.mysql.binlog;

import cn.bucheng.mysql.aware.BeanFactoryUtils;
import cn.bucheng.mysql.callback.BinLogCommitPosition;
import cn.bucheng.mysql.bo.TableBO;
import cn.bucheng.mysql.handle.FieldValueHandle;
import cn.bucheng.mysql.holder.TableColumnIdAndNameHolder;
import cn.bucheng.mysql.listener.IBinLogFileListener;
import cn.bucheng.mysql.listener.IListener;
import cn.bucheng.mysql.util.BinLogUtils;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;


@Component
@Slf4j
public class CompositeListener implements BinaryLogClient.EventListener {
    //这里可以将dbName,tableName设置为成员变量主要和binlog有关，binlog能够保证事务插入的正确性
    private String dbName;
    private String tableName;
    @Autowired
    private TableColumnIdAndNameHolder holder;

    private IBinLogFileListener binLogFileListener;

    private BinLogCommitPosition binLogCommitPosition;

    private AtomicInteger threadIndex = new AtomicInteger(0);

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, Runtime.getRuntime().availableProcessors() * 2, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "execute_thread_" + threadIndex.getAndIncrement());
        }
    });

    @Override
    public void onEvent(Event event) {
        executor.execute(() ->
                handleEvent(event)
        );
    }

    private void handleEvent(Event event) {
        EventType eventType = event.getHeader().getEventType();
        if (eventType == EventType.ROTATE) {
            handleBinLogFile(event);
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

    /**
     * 处理binlog的文件名称和位置
     *
     * @param event
     */
    private void handleBinLogFile(Event event) {
        RotateEventData data = event.getData();
        if (binLogFileListener != null) {
            binLogFileListener.handleBinLogFile(data.getBinlogFilename(), data.getBinlogPosition());
        }
    }

    /**
     * 处理binlog的偏移量
     *
     * @param headerV4
     */
    private void handleCommitPosition(EventHeaderV4 headerV4) {
        long position = headerV4.getNextPosition();
        if (binLogCommitPosition != null) {
            binLogCommitPosition.commitBinLogPosition(position);
        }
    }

    @PostConstruct
    private void init() {
        initBinLogCommitPosition();
        initBinLogFileListener();
    }


    private void initBinLogCommitPosition() {
        String[] beanNamesForType = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(BinLogCommitPosition.class);
        if (beanNamesForType != null && beanNamesForType.length > 0) {
            binLogCommitPosition = BeanFactoryUtils.getBeanFactory().getBean(BinLogCommitPosition.class);
        }
    }


    private void initBinLogFileListener() {
        String[] beanNamesForType = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(IBinLogFileListener.class);
        if (beanNamesForType == null || beanNamesForType.length == 0) {
            return;
        }
        binLogFileListener = BeanFactoryUtils.getBeanFactory().getBean(IBinLogFileListener.class);
    }

    /**
     * 处理binlog中的保存数据记录
     *
     * @param key  唯一标示
     * @param data 添加数据
     */
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

    /**
     * 处理binlog中的更新事件
     *
     * @param key  唯一标示
     * @param data 更新数据
     */
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

    /**
     * 处理binlog的删除事件
     *
     * @param key  唯一标示
     * @param data 删除的数据
     */
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
