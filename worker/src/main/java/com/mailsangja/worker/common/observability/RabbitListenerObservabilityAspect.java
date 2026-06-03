package com.mailsangja.worker.common.observability;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RabbitListenerObservabilityAspect {

    private final ObservabilitySupport observabilitySupport;

    @Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    public Object openRabbitListenerScope(ProceedingJoinPoint joinPoint) throws Throwable {
        Message rawMessage = findRawMessage(joinPoint.getArgs());
        if (rawMessage == null) {
            return joinPoint.proceed();
        }

        Object payload = findPayload(joinPoint.getArgs());
        try (ObservabilitySupport.Scope ignored = observabilitySupport.openRabbitScope(rawMessage, payload)) {
            return joinPoint.proceed();
        }
    }

    private Message findRawMessage(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Message message) {
                return message;
            }
        }
        return null;
    }

    private Object findPayload(Object[] args) {
        for (Object arg : args) {
            if (arg != null && !(arg instanceof Message)) {
                return arg;
            }
        }
        return null;
    }
}
