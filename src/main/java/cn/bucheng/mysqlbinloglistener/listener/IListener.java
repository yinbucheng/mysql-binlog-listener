package cn.bucheng.mysqlbinloglistener.listener;

import java.io.Serializable;

public interface IListener {
    //返回需要映射成的对象实体类型
    <T> Class<T> getClassType();

    //保存事件
    <T> void saveEvent(T data);

    //更新事件
    <T> void updateEvent(T data);

    //删除事件
    void deleteEvent(Serializable id);
}
