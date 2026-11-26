export type Instrument = 'VOCAL' | 'GUITAR' | 'BASS' | 'DRUM' | 'KEYS' | 'ETC';
export type Level = 'BEGINNER' | 'HOBBY' | 'STAGE';

export interface Me {
  id: number;
  provider: string;
  nickname: string | null;
  instruments: Instrument[];
  level: Level | null;
  region: string | null;
  onboarded: boolean;
  role: 'USER' | 'ADMIN';
  restrictedUntil: string | null;
}

export interface Providers {
  providers: string[];
  devLogin: boolean;
  /** 고객센터 연락처 (앱 안에 공개) */
  supportEmail?: string;
}

export interface ProfileInput {
  nickname: string;
  instruments: Instrument[];
  level: Level;
  region: string;
}

export type Genre = 'ROCK' | 'CITYPOP' | 'INDIE' | 'JAZZ' | 'BALLAD';
export type Relation = 'HOST' | 'MEMBER' | 'PENDING' | 'NONE';
export type Seats = Partial<Record<Instrument, number>>;

export interface SeatView {
  instrument: Instrument;
  total: number;
  filled: number;
}

export interface MemberView {
  userId: number;
  nickname: string;
  instrument: Instrument;
  role: 'HOST' | 'MEMBER';
  level: Level | null;
}

export interface ApplicationView {
  id: number;
  userId: number;
  nickname: string;
  userInstruments: Instrument[];
  level: Level | null;
  region: string | null;
  instrument: Instrument;
  message: string | null;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED';
  createdAt: string;
}

export interface BandSummary {
  id: number;
  title: string;
  genre: Genre;
  level: Level | null;
  area: string;
  schedule: string | null;
  recruiting: boolean;
  hostNickname: string;
  memberCount: number;
  seats: SeatView[];
  me: Relation;
  myInstrument: Instrument | null;
  pendingCount: number | null;
  nextJam: JamBrief | null;
  unreadMessages: number | null;
}

export interface BandDetail {
  id: number;
  title: string;
  genre: Genre;
  level: Level | null;
  area: string;
  schedule: string | null;
  intro: string | null;
  recruiting: boolean;
  createdAt: string;
  seats: SeatView[];
  members: MemberView[];
  me: Relation;
  myApplication: ApplicationView | null;
  applications: ApplicationView[] | null;
  nextJam: JamBrief | null;
  unreadMessages: number | null;
}

export interface MyBands {
  bands: BandSummary[];
  applied: BandSummary[];
}

export interface BandCreateInput {
  title: string;
  genre: Genre;
  level: Level | null;
  area: string;
  schedule: string;
  intro: string;
  hostInstrument: Instrument;
  seats: Seats;
}

export type RsvpAnswer = 'YES' | 'LATE' | 'NO';

export interface JamBrief {
  id: number;
  startsAt: string;
  durationMinutes: number;
  place: string | null;
  myAnswer: RsvpAnswer | null;
}

export interface RsvpView {
  userId: number;
  nickname: string;
  instrument: Instrument;
  answer: RsvpAnswer | null;
}

export interface JamView {
  id: number;
  bandId: number;
  bandTitle: string;
  startsAt: string;
  endsAt: string;
  durationMinutes: number;
  place: string | null;
  memo: string | null;
  status: 'SCHEDULED' | 'CANCELED';
  over: boolean;
  host: boolean;
  counts: { yes: number; late: number; no: number; none: number };
  myAnswer: RsvpAnswer | null;
  answers: RsvpView[] | null;
}

export interface JamList {
  upcoming: JamView[];
  past: JamView[];
}

export interface AvailabilityView {
  weekStart: string;
  hours: number[];
  mine: number[];
  counts: Record<string, number>;
  memberCount: number;
  voters: { userId: number; nickname: string; voted: boolean }[];
  best: { slot: number; startsAt: string; count: number; names: string[] }[];
}

export interface JamInput {
  startsAt: string;
  durationMinutes: number;
  place: string;
  memo: string;
}

export interface MessageView {
  id: number;
  bandId: number;
  type: 'TEXT' | 'SYSTEM';
  senderId: number | null;
  senderNickname: string | null;
  body: string;
  createdAt: string;
  clientId: string | null;
  hidden?: boolean;
}

export interface MessagePage {
  messages: MessageView[];
  hasMore: boolean;
  lastReadId: number | null;
}

export type NotificationType =
  | 'APPLICATION_RECEIVED' | 'APPLICATION_ACCEPTED' | 'APPLICATION_REJECTED' | 'MEMBER_LEFT'
  | 'JAM_SCHEDULED' | 'JAM_MOVED' | 'JAM_EDITED' | 'JAM_CANCELED';

export interface NotificationView {
  id: number;
  type: NotificationType;
  title: string;
  body: string | null;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  items: NotificationView[];
  hasMore: boolean;
  unread: number;
}

export interface RoomView { id: number; name: string; capacity: number; pricePerHour: number; gear: string | null }
export interface StudioView {
  id: number; name: string; area: string; station: string | null; openHour: number; closeHour: number;
  lateDiscount: boolean; perks: string[]; rooms: RoomView[];
}
export interface SlotsView {
  roomId: number; studioName: string; roomName: string; date: string;
  slots: { hour: number; startsAt: string; price: number; taken: boolean; past: boolean }[];
}
export interface SettleView { userId: number; nickname: string; amount: number; paid: boolean }
export interface BookingView {
  id: number; bandId: number; studioName: string; roomName: string; area: string; station: string | null;
  startsAt: string; endsAt: string; hours: number; price: number; perPerson: number; status: 'CONFIRMED' | 'CANCELED';
  jamId: number | null; host: boolean; settles: SettleView[]; paidCount: number; mine: SettleView | null;
}
export interface DuesView {
  month: string; amount: number | null; account: string | null; host: boolean; paidCount: number; total: number;
  members: { userId: number; nickname: string; paid: boolean; paidAt: string | null }[];
}

export type CueType = 'SONG' | 'MC';
export interface CueItem { id?: number; type: CueType; title: string; durationSec: number }
export interface GigView {
  id: number; bandId: number; bandTitle: string; title: string; startsAt: string; venue: string | null; slotMinutes: number;
  status: 'SCHEDULED' | 'CANCELED'; version: number; host: boolean; totalSec: number;
  items: CueItem[]; checks: { id: number; text: string; done: boolean; doneBy: string | null }[]; doneCount: number;
}

export type ReportTarget = 'USER' | 'BAND' | 'MESSAGE';
export type ReportReason = 'ABUSE' | 'SPAM' | 'NO_SHOW' | 'FAKE' | 'SEXUAL' | 'OTHER';
export type ModerationAction = 'DISMISS' | 'WARN' | 'SUSPEND_7' | 'SUSPEND_30' | 'HIDE_MESSAGE' | 'CLOSE_RECRUITING';
export interface ReportView {
  id: number; targetType: ReportTarget; targetId: number; reason: ReportReason; reasonLabel: string; detail: string | null;
  status: 'OPEN' | 'RESOLVED' | 'DISMISSED'; createdAt: string; reporterNickname: string;
  accusedId: number; accusedNickname: string | null; targetSummary: string; openOnTarget: number;
  reportsAgainstAccused: number; accusedRestrictedUntil: string | null; resolution: string | null; adminNote: string | null;
}
