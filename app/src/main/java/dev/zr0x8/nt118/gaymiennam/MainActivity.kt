package dev.zr0x8.nt118.gaymiennam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.zr0x8.nt118.gaymiennam.data.CityStore
import dev.zr0x8.nt118.gaymiennam.data.WeatherRepository
import dev.zr0x8.nt118.gaymiennam.data.friendlyMessage
import dev.zr0x8.nt118.gaymiennam.databinding.ActivityMainBinding
import dev.zr0x8.nt118.gaymiennam.ui.CityWeatherAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cityStore: CityStore
    private val repository = WeatherRepository()
    private lateinit var adapter: CityWeatherAdapter

    private var currentLocationQuery: String? = null
    private var loadJob: Job? = null
    private var locatingJob: Job? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) fetchCurrentLocation() else toast("Cần quyền vị trí để lấy thời tiết tại đây.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        cityStore = CityStore(this)
        adapter = CityWeatherAdapter(onDelete = { item ->
            if (item.isCurrentLocation) {
                currentLocationQuery = null
            } else {
                cityStore.removeCity(item.query)
            }
            loadAll()
        })
        binding.rvCities.layoutManager = LinearLayoutManager(this)
        binding.rvCities.adapter = adapter

        binding.btnAdd.setOnClickListener { addCityFromInput() }
        binding.etCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                addCityFromInput()
                true
            } else false
        }
        binding.btnRefresh.setOnClickListener { loadAll() }
        binding.btnLocate.setOnClickListener { requestLocation() }

        if (BuildConfig.WEATHER_API_KEY.isBlank()) {
            binding.tvStatus.text =
                "Thiếu API key. Tạo file .env ở thư mục gốc với API_KEY=\"...\" (xem .env.example) rồi Rebuild."
        }

        // Tự động xin vị trí hiện tại khi mở app lần đầu
        requestLocation(autoLoadIfDenied = true)
    }

    private fun addCityFromInput() {
        val input = binding.etCity.text?.toString().orEmpty().trim()
        if (input.isBlank()) {
            toast("Nhập tên thành phố trước.")
            return
        }
        // Kiểm tra nhanh thành phố có tồn tại trên WeatherAPI không rồi mới lưu
        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = repository.getWeather(input)
                val query = res.location.name.ifBlank { input }
                val added = cityStore.addCity(query)
                binding.etCity.text?.clear()
                if (!added) toast("Thành phố đã có trong danh sách.")
                loadAll()
            } catch (e: Exception) {
                toast(e.friendlyMessage())
                setLoading(false)
            }
        }
    }

    private fun requestLocation(autoLoadIfDenied: Boolean = false) {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            if (autoLoadIfDenied && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Người dùng từng từ chối: vẫn load danh sách đã lưu, không spam dialog
                loadAll()
            } else if (autoLoadIfDenied) {
                // Lần đầu mở app: hỏi quyền, nếu từ chối thì vẫn load list cũ
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                loadAll()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun fetchCurrentLocation() {
        locatingJob?.cancel()
        locatingJob = lifecycleScope.launch {
            setLoading(true, "Đang dò vị trí (GPS/Mạng)...")
            val loc = getFreshLocation()
            if (loc != null) {
                // WeatherAPI hỗ trợ q="lat,lon".
                currentLocationQuery = "${loc.latitude},${loc.longitude}"
            } else {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val gpsOn = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
                val netOn = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
                if (!gpsOn && !netOn) {
                    toast("Chưa bật định vị. Bật Location trong Settings rồi bấm 📍 lại.")
                } else {
                    toast("Không lấy được vị trí. Trên emulator: mở Extended controls (⋯) > Location > set điểm > Set Location. Dùng danh sách đã lưu.")
                }
            }
            loadAll()
        }
    }

    /**
     * 1. Thử getLastKnownLocation (nhanh).
     * 2. Nếu null thì xin fix mới qua requestSingleUpdate, timeout 12s.
     * Trả null nếu không có provider / không quyền / timeout.
     */
    private suspend fun getFreshLocation(): Location? = withContext(Dispatchers.Main) {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Bước 1: cache — nhiều máy/emulator có sẵn sau 1 lần fix
        for (p in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER) + runCatching { lm.getProviders(true) }.getOrDefault(emptyList())) {
            try {
                lm.getLastKnownLocation(p)?.let { return@withContext it }
            } catch (_: SecurityException) {
                return@withContext null
            } catch (_: IllegalArgumentException) {
                continue
            }
        }

        // Bước 2: xin fix mới
        withTimeoutOrNull(12_000) {
            suspendCancellableCoroutine { cont ->
                var done = false
                val listener = LocationListener { loc ->
                    if (!done && cont.isActive) {
                        done = true
                        cont.resume(loc)
                    }
                }
                try {
                    val targets = listOf(
                        LocationManager.NETWORK_PROVIDER,
                        LocationManager.GPS_PROVIDER
                    ).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
                    if (targets.isEmpty()) {
                        if (cont.isActive) cont.resume(null)
                        return@suspendCancellableCoroutine
                    }
                    for (provider in targets) {
                        runCatching {
                            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                        }
                    }
                    cont.invokeOnCancellation {
                        runCatching { lm.removeUpdates(listener) }
                    }
                } catch (_: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                } catch (_: IllegalArgumentException) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private fun loadAll() {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val cities = cityStore.loadCities().toMutableList()
            currentLocationQuery?.let { q ->
                if (cities.none { it.equals(q, ignoreCase = true) }) cities.add(0, q)
            }
            if (cities.isEmpty()) {
                adapter.submitList(emptyList())
                setLoading(false, "Chưa có thành phố nào. Nhập tên rồi bấm Thêm.")
                return@launch
            }
            setLoading(true, "Đang tải thời tiết...")
            try {
                val (items, err) = repository.getWeathers(cities, currentLocationQuery)
                adapter.submitList(items)
                binding.tvStatus.text = err
                    ?: if (currentLocationQuery != null) "Đã cập nhật ${items.size} nơi (📍 là vị trí hiện tại)."
                    else "Đã cập nhật ${items.size} thành phố."
            } catch (e: Exception) {
                binding.tvStatus.text = e.friendlyMessage()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean, status: String? = null) {
        binding.progress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        if (status != null) binding.tvStatus.text = status
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
