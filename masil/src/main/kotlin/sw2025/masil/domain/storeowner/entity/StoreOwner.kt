package sw2025.masil.domain.storeowner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import sw2025.masil.global.entity.BaseEntity

@Entity
@Table(name = "store_owner")
class StoreOwner (

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(20)")
    var name: String,

    @Column(name = "business_number", nullable = false, columnDefinition = "VARCHAR(20)")
    val businessNumber: String,
) : BaseEntity()