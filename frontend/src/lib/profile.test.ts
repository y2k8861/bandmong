import { describe, expect, it } from 'vitest';
import { nicknameError } from './profile';
import { readCookie } from '../api/client';

describe('닉네임 규칙 (서버와 같아야 함)', () => {
  it('2~10자 한글·영문·숫자는 통과', () => {
    expect(nicknameError('퇴근베이스')).toBeNull();
    expect(nicknameError('Drum99')).toBeNull();
  });
  it('짧거나 특수문자면 안내', () => {
    expect(nicknameError('a')).toBe('닉네임은 한글·영문·숫자 2~10자예요');
    expect(nicknameError('베이스!')).toBe('닉네임은 한글·영문·숫자 2~10자예요');
    expect(nicknameError('  ')).toBe('닉네임을 입력해 주세요');
  });
});

describe('CSRF 쿠키 읽기', () => {
  it('XSRF-TOKEN 값을 꺼낸다', () => {
    expect(readCookie('XSRF-TOKEN', 'JSESSIONID=x; XSRF-TOKEN=ab%2Bc')).toBe('ab+c');
    expect(readCookie('XSRF-TOKEN', 'JSESSIONID=x')).toBeNull();
  });
});
