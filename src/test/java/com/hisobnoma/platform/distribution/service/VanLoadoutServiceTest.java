package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.distribution.dto.CreateVanLoadoutRequest;
import com.hisobnoma.platform.distribution.dto.ReconcileVanLoadoutRequest;
import com.hisobnoma.platform.distribution.dto.VanLoadoutDto;
import com.hisobnoma.platform.distribution.entity.VanLoadout;
import com.hisobnoma.platform.distribution.entity.VanLoadoutLine;
import com.hisobnoma.platform.distribution.entity.VanLoadoutStatus;
import com.hisobnoma.platform.distribution.mapper.VanLoadoutMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.VanLoadoutRepository;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.inventory.dto.StockTransferRequest;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VanLoadoutServiceTest {

    @Mock private VanLoadoutRepository loadoutRepository;
    @Mock private VanLoadoutMapper loadoutMapper;
    @Mock private DistributionAgentRepository agentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private StockService stockService;

    @InjectMocks private VanLoadoutService service;

    private static final Long TENANT_ID = 1L;
    private static final Long VEHICLE_LOC = 20L;
    private static final Long SOURCE_LOC = 10L;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        lenient().when(loadoutMapper.toDto(any(VanLoadout.class))).thenReturn(new VanLoadoutDto());
    }

    private Location location(Long id, LocationType type) {
        Location l = Location.builder().code("L" + id).name("Loc " + id).locationType(type).build();
        l.setId(id);
        return l;
    }

    private Product product(long id, BigDecimal cost, BigDecimal price) {
        Product p = Product.builder().sku("SKU-" + id).name("P" + id).sellingPrice(price).costPrice(cost).build();
        p.setId(id);
        return p;
    }

    private VanLoadout loadedLoadout() {
        VanLoadout loadout = VanLoadout.builder()
                .loadoutNumber("LD20260706-00001")
                .status(VanLoadoutStatus.LOADED)
                .agentId(5L).vehicleLocationId(VEHICLE_LOC).sourceLocationId(SOURCE_LOC)
                .tenantId(TENANT_ID).lines(new ArrayList<>())
                .build();
        loadout.setId(70L);
        VanLoadoutLine line = VanLoadoutLine.builder()
                .productId(100L).productName("Cola").quantityLoaded(new BigDecimal("10"))
                .unitCost(new BigDecimal("600")).unitPrice(new BigDecimal("1000"))
                .tenantId(TENANT_ID).build();
        line.setId(700L);
        loadout.addLine(line);
        return loadout;
    }

    // ---- create ----

    @Test
    void createLoadout_snapshotsCostPriceAndComputesLoadedValue() {
        CreateVanLoadoutRequest request = CreateVanLoadoutRequest.builder()
                .agentId(5L).vehicleLocationId(VEHICLE_LOC).sourceLocationId(SOURCE_LOC)
                .lines(List.of(CreateVanLoadoutRequest.LineRequest.builder()
                        .productId(100L).quantityLoaded(new BigDecimal("10")).build()))
                .build();

        when(agentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(new DistributionAgent()));
        when(locationRepository.findByIdAndTenantId(VEHICLE_LOC, TENANT_ID)).thenReturn(Optional.of(location(VEHICLE_LOC, LocationType.VEHICLE)));
        when(locationRepository.findByIdAndTenantId(SOURCE_LOC, TENANT_ID)).thenReturn(Optional.of(location(SOURCE_LOC, LocationType.WAREHOUSE)));
        when(productRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(product(100L, new BigDecimal("600"), new BigDecimal("1000"))));
        when(loadoutRepository.findMaxLoadoutNumberByPrefix(eq(TENANT_ID), anyString())).thenReturn(null);
        when(loadoutRepository.existsByTenantIdAndLoadoutNumber(eq(TENANT_ID), anyString())).thenReturn(false);
        when(loadoutRepository.save(any(VanLoadout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createLoadout(request);

        ArgumentCaptor<VanLoadout> captor = ArgumentCaptor.forClass(VanLoadout.class);
        verify(loadoutRepository).save(captor.capture());
        VanLoadout saved = captor.getValue();
        assertEquals(VanLoadoutStatus.DRAFT, saved.getStatus());
        assertTrue(saved.getLoadoutNumber().startsWith("LD"));
        assertEquals(1, saved.getLines().size());
        assertEquals(0, new BigDecimal("600").compareTo(saved.getLines().get(0).getUnitCost()));
        // loaded value = 10 * 600 = 6000
        assertEquals(0, new BigDecimal("6000").compareTo(saved.getTotalLoadedValue()));
    }

    @Test
    void createLoadout_nonVehicleTarget_throws() {
        CreateVanLoadoutRequest request = CreateVanLoadoutRequest.builder()
                .agentId(5L).vehicleLocationId(VEHICLE_LOC).sourceLocationId(SOURCE_LOC)
                .lines(List.of(CreateVanLoadoutRequest.LineRequest.builder().productId(100L).quantityLoaded(BigDecimal.ONE).build()))
                .build();
        when(agentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(new DistributionAgent()));
        when(locationRepository.findByIdAndTenantId(VEHICLE_LOC, TENANT_ID)).thenReturn(Optional.of(location(VEHICLE_LOC, LocationType.WAREHOUSE)));

        assertThrows(BusinessException.class, () -> service.createLoadout(request));
    }

    // ---- load ----

    @Test
    void load_transfersWarehouseToVehicleAndSetsLoaded() {
        VanLoadout loadout = loadedLoadout();
        loadout.setStatus(VanLoadoutStatus.DRAFT);
        when(loadoutRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(loadout));
        when(loadoutRepository.save(any(VanLoadout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.load(70L);

        assertEquals(VanLoadoutStatus.LOADED, loadout.getStatus());
        ArgumentCaptor<StockTransferRequest> captor = ArgumentCaptor.forClass(StockTransferRequest.class);
        verify(stockService).transferStock(captor.capture());
        StockTransferRequest tr = captor.getValue();
        assertEquals(SOURCE_LOC, tr.getFromLocationId());
        assertEquals(VEHICLE_LOC, tr.getToLocationId());
        assertEquals(1, tr.getItems().size());
        assertEquals(0, new BigDecimal("10").compareTo(tr.getItems().get(0).getQuantity()));
    }

    @Test
    void load_alreadyLoaded_throws() {
        when(loadoutRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(loadedLoadout()));

        assertThrows(BusinessException.class, () -> service.load(70L));
        verify(stockService, never()).transferStock(any());
    }

    // ---- reconcile ----

    @Test
    void reconcile_derivesSoldAndMovesStockAndComputesCash() {
        VanLoadout loadout = loadedLoadout();
        when(loadoutRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(loadout));
        when(loadoutRepository.save(any(VanLoadout.class))).thenAnswer(inv -> inv.getArgument(0));
        when(securityContextHelper.getCurrentUserId()).thenReturn(42L);

        ReconcileVanLoadoutRequest request = ReconcileVanLoadoutRequest.builder()
                .actualCash(new BigDecimal("7000"))
                .lines(List.of(ReconcileVanLoadoutRequest.LineReconcile.builder()
                        .lineId(700L).quantityReturned(new BigDecimal("2")).quantityDamaged(new BigDecimal("1")).build()))
                .build();

        service.reconcile(70L, request);

        VanLoadoutLine line = loadout.getLines().get(0);
        assertEquals(0, new BigDecimal("7").compareTo(line.getQuantitySold())); // 10 - 2 - 1
        assertEquals(VanLoadoutStatus.RECONCILED, loadout.getStatus());
        assertEquals(42L, loadout.getReconciledBy());
        // expected cash = 7 sold * 1000 price
        assertEquals(0, new BigDecimal("7000").compareTo(loadout.getExpectedCash()));
        assertEquals(0, BigDecimal.ZERO.compareTo(loadout.getCashDifference()));
        // sold + damaged issued from vehicle (atomic, direct StockService calls)
        verify(stockService).deductStockForSale(100L, VEHICLE_LOC, new BigDecimal("7"),
                MovementReferenceType.VAN_LOADOUT, 70L, "LD20260706-00001");
        verify(stockService).deductStockForSale(100L, VEHICLE_LOC, new BigDecimal("1"),
                MovementReferenceType.VAN_LOADOUT, 70L, "LD20260706-00001");
        // returns transferred back vehicle -> warehouse
        ArgumentCaptor<StockTransferRequest> tr = ArgumentCaptor.forClass(StockTransferRequest.class);
        verify(stockService).transferStock(tr.capture());
        assertEquals(VEHICLE_LOC, tr.getValue().getFromLocationId());
        assertEquals(SOURCE_LOC, tr.getValue().getToLocationId());
        assertEquals(0, new BigDecimal("2").compareTo(tr.getValue().getItems().get(0).getQuantity()));
    }

    @Test
    void reconcile_returnedPlusDamagedExceedsLoaded_throws() {
        VanLoadout loadout = loadedLoadout();
        when(loadoutRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(loadout));

        ReconcileVanLoadoutRequest request = ReconcileVanLoadoutRequest.builder()
                .actualCash(BigDecimal.ZERO)
                .lines(List.of(ReconcileVanLoadoutRequest.LineReconcile.builder()
                        .lineId(700L).quantityReturned(new BigDecimal("8")).quantityDamaged(new BigDecimal("5")).build()))
                .build();

        assertThrows(BusinessException.class, () -> service.reconcile(70L, request));
        verify(stockService, never()).deductStockForSale(any(), any(), any(), any(), any(), any());
    }

    @Test
    void reconcile_cashShortfallIsNegativeDifference() {
        VanLoadout loadout = loadedLoadout();
        when(loadoutRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(loadout));
        when(loadoutRepository.save(any(VanLoadout.class))).thenAnswer(inv -> inv.getArgument(0));

        // all 10 sold -> expected 10000, agent hands in 9000 -> shortfall -1000
        ReconcileVanLoadoutRequest request = ReconcileVanLoadoutRequest.builder()
                .actualCash(new BigDecimal("9000"))
                .lines(List.of(ReconcileVanLoadoutRequest.LineReconcile.builder().lineId(700L).build()))
                .build();

        service.reconcile(70L, request);

        assertEquals(0, new BigDecimal("10000").compareTo(loadout.getExpectedCash()));
        assertEquals(0, new BigDecimal("-1000").compareTo(loadout.getCashDifference()));
    }

    // ---- cancel / delete ----

    @Test
    void cancel_loadedLoadout_returnsAllStock() {
        VanLoadout loadout = loadedLoadout();
        when(loadoutRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(loadout));
        when(loadoutRepository.save(any(VanLoadout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancel(70L);

        assertEquals(VanLoadoutStatus.CANCELLED, loadout.getStatus());
        ArgumentCaptor<StockTransferRequest> tr = ArgumentCaptor.forClass(StockTransferRequest.class);
        verify(stockService).transferStock(tr.capture());
        assertEquals(VEHICLE_LOC, tr.getValue().getFromLocationId());
        assertEquals(SOURCE_LOC, tr.getValue().getToLocationId());
        assertEquals(0, new BigDecimal("10").compareTo(tr.getValue().getItems().get(0).getQuantity()));
    }

    @Test
    void cancel_draftLoadout_noStockMovement() {
        VanLoadout loadout = loadedLoadout();
        loadout.setStatus(VanLoadoutStatus.DRAFT);
        when(loadoutRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(loadout));
        when(loadoutRepository.save(any(VanLoadout.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancel(70L);

        assertEquals(VanLoadoutStatus.CANCELLED, loadout.getStatus());
        verify(stockService, never()).transferStock(any());
    }

    @Test
    void deleteLoadout_nonDraft_throws() {
        when(loadoutRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(loadedLoadout()));

        assertThrows(BusinessException.class, () -> service.deleteLoadout(70L));
        verify(loadoutRepository, never()).delete(any());
    }
}
