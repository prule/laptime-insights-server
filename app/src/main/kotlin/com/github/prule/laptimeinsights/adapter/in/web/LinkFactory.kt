package com.github.prule.laptimeinsights.adapter.`in`.web

interface LinkFactory<T> {
  fun build(resource: T): Map<String, String>
}
