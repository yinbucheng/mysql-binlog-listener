package cn.bucheng.mysqlbinloglistener.binlog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "mysql.binlog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinLogConfig {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String binlogFile;
    private Long binlogFilePosition;
}