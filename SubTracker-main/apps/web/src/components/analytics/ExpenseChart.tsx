import type { AnalyticsCategoryItem } from "../../types/analytics";
import { formatCurrency } from "../../utils/formatCurrency";

type ExpenseChartProps = {
  currency: string | null;
  items: AnalyticsCategoryItem[];
};

const CHART_HEIGHT = 220;
const CHART_WIDTH = 640;
const PADDING_TOP = 24;
const PADDING_RIGHT = 24;
const PADDING_BOTTOM = 56;
const PADDING_LEFT = 24;
const MAX_BARS = 6;
const BAR_COLORS = [
  ["#cfe6ff", "#7eb8ff"],
  ["#9fd1ff", "#4f9df5"],
  ["#76bcff", "#1f82ea"],
  ["#4b9dff", "#0e68d8"],
  ["#2f84e9", "#0b4fb3"],
  ["#1d5fcb", "#08398b"]
] as const;

const ExpenseChart = ({ currency, items }: ExpenseChartProps) => {
  const visibleItems = items.slice(0, MAX_BARS);
  const maxAmount = Math.max(...visibleItems.map((item) => item.amount), 1);
  const chartInnerWidth = CHART_WIDTH - PADDING_LEFT - PADDING_RIGHT;
  const chartInnerHeight = CHART_HEIGHT - PADDING_TOP - PADDING_BOTTOM;
  const step = chartInnerWidth / Math.max(visibleItems.length, 1);
  const barWidth = Math.min(68, step * 0.56);

  return (
    <div className="expense-chart">
      <svg
        className="expense-chart__svg"
        viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
        role="img"
        aria-label="Expenses by category chart"
      >
        <defs>
          {visibleItems.map((item, index) => {
            const [startColor, endColor] = BAR_COLORS[index % BAR_COLORS.length];

            return (
              <linearGradient
                key={`expense-bar-fill-${item.category}`}
                id={`expense-bar-fill-${index}`}
                x1="0%"
                y1="0%"
                x2="0%"
                y2="100%"
              >
                <stop offset="0%" stopColor={startColor} />
                <stop offset="100%" stopColor={endColor} />
              </linearGradient>
            );
          })}
        </defs>

        <line
          x1={PADDING_LEFT}
          y1={CHART_HEIGHT - PADDING_BOTTOM}
          x2={CHART_WIDTH - PADDING_RIGHT}
          y2={CHART_HEIGHT - PADDING_BOTTOM}
          className="expense-chart__axis"
        />

        {visibleItems.map((item, index) => {
          const [startColor, endColor] = BAR_COLORS[index % BAR_COLORS.length];
          const barHeight = Math.max((item.amount / maxAmount) * chartInnerHeight, 8);
          const x = PADDING_LEFT + index * step + (step - barWidth) / 2;
          const y = CHART_HEIGHT - PADDING_BOTTOM - barHeight;
          const label = item.category.length > 12 ? `${item.category.slice(0, 12)}…` : item.category;

          return (
            <g key={item.category}>
              <rect
                x={x}
                y={y}
                width={barWidth}
                height={barHeight}
                rx={16}
                className="expense-chart__bar"
                style={{ fill: `url(#expense-bar-fill-${index})` }}
              />
              <text x={x + barWidth / 2} y={y - 8} textAnchor="middle" className="expense-chart__value">
                {formatCurrency(item.amount, currency)}
              </text>
              <text
                x={x + barWidth / 2}
                y={CHART_HEIGHT - PADDING_BOTTOM + 22}
                textAnchor="middle"
                className="expense-chart__label"
              >
                {label}
              </text>
            </g>
          );
        })}
      </svg>

      <div className="expense-chart__legend">
        {visibleItems.map((item, index) => (
          <div key={`legend-${item.category}`} className="expense-chart__legend-item">
            <span
              className="expense-chart__legend-dot"
              style={{
                background: `linear-gradient(135deg, ${BAR_COLORS[index % BAR_COLORS.length][0]} 0%, ${
                  BAR_COLORS[index % BAR_COLORS.length][1]
                } 100%)`
              }}
            />
            <span>
              {item.category}: {formatCurrency(item.amount, currency)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ExpenseChart;
