package wang.tato.tauri_plugin_network_information

import app.tauri.plugin.JSObject

enum class Connection() {
    UNKNOWN,
    ETHERNET,
    WIFI,
    CELL_2G,
    CELL_3G,
    CELL_4G,
    CELL_5G,
    CELL,
    NONE;
}
