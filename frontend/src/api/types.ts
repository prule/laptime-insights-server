/**
 * REST resource types — these mirror the JSON produced by the Ktor/Exposed
 * backend. Kotlin's `@JvmInline value class` types serialize as their wrapped
 * primitive (string / long / boolean), so we model them here as the underlying
 * primitive rather than a wrapper object.
 */

export type Iso8601 = string;
export type Uid = string;

/** kotlinx.serialization renders the `Simulator` enum by name. */
export type SimulatorName = "ACC" | "F1" | (string & {});

export type Links = Record<string, string>;

export interface SessionResource {
  uid: Uid;
  startedAt: Iso8601 | null;
  endedAt: Iso8601 | null;
  simulator: SimulatorName;
  track: string | null;
  car: string | null;
  sessionType: string;
  _links: Links;
}

export interface TelemetrySample {
  splinePosition: number;
  speedKph: number;
  gear: number;
  /** ACC world-space X coordinate in metres. Used for track-map rendering. */
  worldPosX: number;
  /** ACC world-space Y coordinate in metres. Used for track-map rendering. */
  worldPosY: number;
}

export interface LapTelemetryResource {
  lapUid: Uid;
  samples: TelemetrySample[];
  _links: Links;
}

export interface LapComparisonSide {
  lapUid: Uid;
  sessionUid: Uid;
  lapNumber: number;
  /** Lap time in milliseconds. */
  lapTimeMs: number;
  valid: boolean;
  personalBest: boolean;
  samples: TelemetrySample[];
}

export interface LapComparisonResource {
  lap1: LapComparisonSide;
  lap2: LapComparisonSide;
  _links: Links;
}

export interface LapResource {
  uid: Uid;
  sessionUid: Uid;
  /** Integer car number within the session (e.g. ACC car index). */
  carId: number;
  recordedAt: Iso8601;
  /** lap time in milliseconds. */
  lapTime: number;
  lapNumber: number;
  valid: boolean;
  personalBest: boolean;
  _links: Links;
}

export interface SessionOptionsResource {
  cars: string[];
  tracks: string[];
  simulators: SimulatorName[];
  minDate: Iso8601;
  maxDate: Iso8601;
  _links: Links;
}

/** Page<T> from `utils/.../SearchRepository.kt`. */
export interface Page<T> {
  page: { page: number; size: number };
  total: number;
  items: T[];
}

export interface SessionFilters {
  car?: string;
  track?: string;
  simulator?: SimulatorName;
  from?: Iso8601;
  to?: Iso8601;
}

export interface PagingAndSort {
  page?: number;
  size?: number;
  sort?: string;
}
