import { useCallback, useEffect, useLayoutEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getBand } from '../api/bands';
import { getMessages, markRead, newClientId, sendMessage } from '../api/chat';
import type { Me, MessageView } from '../api/types';
import { useStream, useStreamEvent } from '../lib/stream';
import ReportSheet from '../components/ReportSheet';
import { blockUser } from '../api/app';
import { useLoad } from '../lib/useLoad';
import { fmtTime, kstDate } from '../lib/time';
import Facts from '../components/Facts';
import { ChevronLeft } from 'lucide-react';

/** 화면에만 있는 "보내는 중/실패" 메시지 */
type Local = MessageView & { state?: 'sending' | 'failed' };

const dayKey = kstDate;
const dayLabel = (key: string) => {
  const [, m, d] = key.split('-').map(Number);
  const dow = ['일', '월', '화', '수', '목', '금', '토'][new Date(key + 'T00:00:00Z').getUTCDay()];
  return `${m}월 ${d}일 ${dow}요일`;
};

/** 서버에서 온 메시지를 합친다: id로 중복 제거, 내가 미리 그려 둔 것(clientId)은 진짜로 바꿔 끼운다 */
export function mergeMessages(cur: Local[], incoming: MessageView[]): Local[] {
  const out = [...cur];
  for (const m of incoming) {
    if (out.some((x) => x.id === m.id && !x.state)) continue;
    const i = m.clientId ? out.findIndex((x) => x.state && x.clientId === m.clientId) : -1;
    if (i >= 0) out[i] = m; else out.push(m);
  }
  const sent = out.filter((x) => !x.state).sort((a, b) => a.id - b.id);
  return [...sent, ...out.filter((x) => x.state)];
}

