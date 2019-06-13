package com.honeywell.greenhouse.sync.comm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MiscUtils {

    public static final Pattern NON_CHINESE_CHARACTERS_REGEX = Pattern.compile("[^\u4e00-\u9fa5]+");

    private static final List<String> IP_HEADER_CANDIDATES = Collections.unmodifiableList(Arrays.asList( //
            "X-Forwarded-For", //
            "Proxy-Client-IP", //
            "WL-Proxy-Client-IP", //
            "HTTP_X_FORWARDED_FOR", //
            "HTTP_X_FORWARDED", //
            "HTTP_X_CLUSTER_CLIENT_IP", //
            "HTTP_CLIENT_IP", //
            "HTTP_FORWARDED_FOR", //
            "HTTP_FORWARDED", //
            "HTTP_VIA", //
            "REMOTE_ADDR" //
    ));


    /**
     * @param t
     * @return root cause (never null) by recursively call {@link Throwable#getCause()}
     * @throws NullPointerException if given parameter is null
     */// a simple way to get root cause, sometimes root cause is nearly impossible (Yuebing Cao)
    public static Throwable getRootCause(@Nonnull Throwable t) {
        int depth = 0;
        Throwable root;
        do {
            root = t;
            t = t.getCause();
        } while (t != null && root != t && ++depth < 1024); // 1024 is a safe guard just in case infinite loop
        return root;
    }

    /**
     * @param file
     * @param defaultExt
     * @return given file's extension, if none found, default to <code>defaultExt</code>
     * @throws NullPointerException if given file is null
     */
    public static String getFileExtension(@Nonnull File file, @Nullable String defaultExt) {
        String fileName = file.getName();
        if (StringUtils.isNotEmpty(fileName)) {
            String ext = FilenameUtils.getExtension(fileName);
            if (StringUtils.isNotEmpty(ext)) {
                return ext;
            }
        }
        return defaultExt;
    }

    /**
     * @param file
     * @param defaultExt
     * @return given file's extension, if none found, default to <code>defaultExt</code>
     * @throws NullPointerException if given file is null
     */
    public static String getFileExtension(@Nonnull MultipartFile file, @Nullable String defaultExt) {
        String fileName = file.getOriginalFilename();
        if (StringUtils.isNotEmpty(fileName)) {
            String ext = FilenameUtils.getExtension(fileName);
            if (StringUtils.isNotEmpty(ext)) {
                return ext;
            }
        }
        return defaultExt;
    }


    /**
     * @param localeStr     the locale string to be parsed
     * @param defaultLocale default locale if localeStr is null/empty or can't be parsed
     * @return successfully parsed {@link Locale} if localeStr is not empty, otherwise
     * default locale. No exception would be thrown.
     */
    public Locale parseLocaleQuietly(@Nullable String localeStr, @Nullable Locale defaultLocale) {
        Locale result = null;
        if (StringUtils.isNotEmpty(localeStr)) {
            try {
                result = LocaleUtils.toLocale(localeStr);
            } catch (Exception e) {
                // dummy
            }
        }
        return result != null ? result : defaultLocale;
    }

    /**
     * @param multipartFile
     * @return a normal file in temporary directory transferred from given multipart file, or null if given multipart is null.
     * @throws IOException
     */
    public File transferFrom(MultipartFile multipartFile) throws IOException {
        File destFile = null;
        if (multipartFile != null) {
            String tempDir = FileUtils.getTempDirectoryPath();
            destFile = new File(tempDir, UUID.randomUUID().toString());
            multipartFile.transferTo(destFile);
        }
        return destFile;
    }
}
