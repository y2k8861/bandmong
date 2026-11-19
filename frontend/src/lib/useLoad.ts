import { useCallback, useEffect, useRef, useState } from 'react';

/** 화면 진입 시 불러오기 + 다시 불러오기. 늦게 도착한 이전 응답은 버린다. */
export function useLoad<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const seq = useRef(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(fn, deps);

  const reload = useCallback(async () => {
    const my = ++seq.current;
    setLoading(true);
    setError(null);
    try {
      const v = await run();
      if (my === seq.current) setData(v);
    } catch (e) {
      if (my === seq.current) setError(e instanceof Error ? e.message : '불러오지 못했어요');
    } finally {
      if (my === seq.current) setLoading(false);
    }
  }, [run]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, setData, error, loading, reload };
}
