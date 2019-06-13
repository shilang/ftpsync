package com.honeywell.greenhouse.sync.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;


import com.honeywell.greenhouse.sync.po.Ftp;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FtpSessionFactory {
    public static FtpService getFtpService(Ftp ftp) {

        SessionFactory<FTPFile> ftpSessionFactory = new DefaultFtpSessionFactory() {

            private AtomicInteger ftpClientCounter = new AtomicInteger();

            {
                setHost(ftp.getHost());
                setPort(ftp.getPort());
                setUsername(ftp.getUsername());
                setPassword(ftp.getPassword());
                setBufferSize(ftp.getBufferSize());
                setFileType(ftp.getFileType());
                setClientMode(ftp.getClientMode());
                setConnectTimeout(ftp.getConnectTimeout());
                setDefaultTimeout(ftp.getDefaultTimeout());
                setDataTimeout(ftp.getDataTimeout());
                setControlEncoding("UTF-8");
            }

            @Override
            protected void postProcessClientBeforeConnect(FTPClient ftpClient) throws IOException {
                ftpClient.setAutodetectUTF8(false); // use specified below, avoid detect cost
                ftpClient.setCharset(StandardCharsets.UTF_8);
                ftpClient.setUseEPSVwithIPv4(true); // Ability to cross firewall
                ftpClient.setRemoteVerificationEnabled(false);
            }

            @Override
            protected void postProcessClientAfterConnect(FTPClient ftpClient) throws IOException {
                ftpClient.sendCommand("OPTS UTF8", "ON"); // to support Chinese filename
                log.info("Successfully created ftpClient[{}]", ftpClientCounter.incrementAndGet());
            }

        };
        CachingSessionFactory<FTPFile> cachingFtpSessionFactory = new CachingSessionFactory<>(ftpSessionFactory, ftp.getSessionPoolSize());
        cachingFtpSessionFactory.setSessionWaitTimeout(ftp.getSessionWaitTimeout());
        return new FtpService(cachingFtpSessionFactory);
    }
}
