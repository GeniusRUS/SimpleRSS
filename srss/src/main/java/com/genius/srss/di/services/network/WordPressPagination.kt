package com.genius.srss.di.services.network

import android.net.Uri


class WordPressPagination {

    fun currentPage(): Int? {
//        return when {
//            uri.queryParameterNames.contains(PAGINATION) -> uri.getQueryParameter(PAGINATION)?.toIntOrNull()
//            else -> null
//        }
        return null
    }

    fun nextPageLink(link: String): String? {
        val uri = Uri.parse(link)
        return when {
            uri.queryParameterNames.contains(PAGINATION) -> {
                val extractedPage = uri.getQueryParameter(PAGINATION)?.toIntOrNull() ?: return null
                val pagedUri = incrementPagedUrl(uri, extractedPage + 1)
                pagedUri.toString()
            }
            else -> {
                val pagedUri = incrementPagedUrl(uri , 2)
                pagedUri.toString()
            }
        }
    }

    private fun incrementPagedUrl(uri: Uri, page: Int): Uri {
        return uri.buildUpon()
            .addQueryParameters(uri, mapOf(PAGINATION to page.toString()))
            .build()
    }

    /*
     * Append or replace query parameters
     */
    private fun Uri.Builder.addQueryParameters(uri: Uri, params: Map<String, String>) = apply {
        if (uri.query == null) {
            appendQueryParameters(params)
        } else {
            clearQuery()
            appendQueryParameters(params)
            val names = params.keys
            uri.queryParameterNames.forEach {
                if (it !in names) appendQueryParameter(it, uri.getQueryParameter(it))
            }
        }
    }

    private fun Uri.Builder.appendQueryParameters(params: Map<String, String>) = apply {
        for ((key, value) in params.entries) {
            appendQueryParameter(key, value)
        }
    }

    companion object {
        private const val PAGINATION = "paged"
    }
}