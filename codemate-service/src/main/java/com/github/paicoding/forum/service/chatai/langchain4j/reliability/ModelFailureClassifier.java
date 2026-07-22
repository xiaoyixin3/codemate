package com.github.paicoding.forum.service.chatai.langchain4j.reliability;

import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.RetriableException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.exception.ToolArgumentsException;
import dev.langchain4j.exception.ToolExecutionException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ModelFailureClassifier {
    public ModelFailureType classify(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof RateLimitException) return ModelFailureType.RATE_LIMIT;
            if (current instanceof StreamTimeoutException || current instanceof TimeoutException || current instanceof RetriableException
                    || current instanceof IOException) return ModelFailureType.RETRIABLE_NETWORK;
            if (current instanceof ToolArgumentsException || current instanceof ToolExecutionException
                    || current instanceof IllegalArgumentException) return ModelFailureType.BUSINESS;
            current = current.getCause();
        }
        return ModelFailureType.MODEL;
    }

    public String publicMessage(Throwable error) {
        switch (classify(error)) {
            case RATE_LIMIT:
                return "模型服务当前限流，请稍后重试";
            case RETRIABLE_NETWORK:
                return "模型网络请求超时或中断，请稍后重试";
            case BUSINESS:
                return "请求或工具参数未通过业务校验";
            default:
                return "模型暂时无法完成请求，请稍后重试";
        }
    }
}
