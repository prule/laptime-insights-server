/**
 * REST resource types — these mirror the JSON produced by the Ktor/Exposed
 * backend. Kotlin's `@JvmInline value class` types serialize as their wrapped
 * primitive (string / long / boolean), so we model them here as the underlying
 * primitive rather than a wrapper object.
 */
export {};
