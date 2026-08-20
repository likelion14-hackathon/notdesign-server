package com.likelionknu.notdesign.common.ai;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 모델 폴백 설정. 성능이 좋은 순서로 모델을 나열한다.
 * 앞에서부터 호출하고, 한도(429)나 사용 불가 시 다음 모델로 넘어간다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.fallback")
public class AiFallbackProperties {
    private List<String> models = List.of();
}
