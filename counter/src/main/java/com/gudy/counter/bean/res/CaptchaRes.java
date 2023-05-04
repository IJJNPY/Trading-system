package com.gudy.counter.bean.res;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class CaptchaRes {

    private String id;

    private String imageBase64;

    public CaptchaRes(String uuid, String base64ByteStr) {
        this.id = uuid;
        this.imageBase64 = base64ByteStr;
    }

}
