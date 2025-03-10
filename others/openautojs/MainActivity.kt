@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)

package org.autojs.autojs.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.ModalDrawer
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stardust.app.permission.DrawOverlaysPermission
import com.stardust.util.IntentUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.autojs.autojs.Pref
import org.openautojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.external.foreground.ForegroundService
import org.autojs.autojs.timing.TimedTaskScheduler
import org.autojs.autojs.ui.build.ProjectConfigActivity
import org.autojs.autojs.ui.build.ProjectConfigActivity_
import org.autojs.autojs.ui.common.ScriptOperations
import org.autojs.autojs.ui.compose.theme.AutoXJsTheme
import org.autojs.autojs.ui.compose.theme.isLight
import org.autojs.autojs.ui.compose.widget.MyIcon
import org.autojs.autojs.ui.compose.widget.SearchBox2
import org.autojs.autojs.ui.explorer.ExplorerViewKt
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.log.LogActivityKt
import org.autojs.autojs.ui.main.drawer.DrawerPage
import org.autojs.autojs.ui.main.scripts.ScriptListFragment
import org.autojs.autojs.ui.main.task.TaskManagerFragmentKt
import org.autojs.autojs.ui.main.web.WebViewFragment
import org.autojs.autojs.ui.widget.fillMaxSize

data class BottomNavigationItem(val icon: Int, val label: String)

class MainActivity : FragmentActivity() {

    companion object {
        @JvmStatic
        fun getIntent(context: Context) = Intent(context, MainActivity::class.java)
    }

    private val scriptListFragment by lazy { ScriptListFragment() }
    private val taskManagerFragment by lazy { TaskManagerFragmentKt() }
    private val webViewFragment by lazy { WebViewFragment() }
    private var lastBackPressedTime = 0L
    private var drawerState: DrawerState? = null
    private val viewPager: ViewPager2 by lazy { ViewPager2(this) }
    private var scope: CoroutineScope? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
//                ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme

        if (Pref.isForegroundServiceEnabled()) ForegroundService.start(this)
        else ForegroundService.stop(this)

        if (Pref.isFloatingMenuShown() && !FloatyWindowManger.isCircularMenuShowing()) {
            if (DrawOverlaysPermission.isCanDrawOverlays(this)) FloatyWindowManger.showCircularMenu()
            else Pref.setFloatingMenuShown(false)
        }
        setContent {
            scope = rememberCoroutineScope()
            AutoXJsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val permission = rememberExternalStoragePermissionsState {
                        if (it) {
                            scriptListFragment.explorerView.onRefresh()
                        }
                    }
                    LaunchedEffect(key1 = Unit, block = {
                        permission.launchMultiplePermissionRequest()
                    })
                    Text(text = "test")
                    MainPage(
                        activity = this,
                        scriptListFragment = scriptListFragment,
                        taskManagerFragment = taskManagerFragment,
                        webViewFragment = webViewFragment,
                        onDrawerState = {
                            this.drawerState = it
                        },
                        viewPager = viewPager
                    )

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TimedTaskScheduler.ensureCheckTaskWorks(application)
    }

    override fun onBackPressed() {
        if (drawerState?.isOpen == true) {
            scope?.launch { drawerState?.close() }
            return
        }
        if (viewPager.currentItem == 0 && scriptListFragment.onBackPressed()) {
            return
        }
        back()
    }

    private fun back() {
        val currentTime = System.currentTimeMillis()
        val interval = currentTime - lastBackPressedTime
        if (interval > 2000) {
            lastBackPressedTime = currentTime
            Toast.makeText(
                this,
                getString(R.string.text_press_again_to_exit),
                Toast.LENGTH_SHORT
            ).show()
        } else super.onBackPressed()
    }
}

