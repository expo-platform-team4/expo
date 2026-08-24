/** 원화 표시. `12,000원` 형태. */
export const formatCurrency = (amount: number): string => `${amount.toLocaleString('ko-KR')}원`
