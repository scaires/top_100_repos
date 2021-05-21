package com.stevecc.topgithubrepos.api.repository

import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path

/*
 * Service to access the GitHub Repository API: https://docs.github.com/en/rest/reference/repos
 * Implementation generated by Retrofit
 */
interface RepositoryService {
    companion object {
        const val BASE_URL = "repos"
    }

    // https://docs.github.com/en/rest/reference/repos#list-repository-contributors
    @GET("$BASE_URL/{owner}/{repo}/contributors")
    fun contributors(@Path("owner") owner: String, @Path("repo") repo: String): Single<List<Contributor>>
}