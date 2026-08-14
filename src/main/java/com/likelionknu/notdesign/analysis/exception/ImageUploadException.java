package com.likelionknu.notdesign.analysis.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

// 이미지가 비었거나, 허용되지 않은 형식이거나, S3 업로드에 실패했을 때.
public class ImageUploadException extends GlobalException {
    public ImageUploadException() {
        super(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
}
