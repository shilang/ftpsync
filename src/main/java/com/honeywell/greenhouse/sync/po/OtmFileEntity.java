package com.honeywell.greenhouse.sync.po;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtmFileEntity {
    private Long attachmentId;
    private String orderNoTpl;
    private String fileName;
}
