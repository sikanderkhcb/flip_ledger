package com.blackink.app

import com.blackink.app.core.AppError
import com.blackink.app.core.DataResult
import com.blackink.app.domain.model.AcquisitionSource
import com.blackink.app.domain.model.AuthDraft
import com.blackink.app.domain.model.BusinessProfile
import com.blackink.app.domain.model.Cost
import com.blackink.app.domain.model.CostType
import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.DeviceCategory
import com.blackink.app.domain.model.DeviceStatus
import com.blackink.app.domain.model.LockStatus
import com.blackink.app.domain.model.Sale
import com.blackink.app.domain.model.SalesChannel
import com.blackink.app.domain.model.SubscriptionAccess
import com.blackink.app.domain.model.SubscriptionTier
import com.blackink.app.domain.model.WorkspaceType
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.domain.repository.InventoryRepository
import com.blackink.app.domain.repository.ProfileRepository
import com.blackink.app.domain.repository.SalesRepository
import com.blackink.app.domain.repository.SessionState
import com.blackink.app.domain.repository.SubscriptionRepository
import com.blackink.app.domain.usecase.AddCostUseCase
import com.blackink.app.domain.usecase.AddDeviceUseCase
import com.blackink.app.domain.usecase.CompleteSaleUseCase
import com.blackink.app.presentation.WizardStore
import com.blackink.app.presentation.screens.addcost.AddCostViewModel
import com.blackink.app.presentation.screens.adddevice.AddDeviceViewModel
import com.blackink.app.presentation.screens.auth.AuthViewModel
import com.blackink.app.presentation.screens.sale.SaleViewModel
import com.blackink.app.presentation.screens.setup.SetupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FormViewModelValidationTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun authDoesNotCallBackendUntilFieldsAreValid() = runTest(dispatcher) {
        val repository = CountingAuthRepository()
        val viewModel = AuthViewModel(repository)
        viewModel.setSignUp(true)

        viewModel.submit()
        assertEquals(0, repository.signUpCalls)
        assertEquals(setOf("name", "email", "password"), viewModel.state.value.fieldErrors.keys)

        viewModel.onName("Jordan Rivera")
        viewModel.onEmail("jordan@example.com")
        viewModel.onPassword("password1")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, repository.signUpCalls)
        assertTrue(viewModel.state.value.success)
        viewModel.clear()
    }

    @Test
    fun addDeviceWizardBlocksEachInvalidStep() = runTest(dispatcher) {
        val store = WizardStore()
        val viewModel = AddDeviceViewModel(
            store,
            AddDeviceUseCase(FakeInventoryRepository(), FakeSubscriptionRepository()),
        )
        viewModel.start()

        assertFalse(viewModel.validateStep(1))
        assertEquals(setOf("category", "model"), viewModel.fieldErrors.value.keys)

        viewModel.setCategory(DeviceCategory.PHONE)
        viewModel.setModel("iPhone 15")
        assertTrue(viewModel.validateStep(1))

        assertFalse(viewModel.validateStep(2))
        assertTrue("price" in viewModel.fieldErrors.value)
        assertTrue("source" in viewModel.fieldErrors.value)
        viewModel.setPrice("500")
        viewModel.setSource(AcquisitionSource.LOCAL)
        assertTrue(viewModel.validateStep(2))

        viewModel.setIdentifier("12A")
        assertFalse(viewModel.validateStep(3))
        assertTrue("identifier" in viewModel.fieldErrors.value)
        viewModel.clear()
    }

    @Test
    fun addCostDoesNotWriteUntilFormIsValid() = runTest(dispatcher) {
        val inventory = FakeInventoryRepository()
        val store = WizardStore().apply { selectedDeviceId = TEST_DEVICE.id }
        val viewModel = AddCostViewModel(store, AddCostUseCase(inventory))
        viewModel.start()

        viewModel.save()
        assertEquals(0, inventory.addCostCalls)
        assertEquals(setOf("type", "amount"), viewModel.fieldErrors.value.keys)

        viewModel.setType(CostType.PARTS)
        viewModel.setAmount("25.50")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, inventory.addCostCalls)
        assertTrue(viewModel.saved.value)
        viewModel.clear()
    }

    @Test
    fun saleWizardBlocksInvalidRequiredFieldsAndFees() = runTest(dispatcher) {
        val inventory = FakeInventoryRepository()
        val store = WizardStore().apply { selectedDeviceId = TEST_DEVICE.id }
        val viewModel = SaleViewModel(
            store,
            inventory,
            CompleteSaleUseCase(FakeSalesRepository()),
        )
        viewModel.start()
        advanceUntilIdle()

        assertFalse(viewModel.validateStep(1))
        assertTrue("price" in viewModel.state.value.fieldErrors)
        assertTrue("channel" in viewModel.state.value.fieldErrors)

        viewModel.setPrice("750")
        viewModel.setChannel(SalesChannel.IN_PERSON)
        assertTrue(viewModel.validateStep(1))

        viewModel.setPlatformFee("1.234")
        assertFalse(viewModel.validateStep(2))
        assertTrue("platformFee" in viewModel.state.value.fieldErrors)
        viewModel.clear()
    }

    @Test
    fun setupDoesNotSaveBlankBusinessOrPartnerNames() = runTest(dispatcher) {
        val repository = CountingProfileRepository()
        val viewModel = SetupViewModel(repository)
        viewModel.start()
        advanceUntilIdle()

        assertFalse(viewModel.validateStep(2))
        viewModel.setBusinessName("Circuit Flip")
        assertTrue(viewModel.validateStep(2))

        viewModel.setWorkspace(WorkspaceType.PARTNER)
        viewModel.setPartnerName("")
        var saved = false
        viewModel.finish { saved = true }
        assertEquals(0, repository.updateCalls)
        assertTrue("partnerName" in viewModel.state.value.fieldErrors)

        viewModel.setPartnerName("Alex")
        viewModel.finish { saved = true }
        advanceUntilIdle()
        assertEquals(1, repository.updateCalls)
        assertTrue(saved)
        viewModel.clear()
    }
}

