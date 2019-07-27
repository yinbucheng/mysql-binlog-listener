package cn.bucheng.mysql.binlog;

import cn.bucheng.mysql.aware.BeanFactoryUtils;
import cn.bucheng.mysql.callback.BinlogConfigMapper;
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
            if (!StringUtils.isEmpty(config.getBinlogFile())) {
                log.info("---------set binlog file and position-----------");
                client.setBinlogFilename(config.getBinlogFile());
                client.setBinlogPosition(config.getBinlogPosition());
            }

            if (config.getBinlogPosition() != null && !config.getBinlogPosition().equals(-1L)) {
                client.setBinlogPosition(config.getBinlogPosition());
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
        String[] beanNamesForType = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(BinlogConfigMapper.class);
        if (beanNamesForType != null && beanNamesForType.length != 0) {
            BinlogConfigMapper binlogConfigMapper = BeanFactoryUtils.getBeanFactory().getBean(BinlogConfigMapper.class);
            binlogConfigMapper.configMapper(binLogConfig);
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
