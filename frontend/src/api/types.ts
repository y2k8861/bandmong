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
