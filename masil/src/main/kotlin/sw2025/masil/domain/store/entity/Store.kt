package sw2025.masil.domain.store.entity

import jakarta.persistence.*
import sw2025.masil.domain.storeowner.entity.StoreOwner
import sw2025.masil.global.entity.BaseEntity

@Entity
@Table(name = "store")
class Store (

    @Column(name = "address", nullable = false, columnDefinition = "VARCHAR(20)")
    var address: String,

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(100)")
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_owner_id", nullable = false, columnDefinition = "BINARY(16)")
    val storeOwner: StoreOwner,
) : BaseEntity()