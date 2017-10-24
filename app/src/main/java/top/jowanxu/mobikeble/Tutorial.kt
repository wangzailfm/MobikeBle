package top.jowanxu.mobikeble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.util.Log
import android.widget.Toast
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author Jowan
 */
class Tutorial : IXposedHookLoadPackage {

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        when (lpParam.packageName) {
            TOP_JOWANXU_MOBIKEBLE -> checkModuleLoaded(lpParam)
            COM_MOBIKE_APP -> hookMobike(lpParam)
        }
    }

    fun loge(tag: String, content: String) = Log.e(tag, content)

    private fun tryHook(hook: () -> Unit) {
        try {
            hook()
        } catch (t: Throwable) {
            XposedBridge.log("$HOOK_ERROR$t"); loge(TAG, "$HOOK_ERROR$t")
        }
    }

    /**
     * 判断模块是否加载成功
     */
    private fun checkModuleLoaded(lpParam: XC_LoadPackage.LoadPackageParam) {
        // 获取Class
        val activityClass = XposedHelpers.findClassIfExists(TOP_JOWANXU_MOBIKEBLE_ACTIVITY, lpParam.classLoader) ?: return
        tryHook {
            // 将方法返回值返回为true
            XposedHelpers.findAndHookMethod(activityClass, HOOK_SCANLOGIN_METHOD_NAME, object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any = true
            })
        }
    }

    private fun hookMobike(lpParam: XC_LoadPackage.LoadPackageParam): Boolean {
        tryHook {
            // 打开Activity，打开蓝牙
            XposedHelpers.findAndHookMethod(HOOK_ACTIVITY_CLASS_NAME, lpParam.classLoader, ON_RESUME, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.thisObject.javaClass.name.contains(HOOK_ACTIVITY_NAME)) {
                        val activity = param.thisObject as Activity
                        Toast.makeText(activity, "${param.thisObject.javaClass.name} open BLE", Toast.LENGTH_SHORT).show()
                        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return
                        if (!bluetoothAdapter.isEnabled) {
                            bluetoothAdapter.enable()
                        }
                    }
                }
            })
        }
        tryHook {
            // 关闭Activity，关闭蓝牙
            XposedHelpers.findAndHookMethod(HOOK_ACTIVITY_CLASS_NAME, lpParam.classLoader, ON_DESTROY, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.thisObject.javaClass.name.contains(HOOK_ACTIVITY_NAME)) {
                        val activity = param.thisObject as Activity
                        Toast.makeText(activity, "${param.thisObject.javaClass.name} close BLE", Toast.LENGTH_SHORT).show()
                        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return
                        if (bluetoothAdapter.isEnabled) {
                            bluetoothAdapter.disable()
                        }
                    }
                }
            })
        }
        return false
    }

    companion object {
        private const val TOP_JOWANXU_MOBIKEBLE = "top.jowanxu.mobikeble"
        private const val TOP_JOWANXU_MOBIKEBLE_ACTIVITY = "top.jowanxu.mobikeble.MainActivity"
        private const val HOOK_SCANLOGIN_METHOD_NAME = "isModuleLoaded"
        private const val COM_MOBIKE_APP = "com.mobike.mobikeapp"
        private const val HOOK_ACTIVITY_CLASS_NAME = "android.app.Activity"
        private const val HOOK_ACTIVITY_NAME = "QRCode"
        private const val ON_RESUME = "onResume"
        private const val ON_DESTROY = "onDestroy"
        private const val HOOK_ERROR = "Hook 出错 "
        private val TAG = Tutorial::class.java.simpleName

    }
}
