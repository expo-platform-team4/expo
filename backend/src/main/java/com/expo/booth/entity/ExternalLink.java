package com.expo.booth.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 링크. 박람회 또는 부스 콘텐츠 중 한쪽에만 속한다({@code ck_external_links_owner_xor}).
 *
 * <p>이 엔티티는 부스 콘텐츠가 소유하는 링크만 다룬다. {@code expo_id} 로 박람회에 속한 링크는 {@code expo} 도메인 몫이라 이
 * 엔티티에서는 항상 {@code null} 로 둔다.
 */
@Getter
@Entity
@Table(name = "external_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalLink extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expo_id")
    private Long expoId;

    @Column(name = "booth_content_id")
    private Long boothContentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 30)
    private ExternalLinkType linkType;

    @Column(length = 100)
    private String label;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** 부스 콘텐츠 외부 링크 등록. */
    public static ExternalLink createForBoothContent(
            Long boothContentId,
            ExternalLinkType linkType,
            String label,
            String url,
            Integer sortOrder) {
        ExternalLink link = new ExternalLink();
        link.boothContentId = boothContentId;
        link.linkType = linkType;
        link.label = label;
        link.url = url;
        link.sortOrder = sortOrder != null ? sortOrder : 0;
        return link;
    }

    /** 링크 정보 수정. */
    public void update(ExternalLinkType linkType, String label, String url) {
        this.linkType = linkType;
        this.label = label;
        this.url = url;
    }

    /** 노출 순서 변경. */
    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