@Composable
fun MainPage(
    activity: FragmentActivity,
    scriptListFragment: ScriptListFragment,
    taskManagerFragment: TaskManagerFragmentKt,
    webViewFragment: WebViewFragment,
    onDrawerState: (DrawerState) -> Unit,
    viewPager: ViewPager2
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val context = LocalContext.current
//    val scaffoldState = rememberScaffoldState()
    onDrawerState(drawerState)
    val scope = rememberCoroutineScope()

    val bottomBarItems = remember {
        getBottomItems(context)
    }
    var currentPage by remember {
        mutableStateOf(0)
    }

//    SetSystemUI(drawerState)

    var width by remember() {
        mutableStateOf(0.dp)
    }
    val localDensity = LocalDensity.current

    ModalNavigationDrawer(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                width = with(localDensity) {
                    it.width.toDp()
                }
            },
        gesturesEnabled = drawerState.isOpen,
        drawerState = drawerState,
        drawerContent = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                DrawerPage(
                    modifier = Modifier
                        .width(width = width - 50.dp)
                        .padding(16.dp)
                )
            }
        }) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.app_name))
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.text_menu)
                            )
                        }
                    },
                    actions = {

                    }
                )
//            Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
//                Column() {
//                    Spacer(
//                        modifier = Modifier
//                            .windowInsetsTopHeight(WindowInsets.statusBars)
//                    )
                TopBar(
                    currentPage = currentPage,
                    requestOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onSearch = { keyword ->
                        scriptListFragment.explorerView.setFilter { it.name.contains(keyword) }
                    },
                    scriptListFragment = scriptListFragment,
                    webViewFragment = webViewFragment
                )
//                }
//            }
            },
            bottomBar = {
                BottomBar(bottomBarItems, currentPage, onSelectedChange = { currentPage = it })
//            Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
//                Column {
//                    BottomBar(bottomBarItems, currentPage, onSelectedChange = { currentPage = it })
////                    Spacer(
////                        modifier = Modifier
////                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
////                    )
//                }
//            }
            },
        ) {
            AndroidView(
                modifier = Modifier.padding(it),
                factory = {
                    viewPager.apply {
                        fillMaxSize()
                        adapter = ViewPager2Adapter(
                            activity,
                            scriptListFragment,
                            taskManagerFragment,
                            webViewFragment
                        )
                        offscreenPageLimit = 3
                        isUserInputEnabled = false
                        ViewCompat.setNestedScrollingEnabled(this, true)
                    }
                },
                update = { viewPager0 ->
                    viewPager0.currentItem = currentPage
                }
            )

        }

    }
}

fun showExternalStoragePermissionToast(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.text_please_enable_external_storage),
        Toast.LENGTH_SHORT
    ).show()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberExternalStoragePermissionsState(onPermissionsResult: (allAllow: Boolean) -> Unit) =
    rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
        onPermissionsResult = { map ->
            onPermissionsResult(map.all { it.value })
        })

@Composable
private fun SetSystemUI(drawerState: DrawerState) {
    val systemUiController = rememberSystemUiController()
//    val useDarkIcons =
//        if (MaterialTheme.colorScheme.isLight) {
//            drawerState.isOpen || drawerState.isAnimationRunning
//        } else false
    val useDarkIcons = !MaterialTheme.colorScheme.isLight
    val navigationUseDarkIcons = !MaterialTheme.colorScheme.isLight
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = true
        )
        systemUiController.setNavigationBarColor(
            Color.Transparent,
            darkIcons = true
        )
    }
}

private fun getBottomItems(context: Context) = mutableStateListOf(
    BottomNavigationItem(
        R.drawable.ic_home,
        context.getString(R.string.text_home)
    ),
    BottomNavigationItem(
        R.drawable.ic_manage,
        context.getString(R.string.text_management)
    ),
    BottomNavigationItem(
        R.drawable.ic_web,
        context.getString(R.string.text_document)
    )
)

