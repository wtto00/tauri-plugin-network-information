use serde::de::DeserializeOwned;
use tauri::{
    plugin::{PluginApi, PluginHandle},
    AppHandle, Runtime,
};

use crate::models::*;

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_network_information);

// initializes the Kotlin or Swift plugin classes
pub fn init<R: Runtime, C: DeserializeOwned>(
    _app: &AppHandle<R>,
    api: PluginApi<R, C>,
) -> crate::Result<NetworkInformation<R>> {
    #[cfg(target_os = "android")]
    let handle = api.register_android_plugin(
        "wang.tato.tauri_plugin_network_information",
        "NetworkInformationPlugin",
    )?;
    #[cfg(target_os = "ios")]
    let handle = api.register_ios_plugin(init_plugin_network_information)?;
    Ok(NetworkInformation(handle))
}

/// Access to the network-information APIs.
pub struct NetworkInformation<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> NetworkInformation<R> {
    pub fn connection(&self) -> crate::Result<ConnectionType> {
        self.0
            .run_mobile_plugin("connection", ())
            .map_err(Into::into)
    }

    pub fn is_online(&self, payload: Option<IsOnlineRequest>) -> crate::Result<bool> {
        self.0
            .run_mobile_plugin("isOnline", payload)
            .map_err(Into::into)
    }
}
