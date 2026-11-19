import type { Instrument, Seats } from '../api/types';
import { MAX_PER_INSTRUMENT, MAX_SEATS, seatTotal } from '../lib/band';
import { INSTRUMENTS } from '../lib/profile';

/** 악기별 자리 수 조절. minimum은 줄일 수 없는 하한(내 자리·이미 찬 자리). */
export default function SeatStepper({ seats, minimum, onChange, note }: {
  seats: Seats;
  minimum: Seats;
  onChange: (next: Seats) => void;
  note?: (inst: Instrument) => string | null;
}) {
  const total = seatTotal(seats);
  const set = (id: Instrument, d: number) => onChange({ ...seats, [id]: (seats[id] ?? 0) + d });
  return (
    <div className="seats-edit">
      <div className="between"><span className="label">필요한 악기 자리</span><span className="sub mono">총 {total}/{MAX_SEATS}자리</span></div>
      {INSTRUMENTS.map((i) => {
        const n = seats[i.id] ?? 0;
        const min = minimum[i.id] ?? 0;
        const tip = note?.(i.id);
        return (
          <div className="seat" key={i.id}>
            <span className="seat-name">{i.name}</span>
            {tip && <span className="tag soft">{tip}</span>}
            <button type="button" className="step" aria-label={`${i.name} 자리 줄이기`} disabled={n <= min} onClick={() => set(i.id, -1)}>−</button>
            <span className="n mono" aria-live="polite">{n}</span>
            <button type="button" className="step" aria-label={`${i.name} 자리 늘리기`}
                    disabled={n >= MAX_PER_INSTRUMENT || total >= MAX_SEATS} onClick={() => set(i.id, 1)}>+</button>
          </div>
        );
      })}
    </div>
  );
}
