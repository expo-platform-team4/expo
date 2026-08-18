package com.expo.notification.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.notification.entity.Notification;
import com.expo.notification.service.rebuild.NotificationTextRebuilder;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 템플릿 코드로 재구성기를 찾아 준다.
 *
 * <p>Spring 이 {@link NotificationTextRebuilder} 구현을 전부 모아 넣어 준다. 템플릿이 하나 늘 때 <b>여기를
 * 고칠 일이 없다</b> — 구현을 하나 더 만들면 자동으로 붙는다. {@code switch} 로 짜면 템플릿마다 두
 * 곳(구현과 분기)을 고쳐야 하고, 한쪽을 빠뜨리면 런타임에야 드러난다.
 *
 * <p>같은 템플릿을 두 구현이 맡으면 {@link Collectors#toMap} 이 <b>기동 시점에 터진다.</b> 조용히 하나를
 * 고르면 어느 쪽이 쓰이는지 아무도 모르게 되므로, 늦게 알기보다 못 뜨는 편이 낫다.
 */
@Component
public class NotificationTextRebuilders {

    private final Map<String, NotificationTextRebuilder> byTemplateCode;

    public NotificationTextRebuilders(List<NotificationTextRebuilder> rebuilders) {
        this.byTemplateCode =
                rebuilders.stream()
                        .collect(
                                Collectors.toMap(
                                        NotificationTextRebuilder::templateCode,
                                        Function.identity()));
    }

    /**
     * 본문을 다시 만든다.
     *
     * @throws BusinessException 그 템플릿을 맡는 재구성기가 없을 때. 새 템플릿을 추가하고 재구성기를 안 만든 경우가
     *     여기다 — 조용히 넘기면 관리자가 눌러도 아무 일이 없다
     */
    public String rebuild(Notification notification) {
        NotificationTextRebuilder rebuilder = byTemplateCode.get(notification.getTemplateCode());
        if (rebuilder == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
        }
        return rebuilder.rebuild(notification);
    }
}
