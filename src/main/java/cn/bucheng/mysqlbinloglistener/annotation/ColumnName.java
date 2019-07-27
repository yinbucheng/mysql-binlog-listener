package cn.bucheng.mysqlbinloglistener.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author buchengyin
 * @create 2019/7/27 8:40
 * @describe
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ColumnName {
    //sql映射的列名
    String sqlColumn();

    //java对应的类型
    String javaColumn() default "";
}
