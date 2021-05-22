package com.stevecc.topgithubrepos.reposlist

import com.stevecc.topgithubrepos.api.repository.RepositoryService
import com.stevecc.topgithubrepos.api.search.RepositoryResults
import com.stevecc.topgithubrepos.api.search.SearchService
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class ReposListModel @Inject constructor(
    private val repositoryService: RepositoryService,
    private val searchService: SearchService
) {

    companion object {
        const val TOP_REPOS_SORTED_BY_STARS_QUERY = "stars:>0"
    }

    fun top100Repositories(): Single<RepositoryResults> {
        return searchService.repositories(searchQuery = TOP_REPOS_SORTED_BY_STARS_QUERY, perPageCount = 100)
    }
}
