import type { AnalyticsForecast } from "../../types/analytics";
import { formatCurrency } from "../../utils/formatCurrency";

type ForecastChartProps = {
  data: AnalyticsForecast;
  monthLabel: string;
  yearLabel: string;
};

const CHART_HEIGHT = 220;
const CHART_WIDTH = 420;
const PADDING_TOP = 24;
const PADDING_RIGHT = 24;
const PADDING_BOTTOM = 56;
const PADDING_LEFT = 24;
const FORECAST_COLORS = [
  ["#9fd1ff", "#4f9df5"],
  ["#1d5fcb", "#08398b"]
] as const;

const ForecastChart = ({ data, monthLabel, yearLabel }: ForecastChartProps) => {
  const items = [
    { label: monthLabel, value: data.monthForecast },
    { label: yearLabel, value: data.yearForecast }
  ];
  const maxAmount = Math.max(...items.map((item) => item.value), 1);
  const chartInnerWidth = CHART_WIDTH - PADDING_LEFT - PADDING_RIGHT;
  const chartInnerHeight = CHART_HEIGHT - PADDING_TOP - PADDING_BOTTOM;
  const step = chartInnerWidth / items.length;
  const barWidth = Math.min(92, step * 0.42);

  return (
    <div className="expense-chart">
      <svg
        className="expense-chart__svg"
        viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
        role="img"
        aria-label="Forecast chart"
      >
        <defs>
          {items.map((item, index) => (
            <linearGradient
              key={`forecast-bar-fill-${item.label}`}
              id={`forecast-bar-fill-${index}`}
              x1="0%"
              y1="0%"
              x2="0%"
              y2="100%"
            >
              <stop offset="0%" stopColor={FORECAST_COLORS[index % FORECAST_COLORS.length][0]} />
              <stop offset="100%" stopColor={FORECAST_COLORS[index % FORECAST_COLORS.length][1]} />
            </linearGradient>
          ))}
        </defs>

        <line
          x1={PADDING_LEFT}
          y1={CHART_HEIGHT - PADDING_BOTTOM}
          x2={CHART_WIDTH - PADDING_RIGHT}
          y2={CHART_HEIGHT - PADDING_BOTTOM}
          className="expense-chart__axis"
        />

        {items.map((item, index) => {
          const barHeight = Math.max((item.value / maxAmount) * chartInnerHeight, 8);
          const x = PADDING_LEFT + index * step + (step - barWidth) / 2;
          const y = CHART_HEIGHT - PADDING_BOTTOM - barHeight;

          return (
            <g key={item.label}>
              <rect
                x={x}
                y={y}
                width={barWidth}
                height={barHeight}
                rx={18}
                className="forecast-chart__bar"
                style={{ fill: `url(#forecast-bar-fill-${index})` }}
              />
              <text x={x + barWidth / 2} y={y - 8} textAnchor="middle" className="expense-chart__value">
                {formatCurrency(item.value, data.currency)}
              </text>
              <text
                x={x + barWidth / 2}
                y={CHART_HEIGHT - PADDING_BOTTOM + 22}
                textAnchor="middle"
                className="expense-chart__label"
              >
                {item.label}
              </text>
            </g>
          );
        })}
      </svg>

      <div className="expense-chart__legend">
        {items.map((item, index) => (
          <div key={`forecast-${item.label}`} className="expense-chart__legend-item">
            <span
              className="forecast-chart__legend-dot"
              style={{
                background: `linear-gradient(135deg, ${FORECAST_COLORS[index % FORECAST_COLORS.length][0]} 0%, ${
                  FORECAST_COLORS[index % FORECAST_COLORS.length][1]
                } 100%)`
              }}
            />
            <span>
              {item.label}: {formatCurrency(item.value, data.currency)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ForecastChart;
