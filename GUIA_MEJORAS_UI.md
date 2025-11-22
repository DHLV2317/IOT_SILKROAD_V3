# 🎨 Guía de Uso - Mejoras UI/UX Material Design 3

## 📋 Resumen de Mejoras Implementadas

### ✅ **IMPLEMENTADO - Prioridad ALTA**

#### 1. **Animaciones Básicas** (6 archivos)
📁 `/res/anim/`

**Archivos creados:**
- `slide_in_right.xml` - Entrada desde derecha
- `slide_in_left.xml` - Entrada desde izquierda
- `slide_out_right.xml` - Salida hacia derecha
- `slide_out_left.xml` - Salida hacia izquierda
- `fade_in.xml` - Aparición gradual
- `fade_out.xml` - Desaparición gradual

**Uso en código Kotlin:**
```kotlin
// Transición entre activities
startActivity(Intent(this, NextActivity::class.java))
overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

// Animación en view
view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))

// ViewGroup transition
val transition = Slide(Gravity.END).apply { 
    duration = 300 
}
TransitionManager.beginDelayedTransition(container, transition)
```

---

#### 2. **Estados Vacíos** 
📁 `/res/layout/layout_empty_state.xml`

**Componentes:**
- ImageView (ilustración)
- Título: "No hay tours disponibles"
- Descripción explicativa
- Botón de acción

**Uso en código:**
```kotlin
// En tu Activity/Fragment
val emptyView = findViewById<LinearLayout>(R.id.empty_state_container)
val contentView = findViewById<RecyclerView>(R.id.recycler_view)

fun showEmptyState() {
    emptyView.visibility = View.VISIBLE
    contentView.visibility = View.GONE
}

fun showContent() {
    emptyView.visibility = View.GONE
    contentView.visibility = View.VISIBLE
}

// Personalizar mensaje
findViewById<TextView>(R.id.empty_title).text = "No hay reservas"
findViewById<TextView>(R.id.empty_description).text = "Aún no has realizado ninguna reserva"
findViewById<Button>(R.id.empty_action_button).apply {
    text = "Explorar tours"
    setOnClickListener { /* navegar a tours */ }
}
```

**Uso en XML:**
```xml
<FrameLayout>
    <!-- Tu contenido normal -->
    <RecyclerView android:id="@+id/recycler_view" ... />
    
    <!-- Estado vacío -->
    <include 
        android:id="@+id/empty_state_container"
        layout="@layout/layout_empty_state"
        android:visibility="gone" />
</FrameLayout>
```

---

#### 3. **Loading States**
📁 `/res/layout/layout_loading_state.xml`

**Componentes:**
- CircularProgressIndicator M3
- TextView "Cargando..."

**Uso en código:**
```kotlin
val loadingView = findViewById<LinearLayout>(R.id.loading_state_container)
val contentView = findViewById<ViewGroup>(R.id.content_container)

fun showLoading() {
    loadingView.visibility = View.VISIBLE
    contentView.visibility = View.GONE
}

fun hideLoading() {
    loadingView.visibility = View.GONE
    contentView.visibility = View.VISIBLE
}

// Personalizar mensaje
findViewById<TextView>(R.id.loading_text).text = "Procesando pago..."
```

**Uso en XML:**
```xml
<FrameLayout>
    <!-- Tu contenido -->
    <LinearLayout android:id="@+id/content_container" ... />
    
    <!-- Loading -->
    <include 
        android:id="@+id/loading_state_container"
        layout="@layout/layout_loading_state"
        android:visibility="gone" />
</FrameLayout>
```

---

#### 4. **Estado de Error**
📁 `/res/layout/layout_error_state.xml`

**Uso:**
```kotlin
fun showError(message: String) {
    errorView.visibility = View.VISIBLE
    contentView.visibility = View.GONE
    
    findViewById<Button>(R.id.retry_button).setOnClickListener {
        loadData() // Reintentar carga
    }
}
```

---

### ✅ **IMPLEMENTADO - Prioridad MEDIA**

#### 5. **Bottom Sheet con Filtros**
📁 `/res/layout/bottom_sheet_filter_tours.xml`

