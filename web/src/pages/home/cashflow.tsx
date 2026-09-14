import { useMemo, useState } from "react";
import type { DashboardCashflow } from "../../api";
import { formatBrlFromCents, monthLabel, shortMonth } from "../../format";

const INCOME = "#2A7A6E";
const PAID = "#B85C4A";
const FORECAST = "#C9A94E";

function linePath(values: number[], max: number, w: number, h: number, pad: number) {
  const n = Math.max(values.length, 2);
  return values
    .map((v, i) => {
      const x = pad + (i * (w - pad * 2)) / (n - 1);
      const y = h - pad - (v / max) * (h - pad * 2);
      return `${i === 0 ? "M" : "L"} ${x} ${y}`;
    })
    .join(" ");
}

function areaPath(values: number[], max: number, w: number, h: number, pad: number) {
  const n = Math.max(values.length, 2);
  const top = linePath(values, max, w, h, pad);
  const lastX = pad + ((values.length - 1) * (w - pad * 2)) / (n - 1);
  return `${top} L ${lastX} ${h - pad} L ${pad} ${h - pad} Z`;
}

export function HomeCashflow({
  data,
  dynamic,
  forecastMonths,
  onDynamicChange,
  onForecastChange,
}: {
  data: DashboardCashflow;
  dynamic: boolean;
  forecastMonths: number;
  onDynamicChange: (next: boolean) => void;
  onForecastChange: (next: number) => void;
}) {
  const [showIncome, setShowIncome] = useState(true);
  const [showPaid, setShowPaid] = useState(true);
  const [showForecast, setShowForecast] = useState(true);
  const [picked, setPicked] = useState<number | null>(null);

  const months = data.months ?? [];
  const income = data.income_cents ?? [];
  const paid = data.expense_paid_cents ?? [];
  const forecast = data.expense_forecast_cents ?? [];

  const max = useMemo(() => {
    const series = [
      ...(showIncome ? income : []),
      ...(showPaid ? paid : []),
      ...(showForecast ? forecast : []),
    ];
    return Math.max(1, ...series);
  }, [forecast, income, paid, showForecast, showIncome, showPaid]);

  const w = 360;
  const h = 150;
  const pad = 16;
  const n = Math.max(months.length, 2);
  const defaultIndex = (() => {
    for (let i = months.length - 1; i >= 0; i -= 1) {
      if ((income[i] ?? 0) + (paid[i] ?? 0) + (forecast[i] ?? 0) > 0) return i;
    }
    return Math.max(0, months.length - 1);
  })();
  const selected = picked ?? defaultIndex;

  if (months.length < 2) {
    return (
      <p className="py-10 text-center text-sm text-muted">
        Sem dados de séries para o gráfico.
      </p>
    );
  }

  const month = months[selected];
  const allHidden = !showIncome && !showPaid && !showForecast;

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2 text-xs">
        <button
          type="button"
          className="rounded-full border border-navy/10 bg-white px-3 py-1 text-navy"
          onClick={() => onDynamicChange(!dynamic)}
        >
          {dynamic ? "Janela dinâmica" : "Eixo fixo"}
        </button>
        <div className="flex items-center gap-2 text-navy">
          <button
            type="button"
            className="px-1"
            onClick={() => onForecastChange(Math.max(1, forecastMonths - 1))}
          >
            −
          </button>
          <span>Previsão: {forecastMonths} meses</span>
          <button
            type="button"
            className="px-1"
            onClick={() => onForecastChange(Math.min(12, forecastMonths + 1))}
          >
            +
          </button>
        </div>
      </div>
      <div className="mb-2 flex flex-wrap gap-2">
        <LegendChip
          color={INCOME}
          label="Proventos"
          on={showIncome}
          onClick={() => setShowIncome((v) => !v)}
        />
        <LegendChip
          color={PAID}
          label="Despesas pagas"
          on={showPaid}
          onClick={() => setShowPaid((v) => !v)}
        />
        <LegendChip
          color={FORECAST}
          dashed
          label="Despesas pendentes (prev.)"
          on={showForecast}
          onClick={() => setShowForecast((v) => !v)}
        />
      </div>
      {allHidden ? (
        <p className="py-10 text-center text-sm text-muted">
          Ative pelo menos uma série na legenda.
        </p>
      ) : (
        <>
      <svg viewBox={`0 0 ${w} ${h}`} className="h-40 w-full">
        {showIncome ? (
          <>
            <path d={areaPath(income, max, w, h, pad)} fill={INCOME} opacity="0.12" />
            <path
              d={linePath(income, max, w, h, pad)}
              fill="none"
              stroke={INCOME}
              strokeWidth="2.2"
            />
          </>
        ) : null}
        {showPaid ? (
          <>
            <path d={areaPath(paid, max, w, h, pad)} fill={PAID} opacity="0.12" />
            <path
              d={linePath(paid, max, w, h, pad)}
              fill="none"
              stroke={PAID}
              strokeWidth="2.2"
            />
          </>
        ) : null}
        {showForecast ? (
          <path
            d={linePath(forecast, max, w, h, pad)}
            fill="none"
            stroke={FORECAST}
            strokeWidth="2"
            strokeDasharray="5 4"
          />
        ) : null}
        {months.map((_, i) => {
          const x = pad + (i * (w - pad * 2)) / (n - 1);
          return (
            <circle
              key={i}
              cx={x}
              cy={8}
              r={10}
              fill="transparent"
              className="cursor-pointer"
              onClick={() => setPicked(i)}
            />
          );
        })}
        {months.map((_, i) => {
          const x = pad + (i * (w - pad * 2)) / (n - 1);
          return (
            <rect
              key={`hit-${i}`}
              x={x - 10}
              y={pad}
              width="20"
              height={h - pad * 2}
              fill={i === selected ? "rgba(27,44,65,0.06)" : "transparent"}
              className="cursor-pointer"
              onClick={() => setPicked(i)}
            />
          );
        })}
      </svg>
      <div className="mt-1 flex justify-between text-[11px] uppercase tracking-wide text-muted">
        <span>{shortMonth(months[0].year, months[0].month)}</span>
        <span>
          {shortMonth(months[months.length - 1].year, months[months.length - 1].month)}
        </span>
      </div>
      {month ? (
        <div className="mt-3 rounded-lg bg-cream-muted/70 px-3 py-2 text-sm">
          <p className="text-navy">
            {monthLabel(month.year, month.month).replace(/^./, (c) =>
              c.toLocaleUpperCase("pt-BR"),
            )}
          </p>
          <p className="mt-1 text-xs text-muted">
            Toque no gráfico para ver o detalhe por mês.
          </p>
          <ul className="mt-2 space-y-1 text-xs">
            {showIncome ? (
              <li className="flex justify-between">
                <span style={{ color: INCOME }}>Proventos</span>
                <span>{formatBrlFromCents(income[selected] ?? 0)}</span>
              </li>
            ) : null}
            {showPaid ? (
              <li className="flex justify-between">
                <span style={{ color: PAID }}>Despesas pagas</span>
                <span>{formatBrlFromCents(paid[selected] ?? 0)}</span>
              </li>
            ) : null}
            {showForecast ? (
              <li className="flex justify-between">
                <span style={{ color: FORECAST }}>Despesas pendentes (prev.)</span>
                <span>{formatBrlFromCents(forecast[selected] ?? 0)}</span>
              </li>
            ) : null}
          </ul>
        </div>
      ) : null}
        </>
      )}
    </div>
  );
}

function LegendChip({
  color,
  label,
  on,
  dashed,
  onClick,
}: {
  color: string;
  label: string;
  on: boolean;
  dashed?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] ${
        on ? "border-navy/15 bg-white text-navy" : "border-navy/10 text-muted line-through"
      }`}
    >
      <span
        className="h-1.5 w-4 rounded-full"
        style={{
          background: on ? color : "#C4BFB5",
          backgroundImage: dashed
            ? `repeating-linear-gradient(90deg, ${on ? color : "#C4BFB5"} 0 3px, transparent 3px 6px)`
            : undefined,
        }}
      />
      {label}
    </button>
  );
}
