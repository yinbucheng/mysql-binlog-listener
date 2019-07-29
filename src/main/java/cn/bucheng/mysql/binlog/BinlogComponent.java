package cn.bucheng.mysql.binlog;

import cn.bucheng.mysql.aware.BeanFactoryUtils;
import cn.bucheng.mysql.callback.BinlogConfigCallback;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * @author buchengyin
 * @create 2019/7/27 8:33
 * @describe
 */
@Component
@Slf4j
public class BinlogComponent {

    private BinaryLogClient client;
    @Autowired
    private BinLogConfig config;
    @Autowired
    private CompositeListener listener;

    @PostConstruct
    public void init() {
        log.info("==========start binlog client==========");
        Thread thread = new Thread(() -> {
            client = new BinaryLogClient(
                    config.getHost(),
                    config.getPort(),
                    config.getUsername(),
                    config.getPassword()
            );
            retryRestConfig(config);
            if (!StringUtils.isEmpty(config.getFile())) {
                client.setBinlogFilename(config.getFile());
            }

            if (config.getPosition() != null && !config.getPosition().equals(-1L)) {
                client.setBinlogPosition(config.getPosition());
            }
            client.registerEventListener(listener);

            try {
                log.info("connecting to mysql start");
                client.connect();
                log.info("connecting to mysql done");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        thread.setName("binlog-listener-thread");
        thread.start();
    }

    private void retryRestConfig(BinLogConfig binLogConfig) {
        String[] beanNamesForType = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(BinlogConfigCallback.class);
        if (beanNamesForType != null && beanNamesForType.length != 0) {
            BinlogConfigCallback binlogConfigMapper = BeanFactoryUtils.getBeanFactory().getBean(BinlogConfigCallback.class);
            binlogConfigMapper.configCallback(binLogConfig);
        }
    }


    @PreDestroy
    public void close() {
        try {
            client.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
