import { useEffect, type ReactNode } from 'react';

/** 아래에서 올라오는 시트. Esc·바깥 누르면 닫힌다. */
export default function Sheet({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="sheet-wrap" onClick={onClose}>
      <div className="sheet pad" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}>
        <div className="between">
          <h2 className="h2">{title}</h2>
          <button className="btn-ghost" onClick={onClose}>닫기</button>
        </div>
        {children}
      </div>
    </div>
  );
}
