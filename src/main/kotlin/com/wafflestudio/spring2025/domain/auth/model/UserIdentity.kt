package com.wafflestudio.spring2025.domain.auth.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("user_identities")
class UserIdentity(
    @Id
    var id: Long? = null,
    @Column("user_id")
    var userId: Long,
    val provider: String,

    @CreatedDate
    @Column("created_at")
    var createdAt: Instant? = null,
)
CREATE TABLE user_identities (
id BIGINT NOT NULL AUTO_INCREMENT,
user_id BIGINT NOT NULL,
provider VARCHAR(50) NOT NULL,
provider_user_id VARCHAR(191) NOT NULL,
created_at DATETIME(6) NOT NULL,
PRIMARY KEY (id),
UNIQUE KEY uk_user_identities_provider_user (provider, provider_user_id),
CONSTRAINT fk_user_identities_user
FOREIGN KEY (user_id) REFERENCES users(id)
);
