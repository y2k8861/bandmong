// 화면 확인용 가짜 API 서버 (스프링 없이 모든 화면을 띄워 보기 위함)
//   node tools/mock-api.mjs            → http://localhost:8080  (vite dev 의 /api 프록시가 여기로 온다)
//   curl -X POST localhost:8080/__mock/logout   로그인 화면 보기 / __mock/login 다시 로그인
//   curl -X POST localhost:8080/__mock/admin    관리자로 / __mock/user 일반 회원으로
// 데이터는 고정 예시다. 바꾸는 요청(POST/PUT/DELETE)은 성공한 척하고 같은 데이터를 돌려준다.
import http from 'node:http';

const PORT = Number(process.env.MOCK_PORT || 8080);
let loggedIn = true;
let role = 'USER';

const DAY = 86400_000;
const KST = 9 * 3600_000;
/** 오늘(한국)부터 d일 뒤 h시(한국) */
const at = (d, h, m = 0) => {
  const now = new Date(Date.now() + KST);
  const base = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()) - KST;
  return new Date(base + d * DAY + (h * 60 + m) * 60_000).toISOString();
};
const ago = (min) => new Date(Date.now() - min * 60_000).toISOString();
const ym = (() => { const d = new Date(Date.now() + KST); return `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, '0')}`; })();
const monday = (() => {
  const d = new Date(Date.now() + KST); const dow = (d.getUTCDay() + 6) % 7;
  const m = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() - dow));
  return m.toISOString().slice(0, 10);
})();

