export const won = (n: number) => `${n.toLocaleString('ko-KR')}원`;
/** 24 이상은 다음 날 새벽: 25 → 01:00 */
export const hourLabel = (h: number) => `${String(h % 24).padStart(2, '0')}:00`;
/** 서버와 같은 나누기: 100원 단위 올림 */
export const perPerson = (price: number, people: number) => Math.ceil(price / Math.max(1, people) / 100) * 100;
