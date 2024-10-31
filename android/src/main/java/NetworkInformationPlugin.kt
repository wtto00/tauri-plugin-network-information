package wang.tato.tauri_plugin_network_information

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import android.webkit.WebView
import app.tauri.annotation.Command
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

@TauriPlugin
class NetworkInformationPlugin(private val activity: Activity) : Plugin(activity) {
    private lateinit var connectivityManager: ConnectivityManager

    private val logTag: String = "NetworkManager"

    override fun load(webView: WebView) {
        super.load(webView)
        connectivityManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.getSystemService(ConnectivityManager::class.java)
        } else {
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val request =
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        connectivityManager.registerNetworkCallback(request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    triggerObject("available", true)
                }

                override fun onLost(network: Network) {
                    triggerObject("available", false)
                }
            })
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Command
    fun isOnline(invoke: Invoke) {
        val args = try {
            invoke.getArgs()
        } catch (e: Exception) {
            JSObject()
        }
        val hostname = args.getString("hostname", "8.8.8.8")
        val port = args.getInteger("port", 53)
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                try {
                    val timeoutMs = 1500
                    val socket = Socket()
                    val address = InetSocketAddress(hostname, port)
                    Log.d(logTag, "connect socket ${hostname}:${port}")

                    socket.connect(address, timeoutMs)
                    socket.close()

                    invoke.resolveObject(true)
                } catch (e: IOException) {
                    invoke.resolveObject(false)
                }
            }
        }
    }

    private fun getConnectionType(): Connection {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // >= Android 6.0 (API 23)
            val network = connectivityManager.activeNetwork ?: return Connection.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network)
                ?: return Connection.NONE
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Connection.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Connection.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val telephonyManager = activity.getSystemService(TelephonyManager::class.java)
                    val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        telephonyManager.dataNetworkType
                    } else {
                        @Suppress("DEPRECATION")
                        telephonyManager.networkType
                    }
                    Log.d(logTag, "telephonyManager.networkType: $networkType")
                    when (networkType) {
                        TelephonyManager.NETWORK_TYPE_GSM,
                        TelephonyManager.NETWORK_TYPE_GPRS,
                        TelephonyManager.NETWORK_TYPE_EDGE,
                        TelephonyManager.NETWORK_TYPE_CDMA,
                        TelephonyManager.NETWORK_TYPE_1xRTT,
                        @Suppress("DEPRECATION")
                        TelephonyManager.NETWORK_TYPE_IDEN -> Connection.CELL_2G

                        TelephonyManager.NETWORK_TYPE_TD_SCDMA,
                        TelephonyManager.NETWORK_TYPE_UMTS,
                        TelephonyManager.NETWORK_TYPE_EVDO_0,
                        TelephonyManager.NETWORK_TYPE_EVDO_A,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_HSUPA,
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_EVDO_B,
                        TelephonyManager.NETWORK_TYPE_EHRPD,
                        TelephonyManager.NETWORK_TYPE_HSPAP -> Connection.CELL_3G

                        TelephonyManager.NETWORK_TYPE_IWLAN,
                        19, // TelephonyManager.NETWORK_TYPE_LTE_CA
                        TelephonyManager.NETWORK_TYPE_LTE -> Connection.CELL_4G

                        TelephonyManager.NETWORK_TYPE_NR -> Connection.CELL_5G

                        else -> Connection.CELL
                    }
                }

                else -> Connection.UNKNOWN
            }
        } else
            @Suppress("DEPRECATION")
            {
                // Use NetworkInfo when < Android 6.0
                val networkInfo =
                    connectivityManager.activeNetworkInfo ?: return Connection.NONE
                if (!networkInfo.isConnectedOrConnecting) return Connection.NONE
                val type = networkInfo.type
                Log.d(logTag, "networkInfo.type: $type")
                when (type) {
                    ConnectivityManager.TYPE_WIFI -> Connection.WIFI
                    ConnectivityManager.TYPE_ETHERNET -> Connection.ETHERNET
                    ConnectivityManager.TYPE_MOBILE -> {
                        when (networkInfo.subtype) {
                            TelephonyManager.NETWORK_TYPE_GSM,
                            TelephonyManager.NETWORK_TYPE_GPRS,
                            TelephonyManager.NETWORK_TYPE_CDMA,
                            TelephonyManager.NETWORK_TYPE_EDGE,
                            TelephonyManager.NETWORK_TYPE_1xRTT,
                            TelephonyManager.NETWORK_TYPE_IDEN -> Connection.CELL_2G

                            TelephonyManager.NETWORK_TYPE_TD_SCDMA,
                            TelephonyManager.NETWORK_TYPE_EVDO_A,
                            TelephonyManager.NETWORK_TYPE_UMTS,
                            TelephonyManager.NETWORK_TYPE_EVDO_0,
                            TelephonyManager.NETWORK_TYPE_HSDPA,
                            TelephonyManager.NETWORK_TYPE_HSUPA,
                            TelephonyManager.NETWORK_TYPE_HSPA,
                            TelephonyManager.NETWORK_TYPE_EVDO_B,
                            TelephonyManager.NETWORK_TYPE_EHRPD,
                            TelephonyManager.NETWORK_TYPE_HSPAP -> Connection.CELL_3G

                            TelephonyManager.NETWORK_TYPE_IWLAN,
                            19, // TelephonyManager.NETWORK_TYPE_LTE_CA
                            TelephonyManager.NETWORK_TYPE_LTE -> Connection.CELL_4G

                            TelephonyManager.NETWORK_TYPE_NR -> Connection.CELL_5G

                            else -> {
                                val subTypename = networkInfo.subtypeName
                                Log.d(logTag, "networkInfo.subtypeName: $subTypename")
                                if (subTypename.equals(
                                        "TD-SCDMA",
                                        true
                                    ) || subTypename.equals(
                                        "WCDMA",
                                        true
                                    ) || subTypename.equals("CDMA2000", true)
                                ) {
                                    Connection.CELL_3G
                                } else {
                                    Connection.CELL
                                }
                            }
                        }
                    }

                    else -> Connection.UNKNOWN
                }
            }
    }

    @Command
    fun connection(invoke: Invoke) {
        invoke.resolveObject(getConnectionType().ordinal)
    }
}
