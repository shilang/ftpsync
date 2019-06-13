package com.honeywell.greenhouse.sync.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.honeywell.greenhouse.sync.comm.MiscUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Liu Gaius
 */
@Slf4j
public class FtpService {
    private SessionFactory<FTPFile> ftpSessionFactory;

    private AsyncTaskExecutor taskExecutor;

    /**
     * @param ftpSessionFactory
     */
    public FtpService(SessionFactory<FTPFile> ftpSessionFactory) {
        this.ftpSessionFactory = ftpSessionFactory;
        taskExecutor = defaultTaskExecutor();
    }


    public void uploadCloudFileToFtp(String ftpFileName, String cloudFileUrl, Long attachmentId) throws IOException {

        try( Session<FTPFile> ftpSession = ftpSessionFactory.getSession();) {
            URL url = null;
            try {
                url = new URL(cloudFileUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (!ftpSession.exists(ftpFileName)) {
                System.out.println(attachmentId + "|" + ftpFileName + ":不存在");
                //如果不存在，那就上传文件
                URLConnection urlConnection = url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.connect();
                try (InputStream saasIS = urlConnection.getInputStream()) {
                    ftpSession.write(saasIS, ftpFileName);
                }
                System.out.println("upload: " + attachmentId + "  |  " + ftpFileName + " | " + cloudFileUrl);
            } else {
                FTPFile[] files = ftpSession.list(ftpFileName);
                if (files != null && files.length == 1) {
                    long s = files[0].getSize();
                    if (s > 0) {
                        System.out.println("有文件，不为空 " + ftpFileName + " [" + s);
                        //说明有内容，不上传
                    } else {
                        System.out.println("有文件，但是为空，上传：" + ftpFileName);
                        URLConnection urlConnection = url.openConnection();
                        urlConnection.setDoInput(true);
                        urlConnection.connect();
                        try (InputStream saasIS = urlConnection.getInputStream()) {
                            ftpSession.write(saasIS, ftpFileName);
                        }
                    System.out.println("upload: " + attachmentId + "  |  " + ftpFileName + " | " + cloudFileUrl);
                    }
                }
            }
        } finally {
            System.out.println("ended:" + attachmentId);
        }
    }

    public void uploadAsyncFromCloud(String otmFile, String cloudFile, Long attachmentId) {
        taskExecutor.execute(() -> {
            try {
                uploadCloudFileToFtp(otmFile, cloudFile, attachmentId);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }
        });
    }

    public AsyncTaskExecutor defaultTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new TraceableThreadPoolTaskExecutor();
//        threadPoolTaskExecutor.setDaemon(true);
        threadPoolTaskExecutor.setCorePoolSize(32);
        threadPoolTaskExecutor.setQueueCapacity(256);
        threadPoolTaskExecutor.setMaxPoolSize(128);
        threadPoolTaskExecutor.setAllowCoreThreadTimeOut(false);
        threadPoolTaskExecutor.setKeepAliveSeconds(1 * 60); // 1 minute
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskExecutor.setAwaitTerminationSeconds(30);
        threadPoolTaskExecutor.setRejectedExecutionHandler(new TraceableCallerRunsPolicy());
        threadPoolTaskExecutor.setThreadNamePrefix("spring-executor-pool-thread-");
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

    public void shutdown() {
        ((ThreadPoolTaskExecutor) taskExecutor).shutdown();
    }

    static class TraceableThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
        private static final long serialVersionUID = 2230208605777034677L;

        @Override
        public void execute(Runnable task) {
            super.execute(decorate(task));
        }

        @Override
        public void execute(Runnable task, long startTimeout) {
            super.execute(decorate(task), startTimeout);
        }

        private Runnable decorate(Runnable task) {
            return () -> {
                try {
                    task.run();
                } catch (RuntimeException e) { // Just guarantees if developer didn't catch it
                    Throwable re = MiscUtils.getRootCause(e);
                    log.error("[TraceableTaskError]: " + re.getMessage(), re);
                    // needn't re-throw, as it's not track-able ...
                }
            };
        }
    }

    /**
     * Trace rejections
     */
    static class TraceableCallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            log.error("[TraceableDiscardPolicy] Task {} run in caller thread instead of {}", r, e);
            log.error("[TraceableDiscardPolicy] ActiveCount:{}, LargestPoolSize:{}, CompletedTaskCount:{}, TaskCount:{}",
                    e.getActiveCount(), e.getLargestPoolSize(), e.getCompletedTaskCount(), e.getTaskCount());
            if (!e.isShutdown()) {
                r.run();
            }
        }

    }

}

