package cn.bucheng.mysqlbinloglistener.utils;


import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

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


    public static <T> T decode(Class<T> clazz, Map<String, Serializable> msg) {
        try {
            T cls = clazz.newInstance();
            Set<Map.Entry<String, Serializable>> entrySet = msg.entrySet();
            for (Map.Entry<String, Serializable> entry : entrySet) {
                BeanUtils.setProperty(cls, entry.getKey(), entry.getValue());
            }
            return cls;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
