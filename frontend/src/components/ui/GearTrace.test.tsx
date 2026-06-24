// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { cleanup, render } from "@testing-library/react";
import { GearTrace } from "./GearTrace";
import type { TelemetrySample } from "../../api/types";

function sample(splinePosition: number, gear: number): TelemetrySample {
  return { splinePosition, gear, speedKph: 100, worldPosX: 0, worldPosY: 0 };
}

function renderTrace(lap1: TelemetrySample[], lap2: TelemetrySample[]) {
  return render(
    <GearTrace
      series={[
        { samples: lap1, color: "#00d4ff", label: "Anchor" },
        { samples: lap2, color: "#e8212a", label: "Challenger" },
      ]}
    />,
  );
}

afterEach(() => cleanup());

describe("GearTrace", () => {
  it("draws one stepped path per lap", () => {
    const { container } = renderTrace(
      [sample(0, 2), sample(0.5, 3), sample(1, 4)],
      [sample(0, 2), sample(0.5, 4), sample(1, 4)],
    );
    expect(container.querySelectorAll("path")).toHaveLength(2);
  });

  it("shades only where the two laps are in different gears", () => {
    // Identical gears → no mismatch shading.
    const { container: same } = renderTrace(
      [sample(0, 3), sample(1, 3)],
      [sample(0, 3), sample(1, 3)],
    );
    expect(same.querySelectorAll("rect")).toHaveLength(0);

    // Differing gears across the lap → some shading rects.
    const { container: diff } = renderTrace(
      [sample(0, 2), sample(1, 2)],
      [sample(0, 4), sample(1, 4)],
    );
    expect(diff.querySelectorAll("rect").length).toBeGreaterThan(0);
  });

  it("renders nothing when a lap has no samples", () => {
    const { container } = renderTrace([sample(0, 2), sample(1, 3)], []);
    expect(container.querySelector("svg")).toBeNull();
  });
});
