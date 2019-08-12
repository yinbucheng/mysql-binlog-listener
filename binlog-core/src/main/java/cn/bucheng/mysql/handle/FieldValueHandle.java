package cn.bucheng.mysql.handle;

import java.io.Serializable;
import java.util.Map;

public interface FieldValueHandle<T> {
    //需要处理的类型
    Class getClassType();

    //将map中的属性映射为对象
    T handle(Map<String, Serializable> values);
}
