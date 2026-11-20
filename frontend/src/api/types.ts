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
}

export interface Providers {
  providers: string[];
  devLogin: boolean;
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