private class CountingAuthRepository : AuthRepository {
    var signUpCalls = 0
    override val isAuthenticated: Flow<Boolean> = MutableStateFlow(false)
    override val sessionState: Flow<SessionState> = MutableStateFlow(SessionState.UNAUTHENTICATED)

    override suspend fun signIn(email: String, password: String) = DataResult.Success(Unit)
    override suspend fun signUp(draft: AuthDraft): DataResult<Unit> {
        signUpCalls += 1
        return DataResult.Success(Unit)
    }
    override suspend fun signInWithApple(identityToken: String) = DataResult.Success(Unit)
    override suspend fun signInWithGoogle(identityToken: String) = DataResult.Success(Unit)
    override suspend fun signOut() = DataResult.Success(Unit)
    override suspend fun syncProfileName() = Unit
}

private class FakeInventoryRepository : InventoryRepository {
    private val devices = MutableStateFlow(listOf(TEST_DEVICE))
    var addCostCalls = 0
    override val error: StateFlow<AppError?> = MutableStateFlow(null)
    override fun observeInventory(): Flow<List<Device>> = devices
    override fun observeDevice(id: String): Flow<Device?> =
        devices.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getDevice(id: String): Device? = devices.value.firstOrNull { it.id == id }
    override suspend fun addDevice(device: Device): DataResult<Device> = DataResult.Success(device)
    override suspend fun updateStatus(deviceId: String, status: DeviceStatus) = DataResult.Success(Unit)
    override suspend fun addCost(deviceId: String, cost: Cost): DataResult<Unit> {
        addCostCalls += 1
        return DataResult.Success(Unit)
    }
    override suspend fun deleteDevice(deviceId: String) = DataResult.Success(Unit)
    override fun clearCache() { devices.value = emptyList() }
    override fun removeCachedDevice(deviceId: String) {
        devices.value = devices.value.filterNot { it.id == deviceId }
    }
}

private class FakeSalesRepository : SalesRepository {
    override val error: StateFlow<AppError?> = MutableStateFlow(null)
    override fun observeSales(): Flow<List<Sale>> = MutableStateFlow(emptyList())
    override suspend fun recordSale(sale: Sale, soldDeviceId: String): DataResult<Sale> =
        DataResult.Success(sale)
    override fun clearCache() = Unit
}

private class FakeSubscriptionRepository : SubscriptionRepository {
    private val access = MutableStateFlow(SubscriptionAccess())
    override fun observeAccess(): Flow<SubscriptionAccess> = access
    override suspend fun refresh() = DataResult.Success(access.value)
    override suspend fun createCheckoutSession(tier: SubscriptionTier) =
        DataResult.Success("https://example.com/checkout")
    override suspend fun createPortalSession() = DataResult.Success("https://example.com/portal")
    override fun recordDeviceAdded() {
        access.value = access.value.copy(
            lifetimeDevicesCreated = access.value.lifetimeDevicesCreated + 1,
        )
    }
    override fun clearCache() {
        access.value = SubscriptionAccess()
    }
}

private class CountingProfileRepository : ProfileRepository {
    private val profile = MutableStateFlow(BusinessProfile())
    var updateCalls = 0
    override val error: StateFlow<AppError?> = MutableStateFlow(null)
    override fun observeProfile(): Flow<BusinessProfile> = profile
    override suspend fun getProfile(): BusinessProfile = profile.value
    override suspend fun updateProfile(profile: BusinessProfile) {
        updateCalls += 1
        this.profile.value = profile
    }
    override suspend fun setOwnerName(name: String) {
        profile.value = profile.value.copy(ownerName = name)
    }
    override suspend fun isOnboarded() = false
    override suspend fun setOnboarded() = Unit
    override fun clearCache() { profile.value = BusinessProfile() }
}

private val TEST_DEVICE = Device(
    id = "device-1",
    category = DeviceCategory.PHONE,
    model = "iPhone 15",
    identifier = "No identifier on file",
    condition = null,
    storage = "128GB",
    lock = LockStatus.UNLOCKED,
    purchasePriceCents = 50_000,
    source = AcquisitionSource.LOCAL,
    purchaseDate = "2024-01-01",
    costs = emptyList(),
    status = DeviceStatus.PURCHASED,
    daysHeld = 0,
)
