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
