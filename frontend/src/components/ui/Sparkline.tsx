/**
 * Inline SVG sparkline. Used for lap-time trends.
 */
export interface SparklineProps {
  values: number[];
  width?: number;
  height?: number;
  /** CSS color (token via `currentColor` works). */
  color?: string;
}

export function Sparkline({ values, width = 120, height = 36, color = "currentColor" }: SparklineProps) {
  if (values.length < 2) return null;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const stepX = width / (values.length - 1);
  const points = values
    .map((v, i) => `${(i * stepX).toFixed(2)},${(height - ((v - min) / span) * height).toFixed(2)}`)
    .join(" ");

  return (
    <svg width={width} height={height} className="block">
      <polyline fill="none" stroke={color} strokeWidth={1.5} points={points} />
    </svg>
  );
}
