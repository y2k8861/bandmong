import { describe, expect, it } from 'vitest';
import { hourLabel, perPerson, won } from './money';

describe('금액', () => {
  it('1인 몫은 100원 단위 올림 (서버와 같게)', () => {
    expect(perPerson(38000, 2)).toBe(19000);
    expect(perPerson(40000, 3)).toBe(13400);
    expect(perPerson(20000, 0)).toBe(20000);
  });
  it('표시', () => {
    expect(won(19000)).toBe('19,000원');
    expect(hourLabel(25)).toBe('01:00');
  });
});
