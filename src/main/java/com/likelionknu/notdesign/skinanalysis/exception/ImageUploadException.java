package com.likelionknu.notdesign.skinanalysis.exception;

import com.likelionknu.notdesign.common.response.ErrorCode;
import com.likelionknu.notdesign.common.response.GlobalException;

/**
 * 이미지 파일이 비었거나 S3 업로드에 실패했을 때.
 */
public class ImageUploadException extends GlobalException {
    public ImageUploadException() {
        super(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
}