@Composable
fun BottomBar(
    items: List<BottomNavigationItem>,
    currentSelected: Int,
    onSelectedChange: (Int) -> Unit
) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            val selected = currentSelected == index

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        onSelectedChange(index)
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label,
                    )
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    currentPage: Int,
    requestOpenDrawer: () -> Unit,
    onSearch: (String) -> Unit,
    scriptListFragment: ScriptListFragment,
    webViewFragment: WebViewFragment,
) {
    var isSearch by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.app_name))
        },
        navigationIcon = {
            IconButton(onClick = {
                requestOpenDrawer()
            }) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(id = R.string.text_menu)
                )
            }
        },
        actions = {
            IconButton(onClick = { LogActivityKt.start(context) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logcat),
                    contentDescription = stringResource(id = R.string.text_logcat)
                )
            }
            when (currentPage) {
                0 -> {
                    var expanded by remember {
                        mutableStateOf(false)
                    }
                    Box() {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.desc_more)
                            )
                        }
                        TopAppBarMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            scriptListFragment = scriptListFragment
                        )
                    }
                }
                1 -> {
                    IconButton(onClick = { AutoJs.getInstance().scriptEngineService.stopAll() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(id = R.string.desc_more)
                        )
                    }
                }
                2 -> {
                    IconButton(onClick = {
                        webViewFragment.swipeRefreshWebView.webView.url?.let {
                            IntentUtil.browse(context, it)
                        }
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_external_link),
                            contentDescription = stringResource(id = R.string.text_browser_open)
                        )
                    }
                }
            }
        }
    )
//    TopAppBar(title = { Text(stringResource(id = R.string.app_name)) })
//    androidx.compose.material.TopAppBar() {
//        CompositionLocalProvider(
//            LocalContentAlpha provides ContentAlpha.high,
//        ) {
//            if (!isSearch) {
//                IconButton(onClick = requestOpenDrawer) {
//                    Icon(
//                        imageVector = Icons.Default.Menu,
//                        contentDescription = stringResource(id = R.string.text_menu),
//                    )
//                }
//
//                ProvideTextStyle(value = MaterialTheme.typography.headlineSmall) {
//                    Text(
//                        modifier = Modifier.weight(1f),
//                        text = stringResource(id = R.string.app_name)
//                    )
//                }
//
//                IconButton(onClick = { isSearch = true }) {
//                    Icon(
//                        imageVector = Icons.Default.Search,
//                        contentDescription = stringResource(id = R.string.text_search)
//                    )
//                }
//            } else {
//                IconButton(onClick = { isSearch = false }) {
//                    Icon(
//                        imageVector = Icons.Default.ArrowBack,
//                        contentDescription = stringResource(id = R.string.text_exit_search)
//                    )
//                }
//
//                var keyword by remember {
//                    mutableStateOf("")
//                }
//                SearchBox2(
//                    value = keyword,
//                    onValueChange = { keyword = it },
//                    modifier = Modifier.weight(1f),
//                    placeholder = { Text(text = stringResource(id = R.string.text_search)) },
//                    keyboardActions = KeyboardActions(onSearch = {
//                        onSearch(keyword)
//                    })
//                )
//            }
//            IconButton(onClick = { LogActivityKt.start(context) }) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_logcat),
//                    contentDescription = stringResource(id = R.string.text_logcat)
//                )
//            }
//            when (currentPage) {
//                0 -> {
//                    var expanded by remember {
//                        mutableStateOf(false)
//                    }
//                    Box() {
//                        IconButton(onClick = { expanded = true }) {
//                            Icon(
//                                imageVector = Icons.Default.Add,
//                                contentDescription = stringResource(id = R.string.desc_more)
//                            )
//                        }
//                        TopAppBarMenu(
//                            expanded = expanded,
//                            onDismissRequest = { expanded = false },
//                            scriptListFragment = scriptListFragment
//                        )
//                    }
//                }
//                1 -> {
//                    IconButton(onClick = { AutoJs.getInstance().scriptEngineService.stopAll() }) {
//                        Icon(
//                            imageVector = Icons.Default.Clear,
//                            contentDescription = stringResource(id = R.string.desc_more)
//                        )
//                    }
//                }
//                2 -> {
//                    IconButton(onClick = {
//                        webViewFragment.swipeRefreshWebView.webView.url?.let {
//                            IntentUtil.browse(context, it)
//                        }
//                    }) {
//                        Icon(
//                            painterResource(id = R.drawable.ic_external_link),
//                            contentDescription = stringResource(id = R.string.text_browser_open)
//                        )
//                    }
//                }
//            }
//
//        }
//    }
}

