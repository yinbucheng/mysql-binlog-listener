package cn.bucheng.mysql.util;


import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：yinchong
 * @create ：2019/7/24 19:09
 * @description：
 * @modified By：
 * @version:
 */
public class BinLogUtils {

    public static String createKey(String dbName, String tableName) {
        return dbName + "-" + tableName;
    }

    private static Map<String, List<String>> clazzFields = new ConcurrentHashMap<>();


    public static <T> T decode(Class<T> clazz, Map<String, Serializable> msg) {
        try {
            T cls = clazz.newInstance();
            List<String> properties = getPropertiesFromClass(clazz);
            if (properties != null) {
                for (String property : properties) {
                    BeanUtils.setProperty(cls, property, msg.get(property));
                }
            }
            return cls;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<String> getPropertiesFromClass(Class clazz) {
        List<String> result = clazzFields.get(clazz.getName());
        if (result != null) {
            return result;
        }
        synchronized (clazz) {
            result = clazzFields.get(clazz.getName());
            if (result == null) {
                Field[] fields = clazz.getDeclaredFields();
                if (fields != null) {
                    result = new LinkedList<>();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        result.add(field.getName());
                    }
                }
                clazzFields.put(clazz.getName(), result);
            }
        }
        return result;
    }


}
