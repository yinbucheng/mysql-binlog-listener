package cn.bucheng.mysql.listener;

import java.io.Serializable;

public interface IListener<T> {
    //返回需要映射成的对象实体类型
    Class getClassType();

    //保存事件
    void saveEvent(T data);

    //更新事件
    void updateEvent(T data);

    //删除事件
    void deleteEvent(Serializable id);
}
