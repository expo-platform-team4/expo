-- 박람회별 최종 정산 리포트  →  com.expo.settlement
-- 근거: docs/init_table_schema.md 6-9. 필수 출력 필드가 명세에 명시되어 있다.
CREATE OR REPLACE VIEW v_client_dashboard_settlements AS
SELECT s.host_client_id            AS host_client_id,
       s.id                        AS settlement_id,
       s.expo_id                   AS expo_id,
       e.title                     AS expo_title,
       e.event_end_at              AS event_end_at,
       s.settlement_due_at         AS settlement_due_at,
       s.status                    AS status,
       s.gross_ticket_sales_amount AS gross_ticket_sales_amount,
       s.ticket_refund_amount      AS ticket_refund_amount,
       s.net_ticket_sales_amount   AS net_ticket_sales_amount,
       s.booking_fee_gross_amount  AS booking_fee_gross_amount,
       s.booking_fee_refund_amount AS booking_fee_refund_amount,
       s.booking_fee_net_amount    AS booking_fee_net_amount,
       s.gross_booth_sales_amount  AS gross_booth_sales_amount,
       s.adjustment_amount         AS adjustment_amount,
       s.remittance_due_amount     AS remittance_due_amount,
       rm.remitted_amount          AS remitted_amount,
       rm.remitted_at              AS remitted_at,
       rm.status                   AS remittance_status,
       rp.file_id                  AS latest_report_file_id,
       rp.format                   AS latest_report_format,
       rp.report_version           AS latest_report_version
FROM settlements s
JOIN expos e ON e.id = s.expo_id
-- 송금과 리포트는 최신 1건만 노출한다.
LEFT JOIN LATERAL (
    SELECT r.remitted_amount, r.remitted_at, r.status
      FROM remittances r
     WHERE r.settlement_id = s.id
     ORDER BY r.created_at DESC LIMIT 1
) rm ON TRUE
LEFT JOIN LATERAL (
    SELECT sr.file_id, sr.format, sr.report_version
      FROM settlement_reports sr
     WHERE sr.settlement_id = s.id
     ORDER BY sr.report_version DESC, sr.generated_at DESC LIMIT 1
) rp ON TRUE;
