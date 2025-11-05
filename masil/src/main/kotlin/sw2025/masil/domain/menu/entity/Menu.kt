package sw2025.masil.domain.menu.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import sw2025.masil.domain.storeowner.entity.StoreOwner
import sw2025.masil.global.entity.BaseEntity

@Entity
@Table(name = "menu")
class Menu (

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(50)")
    var name: String,

    @Column(name = "price", nullable = false, columnDefinition = "INT")
    var price: Int,

    @Column(name = "description", nullable = false, columnDefinition = "VARCHAR(255)")
    var description: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_owner_id", nullable = false, columnDefinition = "BINARY(16)")
    val storeOwner: StoreOwner,
) : BaseEntity()