export default function ChatPage({ me }: { me: Me }) {
  const bandId = Number(useParams().id);
  const band = useLoad(() => getBand(bandId), [bandId]);
  const { connected } = useStream();
  const [msgs, setMsgs] = useState<Local[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [text, setText] = useState('');
  const [loadingOld, setLoadingOld] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const stick = useRef(true); // 맨 아래를 보고 있으면 새 메시지에 따라 내려간다
  const lastMarked = useRef(0);
  const [picked, setPicked] = useState<number | null>(null); // 남의 메시지를 누르면 "신고" 버튼
  const [reporting, setReporting] = useState<MessageView | null>(null);

  const loadLatest = useCallback(async () => {
    try {
      const page = await getMessages(bandId);
      setMsgs((cur) => mergeMessages(cur, page.messages));
      setHasMore((h) => h || page.hasMore);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : '불러오지 못했어요');
    }
  }, [bandId]);

  useEffect(() => { setMsgs([]); setHasMore(false); stick.current = true; loadLatest(); }, [loadLatest]);

  useStreamEvent('message', (m) => { if (m.bandId === bandId) setMsgs((cur) => mergeMessages(cur, [m])); });
  useStreamEvent('reconnect', () => { loadLatest(); });

  // 보고 있는 동안 읽음 위치를 서버에 알린다
  useEffect(() => {
    const last = [...msgs].reverse().find((m) => !m.state);
    if (!last || last.id <= lastMarked.current || document.visibilityState === 'hidden') return;
    lastMarked.current = last.id;
    markRead(bandId, last.id).catch(() => { lastMarked.current = 0; });
  }, [msgs, bandId]);

  useLayoutEffect(() => {
    const el = listRef.current;
    if (el && stick.current) el.scrollTop = el.scrollHeight;
  }, [msgs]);

  async function loadOlder() {
    const oldest = msgs.find((m) => !m.state);
    if (!oldest) return;
    const el = listRef.current;
    const fromBottom = el ? el.scrollHeight - el.scrollTop : 0;
    setLoadingOld(true);
    try {
      const page = await getMessages(bandId, oldest.id);
      stick.current = false;
      setMsgs((cur) => mergeMessages(cur, page.messages));
      setHasMore(page.hasMore);
      requestAnimationFrame(() => { if (el) el.scrollTop = el.scrollHeight - fromBottom; });
    } finally {
      setLoadingOld(false);
    }
  }

  async function deliver(m: Local) {
    setMsgs((cur) => cur.map((x) => (x.clientId === m.clientId ? { ...x, state: 'sending' } : x)));
    try {
      const saved = await sendMessage(bandId, m.body, m.clientId!);
      setMsgs((cur) => mergeMessages(cur, [saved]));
    } catch {
      setMsgs((cur) => cur.map((x) => (x.clientId === m.clientId ? { ...x, state: 'failed' } : x)));
    }
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    const body = text.trim();
    if (!body) return;
    const local: Local = {
      id: -Date.now(), bandId, type: 'TEXT', senderId: me.id, senderNickname: me.nickname, body,
      createdAt: new Date().toISOString(), clientId: newClientId(), state: 'sending',
    };
    setText('');
    stick.current = true;
    setMsgs((cur) => [...cur, local]);
    deliver(local);
  }

  const onScroll = () => {
    const el = listRef.current;
    if (el) stick.current = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
  };

  let prevDay = '';
  let prevSender: number | null = null;

  return (
    <main className="scr chat">
      <header className="top pad row">
        <Link className="btn-ghost" to={`/bands/${bandId}`} aria-label="밴드로 돌아가기"><ChevronLeft aria-hidden size={18} /></Link>
        <div className="chat-title">
          <b>{band.data?.title ?? '밴드 채팅'}</b>
          <span className="sub"><Facts items={[band.data && `멤버 ${band.data.members.length}명`, !connected && '연결 중…']} /></span>
        </div>
      </header>

      <div className="msgs pad" ref={listRef} onScroll={onScroll} aria-live="polite">
        {hasMore && (
          <button className="link-btn center-text" onClick={loadOlder} disabled={loadingOld}>
            {loadingOld ? '불러오는 중…' : '이전 메시지 더 보기'}
          </button>
        )}
        {error && <p className="err-text center-text">{error}</p>}
        {msgs.length === 0 && !error && <p className="sub center-text">첫 메시지를 남겨 보세요.</p>}
        {msgs.map((m) => {
          const day = dayKey(m.createdAt);
          const showDay = day !== prevDay;
          prevDay = day;
          if (m.type === 'SYSTEM') {
            prevSender = null;
            return (
              <div key={m.id}>
                {showDay && <div className="day">{dayLabel(day)}</div>}
                <div className="sys">{m.body}</div>
              </div>
            );
          }
          const mine = m.senderId === me.id;
          const showName = !mine && (prevSender !== m.senderId || showDay);
          prevSender = m.senderId;
          return (
            <div key={m.clientId && m.state ? m.clientId : m.id}>
              {showDay && <div className="day">{dayLabel(day)}</div>}
              <div className={`msg ${mine ? 'mine' : ''}`}>
                {showName && <span className="who">{m.senderNickname}</span>}
                <div className="line">
                  <p className={`bubble ${m.hidden ? 'hidden' : ''}`}
                     onClick={() => !mine && !m.hidden && m.id > 0 && setPicked(picked === m.id ? null : m.id)}>{m.body}</p>
                  <span className="meta">
                    {m.state === 'sending' ? '보내는 중' : m.state === 'failed' ? '' : fmtTime(m.createdAt)}
                  </span>
                </div>
                {picked === m.id && !mine && (
                  <span className="row">
                    <button className="link-btn msg-report" onClick={() => { setReporting(m); setPicked(null); }}>이 메시지 신고</button>
                    <button className="link-btn msg-report" onClick={async () => {
                      setPicked(null);
                      try {
                        await blockUser(m.senderId!);
                        setMsgs((cur) => cur.filter((x) => x.senderId !== m.senderId)); // 차단한 사람 메시지는 바로 지운다
                      } catch { /* 실패하면 그대로 둔다 */ }
                    }}>{m.senderNickname}님 차단</button>
                  </span>
                )}
                {m.state === 'failed' && (
                  <div className="fail">
                    <span>보내지 못했어요</span>
                    <button onClick={() => deliver(m)}>다시 보내기</button>
                    <button onClick={() => setMsgs((cur) => cur.filter((x) => x !== m))}>지우기</button>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {reporting && <ReportSheet target="MESSAGE" id={reporting.id} label="메시지" onClose={() => setReporting(null)} />}
      <form className="composer pad" onSubmit={submit}>
        <textarea className="input" rows={1} maxLength={1000} placeholder="메시지 보내기" aria-label="메시지" value={text}
                  onChange={(e) => setText(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) { e.preventDefault(); submit(e); } }} />
        <button className="cta small" disabled={!text.trim()}>보내기</button>
      </form>
    </main>
  );
}
