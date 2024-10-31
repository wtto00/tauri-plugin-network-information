#[cfg(mobile)]
use tauri::{
    plugin::{Builder, TauriPlugin},
    Manager, Runtime,
};

#[cfg(mobile)]
pub use models::*;

#[cfg(mobile)]
mod mobile;

#[cfg(mobile)]
mod commands;
#[cfg(mobile)]
mod error;
#[cfg(mobile)]
mod models;

#[cfg(mobile)]
pub use error::{Error, Result};

#[cfg(mobile)]
use mobile::NetworkInformation;

/// Extensions to [`tauri::App`], [`tauri::AppHandle`] and [`tauri::Window`] to access the network-information APIs.
#[cfg(mobile)]
pub trait NetworkInformationExt<R: Runtime> {
    fn network_information(&self) -> &NetworkInformation<R>;
}

#[cfg(mobile)]
impl<R: Runtime, T: Manager<R>> crate::NetworkInformationExt<R> for T {
    fn network_information(&self) -> &NetworkInformation<R> {
        self.state::<NetworkInformation<R>>().inner()
    }
}

/// Initializes the plugin.
#[cfg(mobile)]
pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("network-information")
        .invoke_handler(tauri::generate_handler![
            commands::connection,
            commands::is_online
        ])
        .setup(|app, api| {
            let network_information = mobile::init(app, api)?;
            app.manage(network_information);
            Ok(())
        })
        .build()
}