**Componentes:**
- ChipGroup para destinos (Arequipa, Cusco, Puno, Lima)
- RangeSlider para precios
- ChipGroup para duración
- Botones "Limpiar" y "Aplicar"

**Uso en código:**
```kotlin
class ToursActivity : AppCompatActivity() {
    
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tours)
        
        // Setup Bottom Sheet
        val bottomSheet = findViewById<LinearLayout>(R.id.bottom_sheet_filter)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        
        // Configurar listeners
        findViewById<Button>(R.id.btn_apply_filters).setOnClickListener {
            applyFilters()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        
        findViewById<Button>(R.id.btn_clear_filters).setOnClickListener {
            clearFilters()
        }
        
        // Listener de chips
        findViewById<ChipGroup>(R.id.chip_group_destination).setOnCheckedStateChangeListener { 
            group, checkedIds ->
            // Filtrar por destinos seleccionados
        }
        
        // Listener de slider
        findViewById<RangeSlider>(R.id.price_range_slider).addOnChangeListener { 
            slider, value, fromUser ->
            findViewById<TextView>(R.id.tv_min_price).text = "S/ ${slider.values[0].toInt()}"
            findViewById<TextView>(R.id.tv_max_price).text = "S/ ${slider.values[1].toInt()}"
        }
    }
    
    private fun applyFilters() {
        val selectedDestinations = mutableListOf<String>()
        if (findViewById<Chip>(R.id.chip_arequipa).isChecked) 
            selectedDestinations.add("Arequipa")
        // ... obtener otros filtros
        
        // Aplicar filtros a RecyclerView
        toursAdapter.filter(selectedDestinations, priceRange, duration)
    }
}
```

**Uso en XML:**
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <!-- Tu contenido principal -->
    <RecyclerView ... />
    
    <!-- Bottom Sheet -->
    <include layout="@layout/bottom_sheet_filter_tours" />
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

---

#### 6. **Dialog de Confirmación M3**
📁 `/res/layout/dialog_confirm_action.xml`

**Uso:**
```kotlin
fun showConfirmDialog(message: String, onConfirm: () -> Unit) {
    val dialog = Dialog(this)
    dialog.setContentView(R.layout.dialog_confirm_action)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    
    dialog.findViewById<TextView>(R.id.dialog_message).text = message
    
    dialog.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
        onConfirm()
        dialog.dismiss()
    }
    
    dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
        dialog.dismiss()
    }
    
    dialog.show()
}

// Uso:
showConfirmDialog("¿Estás seguro de eliminar este tour?") {
    deleteTour()
}
```

---

#### 7. **Splash Screen API**
📁 `/res/values/themes.xml` - Theme.App.Starting

**Configuración en AndroidManifest.xml:**
```xml
<application
    android:theme="@style/Theme.App.Starting">
    
    <activity
        android:name=".MainActivity"
        android:exported="true"
        android:theme="@style/Theme.App.Starting">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

**En MainActivity.kt:**
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar splash screen antes de super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
```

**Dependencia necesaria en build.gradle:**
```gradle
dependencies {
    implementation "androidx.core:core-splashscreen:1.0.1"
}
```

---

### ✅ **IMPLEMENTADO - Prioridad BAJA**

#### 8. **Motion Layout - Onboarding**
📁 `/res/layout/activity_onboarding_motion.xml`
📁 `/res/xml/scene_onboarding.xml`

**Características:**
- Transición suave entre pantallas de onboarding
- Swipe gesture para navegar
- KeyFrames para animaciones complejas
- Indicadores de página

**Uso:**
```kotlin
class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var motionLayout: MotionLayout
    private var currentPage = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_motion)
        
        motionLayout = findViewById(R.id.onboarding_content)
        
        findViewById<Button>(R.id.btn_next).setOnClickListener {
            if (currentPage < 2) {
                currentPage++
                motionLayout.transitionToEnd()
                updateContent(currentPage)
            } else {
                // Ir a MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        
        // Listener de transición
        motionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                // Actualizar contenido cuando termine la animación
            }
            // ... otros métodos
        })
    }
    
    private fun updateContent(page: Int) {
        when(page) {
            0 -> {
                findViewById<TextView>(R.id.onboarding_title).text = "Bienvenido"
                findViewById<ImageView>(R.id.onboarding_image).setImageResource(R.drawable.onboarding_1)
            }
            1 -> {
                findViewById<TextView>(R.id.onboarding_title).text = "Explora"
                findViewById<ImageView>(R.id.onboarding_image).setImageResource(R.drawable.onboarding_2)
            }
            2 -> {
                findViewById<TextView>(R.id.onboarding_title).text = "Reserva"
                findViewById<ImageView>(R.id.onboarding_image).setImageResource(R.drawable.onboarding_3)
            }
        }
    }
}
```

