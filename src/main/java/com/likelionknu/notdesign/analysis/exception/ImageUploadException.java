package com.likelionknu.notdesign.analysis.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

public class ImageUploadException extends GlobalException {
    public ImageUploadException() {
        super(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
}