const me = () => ({ id: 1, provider: 'dev', nickname: '승호', instruments: ['BASS'], level: 'HOBBY', region: '마포구', onboarded: true, role, restrictedUntil: null });
const seat = (instrument, total, filled) => ({ instrument, total, filled });
const members = [
  { userId: 1, nickname: '승호', instrument: 'BASS', role: 'HOST', level: 'HOBBY' },
  { userId: 2, nickname: '유나', instrument: 'GUITAR', role: 'MEMBER', level: 'STAGE' },
  { userId: 3, nickname: '민재', instrument: 'VOCAL', role: 'MEMBER', level: 'HOBBY' },
  { userId: 4, nickname: '다온', instrument: 'DRUM', role: 'MEMBER', level: 'BEGINNER' },
];
const nextJam = { id: 3, startsAt: at(5, 20), durationMinutes: 120, place: '합정 합주실 B룸', myAnswer: 'YES' };
const band1Summary = {
  id: 1, title: '금요일 밤 록 커버', genre: 'ROCK', level: 'HOBBY', area: '마포·홍대', schedule: '격주 목요일 저녁', recruiting: true,
  hostNickname: '승호', memberCount: 4, seats: [seat('VOCAL', 1, 1), seat('GUITAR', 2, 1), seat('BASS', 1, 1), seat('DRUM', 1, 1)],
  me: 'HOST', myInstrument: 'BASS', pendingCount: 1, nextJam, unreadMessages: 3,
};
const band2Summary = {
  id: 2, title: '화요일 시티팝', genre: 'CITYPOP', level: 'BEGINNER', area: '강남·역삼', schedule: '매주 화요일 19시', recruiting: false,
  hostNickname: '서윤', memberCount: 5, seats: [seat('VOCAL', 1, 1), seat('GUITAR', 1, 1), seat('BASS', 1, 1), seat('DRUM', 1, 1), seat('KEYS', 1, 1)],
  me: 'MEMBER', myInstrument: 'BASS', pendingCount: null,
  nextJam: { id: 5, startsAt: at(3, 19), durationMinutes: 120, place: null, myAnswer: null }, unreadMessages: 0,
};
const explore = [
  { ...band1Summary, id: 11, title: '합정 인디 합주', genre: 'INDIE', area: '마포·합정', hostNickname: '하람', me: 'NONE', myInstrument: null, pendingCount: null, nextJam: null, unreadMessages: null, memberCount: 3, seats: [seat('VOCAL', 1, 1), seat('GUITAR', 1, 1), seat('BASS', 1, 0), seat('DRUM', 1, 1)], schedule: '토요일 오후' },
  { ...band1Summary, id: 12, title: '상수 재즈 트리오', genre: 'JAZZ', level: 'STAGE', area: '마포·상수', hostNickname: '도윤', me: 'NONE', myInstrument: null, pendingCount: null, nextJam: null, unreadMessages: null, memberCount: 2, seats: [seat('KEYS', 1, 1), seat('DRUM', 1, 1), seat('BASS', 1, 0)], schedule: '격주 수요일 20시' },
  { ...band1Summary, id: 13, title: '퇴근길 발라드', genre: 'BALLAD', level: 'BEGINNER', area: '종로·광화문', hostNickname: '지우', me: 'PENDING', myInstrument: 'BASS', pendingCount: null, nextJam: null, unreadMessages: null, memberCount: 3, seats: [seat('VOCAL', 1, 1), seat('KEYS', 1, 1), seat('GUITAR', 1, 1), seat('BASS', 1, 0)], schedule: '매주 목요일 19시 30분' },
  { ...band1Summary, id: 14, title: '금요일 밤 록 커버', me: 'HOST' },
];
const application = {
  id: 21, userId: 7, nickname: '하늘', userInstruments: ['GUITAR'], level: 'HOBBY', region: '마포구', instrument: 'GUITAR',
  message: '평일 저녁은 다 돼요. 주말은 어려워요.', status: 'PENDING', createdAt: ago(40),
};
const bandDetail = (id) => ({
  id, title: band1Summary.title, genre: 'ROCK', level: 'HOBBY', area: '마포·홍대', schedule: '격주 목요일 저녁',
  intro: '90~00년대 록 커버를 해요. 실력보다 꾸준히 나오는 사람이 좋아요. 합주 끝나고 가끔 맥주 한 잔.',
  recruiting: true, createdAt: ago(60 * 24 * 40), seats: band1Summary.seats, members, me: 'HOST', myApplication: null,
  applications: [application], nextJam, unreadMessages: 3,
});
const rsvp = (u, answer) => ({ userId: u.userId, nickname: u.nickname, instrument: u.instrument, answer });
const jam = (id, d, h, place) => ({
  id, bandId: 1, bandTitle: band1Summary.title, startsAt: at(d, h), endsAt: at(d, h + 2), durationMinutes: 120, place, memo: '셋리스트 3곡 맞춰 보기. 브릿지 템포 확인.',
  status: 'SCHEDULED', over: d < 0, host: true, counts: { yes: 2, late: 1, no: 0, none: 1 }, myAnswer: 'YES',
  answers: [rsvp(members[0], 'YES'), rsvp(members[1], null), rsvp(members[2], 'YES'), rsvp(members[3], 'LATE')],
});
const T = (min) => ago(min);
const msg = (id, sid, body, min, type = 'TEXT') => ({ id, bandId: 1, type, senderId: sid, senderNickname: sid ? members.find((m) => m.userId === sid)?.nickname : null, body, createdAt: T(min), clientId: null });
const messages = [
  msg(1, null, '유나님이 기타로 들어왔어요', 300, 'SYSTEM'),
  msg(2, 2, '안녕하세요! 기타 유나예요 잘 부탁드려요', 295),
  msg(3, 1, '환영해요. 다음 합주 때 셋리스트 맞춰봐요', 290),
  msg(4, null, '합주가 확정됐어요', 200, 'SYSTEM'),
  msg(5, 3, '저번 합주 녹음 들어보니까 브릿지 템포가 좀 빨라요', 60),
  msg(6, 4, '드럼 스틱 하나 놓고 갔어요 ㅠ 혹시 보신 분?', 30),
  msg(7, 1, '합주실에 맡겨 뒀어요. 목요일에 챙겨 갈게요', 25),
];
const notifications = [
  { id: 1, type: 'APPLICATION_RECEIVED', title: '기타 자리에 지원이 왔어요', body: '하늘님: 평일 저녁은 다 돼요.', link: '/bands/1', read: false, createdAt: ago(40) },
  { id: 2, type: 'JAM_SCHEDULED', title: '합주가 확정됐어요', body: '금요일 밤 록 커버 · 합정 합주실 B룸', link: '/jams/3', read: false, createdAt: ago(200) },
  { id: 3, type: 'JAM_MOVED', title: '합주 시간이 바뀌었어요', body: '화요일 시티팝 · 참석 여부를 다시 알려 주세요', link: '/jams/5', read: true, createdAt: ago(60 * 26) },
  { id: 4, type: 'APPLICATION_ACCEPTED', title: '화요일 시티팝에 들어갔어요', body: '베이스 자리로 수락됐어요', link: '/bands/2', read: true, createdAt: ago(60 * 24 * 6) },
];
const hours = [14, 16, 18, 19, 20, 21];
const availability = () => ({
  weekStart: monday, hours, mine: [3, 4, 9, 10, 22], memberCount: 4,
  counts: { 3: 2, 4: 4, 9: 3, 10: 3, 15: 1, 16: 2, 22: 3, 23: 1, 28: 2 },
  voters: members.map((m, i) => ({ userId: m.userId, nickname: m.nickname, voted: i !== 1 })),
  best: [
    { slot: 4, startsAt: at(0, 20), count: 4, names: ['승호', '유나', '민재', '다온'] },
    { slot: 9, startsAt: at(1, 19), count: 3, names: ['승호', '민재', '다온'] },
    { slot: 22, startsAt: at(3, 20), count: 3, names: ['승호', '유나', '다온'] },
  ],
});
const rooms = [
  { id: 1, name: 'A룸', capacity: 5, pricePerHour: 15000, gear: '마샬 JCM800, 펜더 트윈, 야마하 드럼' },
  { id: 2, name: 'B룸', capacity: 7, pricePerHour: 18000, gear: '오렌지 앰프, 앰펙 베이스 앰프, 펄 드럼' },
];
const studios = [
  { id: 1, name: '합정 합주실', area: '마포', station: '합정역 3번 출구', openHour: 10, closeHour: 26, lateDiscount: true, perks: ['주차 1대', '녹음 가능'], rooms },
  { id: 2, name: '홍대 사운드룸', area: '마포', station: '홍대입구역 9번 출구', openHour: 12, closeHour: 24, lateDiscount: false, perks: ['악기 대여'], rooms: [{ ...rooms[0], id: 3, pricePerHour: 13000 }] },
];
const slots = (roomId, date) => ({
  roomId, studioName: '합정 합주실', roomName: 'B룸', date,
  slots: Array.from({ length: 14 }, (_, i) => ({ hour: 10 + i, startsAt: `${date}T${String(10 + i - 9).padStart(2, '0')}:00:00Z`, price: 10 + i >= 22 ? 16200 : 18000, taken: [3, 4, 9].includes(i), past: false })),
});
const settle = (u, paid) => ({ userId: u.userId, nickname: u.nickname, amount: 9000, paid });
const bookings = [{
  id: 1, bandId: 1, studioName: '합정 합주실', roomName: 'B룸', area: '마포', station: '합정역 3번 출구', startsAt: at(5, 20), endsAt: at(5, 22), hours: 2,
  price: 36000, perPerson: 9000, status: 'CONFIRMED', jamId: 3, host: true,
  settles: [settle(members[0], true), settle(members[1], false), settle(members[2], true), settle(members[3], false)], paidCount: 2, mine: settle(members[0], true),
}];
const dues = () => ({
  month: ym, amount: 30000, account: '카카오뱅크 3333-01-2345678 (승호)', host: true, paidCount: 3, total: 4,
  members: members.map((m, i) => ({ userId: m.userId, nickname: m.nickname, paid: i !== 3, paidAt: i !== 3 ? ago(60 * 24 * (i + 1)) : null })),
});
const cue = (id, type, title, durationSec) => ({ id, type, title, durationSec });
const gig = (id) => ({
  id, bandId: 1, bandTitle: band1Summary.title, title: '연말 직장인 밴드 공연', startsAt: at(22, 18), venue: '홍대 라이브 클럽', slotMinutes: 20,
  status: 'SCHEDULED', version: 3, host: true, totalSec: 1180,
  items: [cue(1, 'MC', '오프닝 인사', 60), cue(2, 'SONG', "Don't Look Back in Anger", 290), cue(3, 'SONG', '한숨', 250), cue(4, 'MC', '멤버 소개', 90), cue(5, 'SONG', 'Basket Case', 185), cue(6, 'SONG', '나는 나비', 260), cue(7, 'MC', '마무리 인사', 45)],
  checks: [{ id: 1, text: '합주실 리허설 2회', done: true, doneBy: '승호' }, { id: 2, text: 'MR 파일 USB에 담기', done: true, doneBy: '민재' }, { id: 3, text: '드럼 스틱 여분', done: false, doneBy: null }],
  doneCount: 2,
});
const reports = [{
  id: 1, targetType: 'MESSAGE', targetId: 6, reason: 'ABUSE', reasonLabel: '욕설·비방', detail: '채팅에서 반복적으로 욕을 해요', status: 'OPEN', createdAt: ago(90),
  reporterNickname: '민재', accusedId: 9, accusedNickname: '스팸계정', targetSummary: '"***같은 합주실 왜 잡음"', openOnTarget: 2, reportsAgainstAccused: 3,
  accusedRestrictedUntil: null, resolution: null, adminNote: null,
}];