---

#### 9. **Motion Layout - Tour Detail con Collapsing Toolbar**
📁 `/res/layout/activity_tour_detail_motion.xml`
📁 `/res/xml/scene_tour_detail_collapse.xml`

**Características:**
- Imagen de header que se colapsa al scroll
- Título que se anima hacia el toolbar
- Transición suave de tamaños de texto
- Toolbar que aparece gradualmente

**Uso:**
```kotlin
class TourDetailMotionActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tour_detail_motion)
        
        val motionLayout = findViewById<MotionLayout>(R.id.motion_layout_root)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        // El scroll automáticamente activa la transición por el OnSwipe en el MotionScene
        // No necesitas código adicional para el colapso
    }
}
```

---

## 🎯 Ejemplos Completos de Integración

### Ejemplo 1: RecyclerView con Estados Completos

```kotlin
class ToursListFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingView: View
    private lateinit var emptyView: View
    private lateinit var errorView: View
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recycler_tours)
        loadingView = view.findViewById(R.id.loading_state_container)
        emptyView = view.findViewById(R.id.empty_state_container)
        errorView = view.findViewById(R.id.error_state_container)
        
        loadTours()
    }
    
    private fun loadTours() {
        showLoading()
        
        viewModel.tours.observe(viewLifecycleOwner) { result ->
            when {
                result.isLoading -> showLoading()
                result.isSuccess -> {
                    val tours = result.data
                    if (tours.isEmpty()) {
                        showEmpty()
                    } else {
                        showContent(tours)
                    }
                }
                result.isError -> showError(result.error)
            }
        }
    }
    
    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
        errorView.visibility = View.GONE
    }
    
    private fun showContent(tours: List<Tour>) {
        recyclerView.adapter = ToursAdapter(tours)
        recyclerView.visibility = View.VISIBLE
        loadingView.visibility = View.GONE
        emptyView.visibility = View.GONE
        errorView.visibility = View.GONE
        
        // Animación de entrada
        recyclerView.layoutAnimation = AnimationUtils
            .loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
    }
    
    private fun showEmpty() {
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        errorView.visibility = View.GONE
        
        emptyView.findViewById<Button>(R.id.empty_action_button).setOnClickListener {
            // Navegar a explorar tours
        }
    }
    
    private fun showError(message: String) {
        errorView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyView.visibility = View.GONE
        
        errorView.findViewById<Button>(R.id.retry_button).setOnClickListener {
            loadTours()
        }
    }
}
```

---

## 📦 Dependencias Necesarias

Asegúrate de tener en `build.gradle (app)`:

```gradle
dependencies {
    // Material Design 3
    implementation 'com.google.android.material:material:1.11.0'
    
    // Splash Screen
    implementation 'androidx.core:core-splashscreen:1.0.1'
    
    // ConstraintLayout (para MotionLayout)
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

---

## 🚀 Próximos Pasos Sugeridos

1. **Probar cada componente** en modo desarrollo
2. **Personalizar colores y textos** según tu marca
3. **Implementar ViewModels** para gestión de estados
4. **Añadir más animaciones** personalizadas según necesites
5. **Optimizar rendimiento** con ViewBinding/DataBinding

---

## 💡 Tips de Uso

- **Usa MotionLayout** solo cuando necesites animaciones complejas
- **Reutiliza los layouts de estados** en todas tus pantallas
- **Combina animaciones** para transiciones más fluidas
- **Mantén consistencia** en tiempos de animación (300ms recomendado)
- **Prueba en dispositivos reales** para verificar performance

¡Disfruta de tu nueva UI mejorada! 🎉
