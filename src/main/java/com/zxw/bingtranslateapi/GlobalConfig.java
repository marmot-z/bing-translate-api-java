package com.zxw.bingtranslateapi;

import lombok.Data;

@Data
public class GlobalConfig {
    private String IG;
    private String IID;
    private String subdomian;
    private String cookie;
    private Integer key;
    private String token;
    private Integer tokenTs;
    private Integer tokenExpiryInterval;
    private Integer count;
}
