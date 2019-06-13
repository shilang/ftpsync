package com.honeywell.greenhouse.sync;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;

import com.honeywell.greenhouse.sync.comm.AppFileUriUtil;
import com.honeywell.greenhouse.sync.core.FtpService;
import com.honeywell.greenhouse.sync.core.FtpSessionFactory;
import com.honeywell.greenhouse.sync.po.CloudFileEntity;
import com.honeywell.greenhouse.sync.po.Ftp;
import com.honeywell.greenhouse.sync.po.OtmFileEntity;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class FtpSync {

    private static String url = "https://api.test.com.cn";

    private static ConcurrentMap<Long, CloudFileEntity> cloudFileTable = new ConcurrentHashMap();

    private static ConcurrentMap<Long, OtmFileEntity> otmFileTable = new ConcurrentHashMap();

    public static void main(String[] args) {
        FtpSync sync = new FtpSync();
        sync.loadReadCloudFile();
        sync.loadReadOtmFile();
        System.out.println("cloud:" + cloudFileTable.size());
        System.out.println("otm:" + otmFileTable.size());
//        sync.start();
        System.out.println(sync.getCloudyFileUrl("28,0136ae81b10120"));
//        sync.testUpload();
    }

    public void testUpload() {
        FtpService ftpService = FtpSessionFactory.getFtpService(getFtpPropertie());
        String otm = "/PRESHIPMENTRECEIPT/201906060177/20190606-0177_回单照片_122883.jpg";
        String cloud = "https://api.test.com.cn/1/file/general/0/8ae851e/5,080a7b1c2762c3";
        try {
            ftpService.uploadCloudFileToFtp(otm, cloud, 122883L);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        FtpService ftpService = FtpSessionFactory.getFtpService(getFtpPropertie());
        Iterator<Map.Entry<Long, OtmFileEntity>> otmIterator = otmFileTable.entrySet().iterator();
        while (otmIterator.hasNext()) {
            Map.Entry<Long, OtmFileEntity> otmEntry = otmIterator.next();
            OtmFileEntity otmFileEntity = otmEntry.getValue();
            CloudFileEntity cloudFileEntity = cloudFileTable.get(otmFileEntity.getAttachmentId());
            if (cloudFileEntity == null) {
                System.out.println(otmFileEntity.getAttachmentId() + " 没有对应的云文件");
                continue;
            }
            String cloudFile = getCloudyFileUrl(cloudFileEntity.getFid());
            System.out.println("start:" + otmFileEntity.getAttachmentId() + "    " + cloudFile);

            try {
                ftpService.uploadCloudFileToFtp(otmFileEntity.getFileName(), cloudFile, otmFileEntity.getAttachmentId());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ftpService.shutdown();
    }

    public void loadReadCloudFile() {
        try {
            for (String line : FileUtils.readLines(new File("./file/cloud_file.txt"), "utf-8")) {
                String[] split = line.split("\\t");
                CloudFileEntity entity = new CloudFileEntity();
                entity.setOrderId(Long.valueOf(split[0]));
                entity.setOrderNoTpl(String.valueOf(split[1]));
                entity.setAttachmentId(Long.valueOf(split[2]));
                entity.setFid(String.valueOf(split[3]));
                entity.setFileName(String.valueOf(split[4]));
                cloudFileTable.put(entity.getAttachmentId(), entity);
            }
            ;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadReadOtmFile() {
        try {
            for (String line : FileUtils.readLines(new File("./file/otm_file.txt"), "utf-8")) {
                String[] split = line.split(",");
                OtmFileEntity entity = new OtmFileEntity();
                String shensyId = split[0];
                shensyId = shensyId.replace("SHENSY.", "");
                entity.setAttachmentId(Long.valueOf(shensyId));
                entity.setOrderNoTpl(String.valueOf(split[1]));
                entity.setFileName(String.valueOf(split[3]));
                otmFileTable.put(entity.getAttachmentId(), entity);
            }
            ;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Ftp getFtpPropertie() {
        Ftp ftp = new Ftp();
        ftp.setEnabled(true);
        ftp.setHost("127.0.0.1");
        ftp.setPassword("HN3e#!8-999");
        ftp.setPort(21);
        ftp.setUsername("HOki9ujELLFTP");
//        ftp.setSessionWaitTimeout(5000);
        ftp.setSessionPoolSize(2);
        return ftp;
    }

    public String getCloudyFileUrl(String fid) {
        //"5,0811406428396f"
        String fileUrl = AppFileUriUtil.genGeneralDownloadPublicUri(0, fid);
        return url + fileUrl;
    }
}
