# Specification: Environmental Context

## Goal

Provide context to why laptimes might vary (e.g., a "Green" track vs. a rubbered-in track).

## Functional Requirements

- **Condition Snapshot:** Capture `trackGrip`, `rainIntensity`, and `trackTemp` at the start of every lap.
- **Insight Generation:** When displaying lap history, categorize laps by "Dry", "Damp", or "Wet" conditions.

## Business Rules

- If `rainIntensity` > 0.3, the lap is automatically tagged as "Wet Weather Practice".