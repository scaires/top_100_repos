package com.stevecc.topgithubrepos.api.search

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

// https://docs.github.com/en/rest/reference/search#search-repositories
@JsonClass(generateAdapter = true)
@Parcelize
data class RepositoryResults(
    val total_count: Int,
    val incomplete_results: Boolean,
    val items: List<Repository>
) : Parcelable

// https://docs.github.com/en/rest/reference/search#search-repositories
@JsonClass(generateAdapter = true)
@Parcelize
data class Repository(
    val id: Int,
    val name: String,
    val full_name: String,
    val owner: Owner,
    val stargazers_count: Int
) : Parcelable

// https://docs.github.com/en/rest/reference/search#search-repositories
@JsonClass(generateAdapter = true)
@Parcelize
data class Owner(
    val id: Int,
    val login: String
) : Parcelable
