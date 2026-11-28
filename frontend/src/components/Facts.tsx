import type { ReactNode } from 'react';

/** 사실 여러 개를 한 줄에. 가운뎃점(·)으로 잇지 않고 가는 세로선으로 나눈다. 빈 값은 건너뛴다. */
export default function Facts({ items, className = '' }: { items: (ReactNode | null | undefined | false | '')[]; className?: string }) {
  const shown = items.filter((x) => x !== null && x !== undefined && x !== false && x !== '');
  return (
    <span className={`facts ${className}`.trim()}>
      {shown.map((x, i) => <span key={i}>{x}</span>)}
    </span>
  );
}
