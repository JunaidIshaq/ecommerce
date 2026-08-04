package com.shopfast.inventoryservice.repository;

import com.shopfast.inventoryservice.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findByProductId(UUID productId);

    /**
     * Atomically moves stock from available -> reserved.
     *
     * <p>The {@code availableQuantity >= :qty} predicate is evaluated by the database as part
     * of the UPDATE, so two concurrent reservations cannot both observe sufficient stock.
     * This replaces the previous read-check-write in the service layer, which could oversell
     * under concurrency because nothing held a lock between the check and the write.</p>
     *
     * @return 1 if the reservation succeeded, 0 if there was insufficient stock.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InventoryItem i
               SET i.availableQuantity = i.availableQuantity - :qty,
                   i.reservedQuantity  = i.reservedQuantity  + :qty
             WHERE i.productId = :productId
               AND i.availableQuantity >= :qty
            """)
    int tryReserve(@Param("productId") UUID productId, @Param("qty") int qty);

    /** Atomically moves stock from reserved -> available. @return 1 on success, 0 otherwise. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InventoryItem i
               SET i.reservedQuantity  = i.reservedQuantity  - :qty,
                   i.availableQuantity = i.availableQuantity + :qty
             WHERE i.productId = :productId
               AND i.reservedQuantity >= :qty
            """)
    int tryRelease(@Param("productId") UUID productId, @Param("qty") int qty);

    /** Atomically moves stock from reserved -> sold. @return 1 on success, 0 otherwise. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InventoryItem i
               SET i.reservedQuantity = i.reservedQuantity - :qty,
                   i.soldQuantity     = i.soldQuantity     + :qty
             WHERE i.productId = :productId
               AND i.reservedQuantity >= :qty
            """)
    int tryConfirm(@Param("productId") UUID productId, @Param("qty") int qty);

    /** Atomically applies a delta, refusing to drive available stock negative. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InventoryItem i
               SET i.availableQuantity = i.availableQuantity + :delta
             WHERE i.productId = :productId
               AND i.availableQuantity + :delta >= 0
            """)
    int tryAdjust(@Param("productId") UUID productId, @Param("delta") int delta);
}
