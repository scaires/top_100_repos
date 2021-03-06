package com.stevecc.topgithubrepos.reposlist

import com.stevecc.topgithubrepos.api.repository.Contributor
import com.stevecc.topgithubrepos.api.repository.RepositoryService
import com.stevecc.topgithubrepos.api.search.Repository
import com.stevecc.topgithubrepos.api.search.RepositoryResults
import com.stevecc.topgithubrepos.api.search.SearchService
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject
import kotlin.collections.HashMap

class ReposListModel @Inject constructor(
    private val repositoryService: RepositoryService,
    private val searchService: SearchService
) {

    private val repositoryIdToTopContributorsMap = HashMap<Int, List<Contributor>>()
    private val repositoryRequestsInFlight = HashMap<Int, Boolean>()

    companion object {
        const val TOP_REPOS_SORTED_BY_STARS_QUERY = "stars:>0"
    }

    fun top100Repositories(): Single<RepositoryResults> {
        return searchService.repositories(searchQuery = TOP_REPOS_SORTED_BY_STARS_QUERY, perPageCount = 100)
    }

    fun topContributorsForRepository(repository: Repository): Single<List<Contributor>> {
        return if (repositoryIdToTopContributorsMap.containsKey(repository.id)) {
            Single.just(repositoryIdToTopContributorsMap[repository.id])
        } else {
            if (repositoryRequestsInFlight[repository.id] == true) {
                // Don't make a new request if one is in flight
                return Single.never()
            } else {
                repositoryRequestsInFlight[repository.id] = true
                repositoryService.contributors(
                    owner = repository.owner.login,
                    repo = repository.name)
                    .doAfterSuccess {
                        // Cache the top contributors
                        repositoryIdToTopContributorsMap[repository.id] = it
                    }
                    .doFinally {
                        repositoryRequestsInFlight[repository.id] = false
                    }
                    .flatMap {
                        // Return all contributors
                        Single.just(it)
                    }
            }
        }
    }
}