@Composable
fun TopAppBarMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    scriptListFragment: ScriptListFragment
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, offset = offset) {
        val context = LocalContext.current
        NewDirectory(context, scriptListFragment, onDismissRequest)
        NewFile(context, scriptListFragment, onDismissRequest)
        ImportFile(context, scriptListFragment, onDismissRequest)
        NewProject(context, scriptListFragment, onDismissRequest)
//        DropdownMenuItem(onClick = { /*TODO*/ }) {
//            MyIcon(
//                painter = painterResource(id = R.drawable.ic_timed_task),
//                contentDescription = stringResource(id = R.string.text_switch_timed_task_scheduler)
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(text = stringResource(id = R.string.text_switch_timed_task_scheduler))
//        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NewDirectory(
    context: Context,
    scriptListFragment: ScriptListFragment,
    onDismissRequest: () -> Unit
) {
    val permission = rememberExternalStoragePermissionsState {
        if (it) getScriptOperations(
            context,
            scriptListFragment.explorerView
        ).newDirectory()
        else showExternalStoragePermissionToast(context)
    }
    androidx.compose.material.DropdownMenuItem(onClick = {
        onDismissRequest()
        permission.launchMultiplePermissionRequest()
    }) {
        MyIcon(
            painter = painterResource(id = R.drawable.ic_floating_action_menu_dir),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.text_directory))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NewFile(
    context: Context,
    scriptListFragment: ScriptListFragment,
    onDismissRequest: () -> Unit
) {
    val permission = rememberExternalStoragePermissionsState {
        if (it) getScriptOperations(
            context,
            scriptListFragment.explorerView
        ).newFile()
        else showExternalStoragePermissionToast(context)
    }
    androidx.compose.material.DropdownMenuItem(onClick = {
        onDismissRequest()
        permission.launchMultiplePermissionRequest()
    }) {
        MyIcon(
            painter = painterResource(id = R.drawable.ic_floating_action_menu_file),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.text_file))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ImportFile(
    context: Context,
    scriptListFragment: ScriptListFragment,
    onDismissRequest: () -> Unit
) {
    val permission = rememberExternalStoragePermissionsState {
        if (it) getScriptOperations(
            context,
            scriptListFragment.explorerView
        ).importFile()
        else showExternalStoragePermissionToast(context)
    }
    androidx.compose.material.DropdownMenuItem(onClick = {
        onDismissRequest()
        permission.launchMultiplePermissionRequest()
    }) {
        MyIcon(
            painter = painterResource(id = R.drawable.ic_floating_action_menu_open),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.text_import))
    }
}

@Composable
private fun NewProject(
    context: Context,
    scriptListFragment: ScriptListFragment,
    onDismissRequest: () -> Unit
) {
    androidx.compose.material.DropdownMenuItem(onClick = {
        onDismissRequest()
        ProjectConfigActivity_.intent(context)
            .extra(
                ProjectConfigActivity.EXTRA_PARENT_DIRECTORY,
                scriptListFragment.explorerView.currentPage?.path
            )
            .extra(ProjectConfigActivity.EXTRA_NEW_PROJECT, true)
            .start()
    }) {
        MyIcon(
            painter = painterResource(id = R.drawable.ic_project2),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.text_project))
    }
}

private fun getScriptOperations(
    context: Context,
    explorerView: ExplorerViewKt
): ScriptOperations {
    return ScriptOperations(
        context,
        explorerView,
        explorerView.currentPage
    )
}