package cn.bucheng.mysql.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author ：yinchong
 * @create ：2019/7/29 12:45
 * @description：
 * @modified By：
 * @version:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinLogBO implements Serializable {
    private String logFile;
    private Long fileSize;
}
