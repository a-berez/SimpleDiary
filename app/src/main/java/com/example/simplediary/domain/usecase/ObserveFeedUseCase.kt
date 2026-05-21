package com.example.simplediary.domain.usecase

import com.example.simplediary.domain.model.FeedFilter
import com.example.simplediary.domain.repository.FeedRow
import com.example.simplediary.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow

class ObserveFeedUseCase(
    private val repository: JournalRepository,
) {
    operator fun invoke(filter: FeedFilter): Flow<List<FeedRow>> = repository.observeFeed(filter)
}
