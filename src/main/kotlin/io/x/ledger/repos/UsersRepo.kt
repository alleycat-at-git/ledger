package io.x.ledger.repos

import io.x.ledger.models.CreateUser
import io.x.ledger.models.UpdateUser
import io.x.ledger.models.User
import io.x.ledger.utils.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.*
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.stereotype.Component
import java.util.*

interface UsersRepo {
    fun list(page: Int? = null, size: Int? = null): Flow<User>
    suspend fun find(uuid: UUID): User
    suspend fun findByEmail(email: String): User
    suspend fun create(user: CreateUser): User
    suspend fun update(uuid: UUID, user: UpdateUser): User
    suspend fun delete(uuid: UUID): Void?
    suspend fun isUnique(uuid: UUID, email: String): Boolean
}

@Component
class UsersRepoImpl(private val conn: DatabaseClient): UsersRepo {
    override fun list(page: Int?, size: Int?): Flow<User> =
            conn.select()
                    .from("users")
                    .page(
                            PageRequest.of(
                                    page ?: 0,
                                    (size ?: DEFAULT_PAGE_SIZE).coerceAtMost(MAX_PAGE_SIZE),
                                    Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD)
                            )
                    )
                    .asType<User>()
                    .fetch()
                    .flow()

    override suspend fun find(uuid: UUID): User =
            conn.select()
                    .from(User::class.java)
                    .matching(where("uuid").`is`(uuid))
                    .page(PageRequest.of(0, 1))
                    .fetch()
                    .awaitOne()

    override suspend fun findByEmail(email: String): User =
            conn.select()
                    .from(User::class.java)
                    .matching(where("email").`is`(email))
                    .page(PageRequest.of(0, 1))
                    .fetch()
                    .awaitOne()

    override suspend fun create(user: CreateUser): User =
        conn.insert()
                .into(CreateUser::class.java)
                .using(user)
                .fetch()
                .awaitOne()
                .toObject(User::class)

    override suspend fun update(uuid: UUID, user: UpdateUser): User {
        conn.update()
            .table("users")
            .using(toUpdate(user, UpdateUser::class))
            .matching(where("uuid").`is`(uuid))
            .then()
            .awaitFirstOrNull()
        return this.find(uuid)
    }

    override suspend fun delete(uuid: UUID): Void? =
        conn.delete()
                .from(User::class.java)
                .matching(where("uuid").`is`(uuid))
                .then()
                .awaitFirstOrNull()

    override suspend fun isUnique(uuid: UUID, email: String): Boolean {
        val count = conn.execute("SELECT COUNT(*) FROM users WHERE uuid = :uuid OR email = :email")
                .bind("uuid", uuid)
                .bind("email", email)
                .asType<Long>()
                .fetch()
                .awaitOne()
        return count == 0L
    }
}
