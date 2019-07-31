package cn.bucheng.mysql.binlog;

import cn.bucheng.mysql.aware.BeanFactoryUtils;
import cn.bucheng.mysql.binlog.chain.BinLogChain;
import cn.bucheng.mysql.binlog.chain.BinLogChainImpl;
import cn.bucheng.mysql.binlog.chain.BinLogHandle;
import cn.bucheng.mysql.binlog.logs.BinaryLogComponent;
import cn.bucheng.mysql.callback.BinlogConfigCallback;
import cn.bucheng.mysql.entity.BinLogBO;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author buchengyin
 * @create 2019/7/27 8:33
 * @describe
 */
@Component
@Slf4j
@Order(Integer.MAX_VALUE)
public class BinlogComponent implements CommandLineRunner {
    @Autowired
    private BinaryLogComponent binaryLogComponent;
    private volatile BinLogChain chain = new BinLogChainImpl();
    @Autowired
    private CompositeListener listener;
    @Autowired
    private BinLogConfig config;
    private static ScheduledExecutorService checkThread = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledExecutorService binlogThread = Executors.newSingleThreadScheduledExecutor();


    public void init() {
        //binlog执行器负责收集监听binlog的事件
        binlogThread.submit(() -> {
            while (true) {
                List<BinLogHandle> handles = initBinLogHandle();
                int len = handles.size();
                for (int i = 0; i < len; i++) {
                    BinLogHandle handle = handles.get(i);
                    chain.addHandle(handle, handle.isCurrent());
                }
                chain.procced();
                Thread.sleep(1000);
            }
        });

        //校验当前执行的binlog是否为活跃的binlog
        checkThread.scheduleWithFixedDelay(() -> {
            if (chain == null) {
                log.warn("chain is empty ,skip check current BinlogHandle");
                return;
            }
            BinLogHandle currentHandle = chain.getCurrentHandle();
            if (currentHandle == null) {
                log.warn("current bin log is empty, maybe binlog client execute prev binlog file");
                return;
            }
            BinLogBO currentBinaryLog = binaryLogComponent.getCurrentBinaryLog();
            if (!currentBinaryLog.getLogFile().equals(currentHandle.getFileName())) {
                currentHandle.awaken();
            }
        }, 5, 2, TimeUnit.MINUTES);
    }

    private List<BinLogHandle> initBinLogHandle() {
        List<BinLogBO> binLogs = binaryLogComponent.listBinaryLogs();
        retryRestConfig(config);
        String file = config.getFile();
        Long position = config.getPosition();
        if (Strings.isBlank(file)) {
            file = binLogs.get(binLogs.size() - 1).getLogFile();
        }
        if (position == null) {
            position = -1L;
        }
        List<BinLogHandle> handles = getWaitExecuteHandle(file, position, binLogs);
        return handles;
    }

    private List<BinLogHandle> getWaitExecuteHandle(String file, long position, List<BinLogBO> binLogs) {
        if (binLogs == null || binLogs.size() == 0) {
            return null;
        }
        boolean flag = false;
        List<BinLogHandle> result = new LinkedList<>();
        for (int i = 0; i < binLogs.size(); i++) {
            BinLogBO binLog = binLogs.get(i);
            long tempPosition = 0L;
            if (!flag && binLog.getLogFile().equals(file)) {
                flag = true;
            }
            if (flag) {
                boolean current = false;
                if (binLog.getLogFile().equals(file)) {
                    tempPosition = position;
                }
                if (i == binLogs.size() - 1) {
                    current = true;
                }
                BinLogHandle handle = new BinLogHandle(file, tempPosition, current, config, listener);
                result.add(handle);
            }
        }
        return result;
    }

    //提供用户加载binlog文件配置
    private void retryRestConfig(BinLogConfig binLogConfig) {
        String[] beanNamesForType = BeanFactoryUtils.getBeanFactory().getBeanNamesForType(BinlogConfigCallback.class);
        if (beanNamesForType != null && beanNamesForType.length != 0) {
            BinlogConfigCallback binlogConfigMapper = BeanFactoryUtils.getBeanFactory().getBean(BinlogConfigCallback.class);
            binlogConfigMapper.configCallback(binLogConfig);
        }
    }


    @Override
    public void run(String... args) throws Exception {
        init();
    }
}
