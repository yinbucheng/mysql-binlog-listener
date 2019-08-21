package cn.bucheng.mysql.handle;

import java.io.Serializable;
import java.util.Map;

public interface FieldValueHandle<T> {
    /**
     * 需要特殊处理的DO
     * @return 需要进行特殊处理的DO
     */
    Class getClassType();

    /**
     * 回设的对象
     * @param values 需要设置的map其键为字段名称 值为数据
     * @return 序列化的对象
     */
    T handle(Map<String, Serializable> values);
}