const ERR = Symbol('err');
const err = (code, message) => ({ [ERR]: code, message });

function route(method, path, q) {
  if (path === '/api/auth/providers') return { providers: ['kakao', 'naver', 'google'], devLogin: true };
  if (!loggedIn && path !== '/api/dev/login') return err(401, '로그인이 필요해요');
  if (path === '/api/dev/login') { loggedIn = true; return me(); }
  if (path === '/api/auth/logout') { loggedIn = false; return {}; }
  if (path === '/api/me' && method === 'GET') return me();
  if (path === '/api/me/profile') return me();
  if (path === '/api/me/bands') return { bands: [band1Summary, band2Summary], applied: [explore[2]] };
  if (path === '/api/bands' && method === 'GET') return explore;
  if (path === '/api/bands' && method === 'POST') return bandDetail(1);
  if (path === '/api/blocks') return [{ userId: 9, nickname: '스팸계정', blockedAt: ago(60 * 24 * 7) }];
  if (path === '/api/notifications/unread') return { unread: 2 };
  if (path === '/api/notifications/read-all') return { unread: 0 };
  if (path === '/api/notifications') return { items: notifications, hasMore: false, unread: 2 };
  if (path === '/api/studios') return studios;
  if (path === '/api/reports') return { id: 1, message: '신고가 접수됐어요' };
  if (path === '/api/admin/reports') return q.get('status') === 'OPEN' ? reports : [];
  let m;
  if ((m = path.match(/^\/api\/bands\/(\d+)\/messages$/))) return method === 'GET' ? { messages, hasMore: false, lastReadId: 4 } : msg(99, 1, '…', 0);
  if (path.match(/^\/api\/bands\/\d+\/availability$/)) return availability();
  if (path.match(/^\/api\/bands\/\d+\/jams$/)) return method === 'GET' ? { upcoming: [jam(3, 5, 20, '합정 합주실 B룸'), jam(8, 19, 20, null)], past: [jam(2, -9, 20, '홍대 사운드룸 A룸')] } : jam(3, 5, 20, '합정 합주실 B룸');
  if (path.match(/^\/api\/bands\/\d+\/bookings$/)) return method === 'GET' ? bookings : bookings[0];
  if (path.match(/^\/api\/bands\/\d+\/dues/)) return dues();
  if (path.match(/^\/api\/bands\/\d+\/gigs$/)) return method === 'GET' ? [gig(7)] : gig(7);
  if ((m = path.match(/^\/api\/bands\/(\d+)(\/.*)?$/))) return bandDetail(Number(m[1]));
  if ((m = path.match(/^\/api\/jams\/(\d+)/))) return jam(Number(m[1]), 5, 20, '합정 합주실 B룸');
  if ((m = path.match(/^\/api\/gigs\/(\d+)/))) return gig(Number(m[1]));
  if ((m = path.match(/^\/api\/rooms\/(\d+)\/slots$/))) return slots(Number(m[1]), q.get('date'));
  if ((m = path.match(/^\/api\/bookings\/(\d+)/))) return bookings[0];
  if (path.match(/^\/api\/notifications\/\d+\/read$/)) return notifications[0];
  if (path.match(/^\/api\/admin\//)) return reports[0];
  return err(404, '없는 주소예요');
}

http.createServer((req, res) => {
  const url = new URL(req.url, 'http://x');
  const send = (code, body) => { res.writeHead(code, { 'content-type': 'application/json; charset=utf-8' }); res.end(JSON.stringify(body)); };
  if (url.pathname.startsWith('/__mock/')) {
    const what = url.pathname.slice(8);
    if (what === 'logout') loggedIn = false; if (what === 'login') loggedIn = true;
    if (what === 'admin') role = 'ADMIN'; if (what === 'user') role = 'USER';
    return send(200, { loggedIn, role });
  }
  if (url.pathname === '/api/stream') {
    if (!loggedIn) return send(401, {});
    res.writeHead(200, { 'content-type': 'text/event-stream', 'cache-control': 'no-cache' });
    res.write('event: ready\ndata: {}\n\n');
    const t = setInterval(() => res.write(': ping\n\n'), 15000);
    return req.on('close', () => clearInterval(t));
  }
  req.resume();
  req.on('end', () => {
    const out = route(req.method, url.pathname, url.searchParams);
    if (out && out[ERR]) return send(out[ERR], { message: out.message });
    if (req.method === 'DELETE' && out && Object.keys(out).length === 0) { res.writeHead(204); return res.end(); }
    send(200, out);
  });
}).listen(PORT, () => console.log(`mock api on http://localhost:${PORT}`));
