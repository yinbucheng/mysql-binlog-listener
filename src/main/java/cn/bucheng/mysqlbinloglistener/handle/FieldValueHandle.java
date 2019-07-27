package cn.bucheng.mysqlbinloglistener.handle;

import java.io.Serializable;
import java.util.Map;

public interface FieldValueHandle {
    //需要处理的类型
    <T> Class<T> getClassType();

    //将map中的属性映射为对象
    <T> T handle(Map<String, Serializable> values);
}
