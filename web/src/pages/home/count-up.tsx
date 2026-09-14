import { useEffect, useRef, useState } from "react";
import { formatBrlFromCents } from "../../format";

function prefersReducedMotion(): boolean {
  return (
    typeof window !== "undefined" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

export function useCountUp(target: number, duration = 900): number {
  const [value, setValue] = useState(0);
  const fromRef = useRef(0);

  useEffect(() => {
    const from = fromRef.current;
    if (from === target || prefersReducedMotion() || duration <= 0) {
      fromRef.current = target;
      setValue(target);
      return;
    }
    let frame = 0;
    const started = performance.now();
    const tick = (now: number) => {
      const t = Math.min(1, (now - started) / duration);
      const eased = 1 - (1 - t) ** 3;
      setValue(Math.round(from + (target - from) * eased));
      if (t < 1) frame = requestAnimationFrame(tick);
      else fromRef.current = target;
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [duration, target]);

  return value;
}

export function MoneyCount({
  cents,
  className,
}: {
  cents: number;
  className?: string;
}) {
  const shown = useCountUp(cents);
  return (
    <span className={className} aria-label={formatBrlFromCents(cents)}>
      {formatBrlFromCents(shown)}
    </span>
  );
}
