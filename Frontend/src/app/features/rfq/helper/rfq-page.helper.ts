import { RfqCriterion, RfqScoreWeights, RfqStatus } from '../model/rfq.models';

export type RfqTab =
  | 'ALL'
  | 'MINE'
  | 'PENDING_ME'
  | 'DRAFT'
  | 'PENDING'
  | 'RECEIVING'
  | 'COMPARING'
  | 'SELECTED'
  | 'CLOSED';

export type DetailTab = 'OVERVIEW' | 'QUOTES' | 'COMPARISON' | 'APPROVAL' | 'HISTORY';

export type KpiFilter =
  | 'M_DRAFT'
  | 'M_RETURNED'
  | 'M_WAITING'
  | 'M_COMPARE'
  | 'C_RFQ'
  | 'C_RESULT'
  | 'C_OVERDUE'
  | 'C_APPROVED'
  | 'A_OPEN'
  | 'A_PENDING'
  | 'A_OVERDUE'
  | 'A_VALUE';

export const STATUS_LABELS: Record<RfqStatus, string> = {
  DRAFT: 'Nháp',
  PENDING_RFQ_APPROVAL: 'Chờ duyệt RFQ',
  RETURNED: 'Bị trả lại',
  SENT: 'Đã gửi',
  RECEIVING_QUOTES: 'Đang nhận báo giá',
  CLOSED_QUOTES: 'Đã đóng nhận giá',
  COMPARING: 'Đang so sánh',
  PENDING_RESULT_APPROVAL: 'Chờ duyệt kết quả',
  SELECTED_SUPPLIER: 'Đã chọn NCC',
  PO_CREATED: 'Đã tạo PO',
  REJECTED: 'Từ chối',
  CANCELED: 'Đã hủy',
  EXPIRED: 'Hết hạn',
};

export const CRITERION_FROM_KEY: Record<keyof RfqScoreWeights, RfqCriterion> = {
  price: 'PRICE',
  technical: 'TECHNICAL',
  delivery: 'DELIVERY',
  payment: 'PAYMENT',
  reputation: 'REPUTATION',
};

export const KEY_FROM_CRITERION: Record<RfqCriterion, keyof RfqScoreWeights> = {
  PRICE: 'price',
  TECHNICAL: 'technical',
  DELIVERY: 'delivery',
  PAYMENT: 'payment',
  REPUTATION: 'reputation',
};

export const SCORE_LABELS: Record<keyof RfqScoreWeights, string> = {
  price: 'Giá',
  technical: 'Kỹ thuật / chất lượng',
  delivery: 'Giao hàng',
  payment: 'Thanh toán',
  reputation: 'Uy tín NCC',
};

export const DEFAULT_WEIGHTS: RfqScoreWeights = {
  price: 40,
  technical: 25,
  delivery: 15,
  payment: 10,
  reputation: 10,
};

export const FINAL_STATUSES: RfqStatus[] = [
  'SELECTED_SUPPLIER',
  'PO_CREATED',
  'REJECTED',
  'CANCELED',
  'EXPIRED',
];

export const isOpenStatus = (status: RfqStatus): boolean => !FINAL_STATUSES.includes(status);

export const dayString = (date: Date): string => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
};
