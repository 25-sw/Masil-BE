package sw2025.masil.domain.storeowner.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import sw2025.masil.global.entity.BaseEntity

@Entity
@Table(name = "store_owner")
class StoreOwner(
    @Column(name = "account_id", nullable = false, unique = true, columnDefinition = "VARCHAR(20)")
    var accountId: String,

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(20)")
    var name: String,

    @Column(name = "password", nullable = false)
    var password: String
) : BaseEntity()
