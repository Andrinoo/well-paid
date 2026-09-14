import { thumbnailSrc } from "../../api";

export function GoalThumb({
  url,
  className = "h-14 w-14",
}: {
  url?: string | null;
  className?: string;
}) {
  const src = thumbnailSrc(url);
  if (!src) {
    return (
      <span
        className={`inline-grid shrink-0 place-items-center rounded-2xl bg-sage text-teal-deep ${className}`}
        aria-hidden
      >
        <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.8">
          <path d="M5 20V6.5c4-2 6 .5 7 2 1.2 1.8 3.5 3.6 7 1.6V18c-3.4 1.4-6-.4-7-2.2C11 14 8.2 12.4 5 14.6" />
        </svg>
      </span>
    );
  }
  return (
    <img
      src={src}
      alt=""
      referrerPolicy="no-referrer"
      className={`shrink-0 rounded-2xl bg-cream-muted object-cover ${className}`}
    />
  );
}
