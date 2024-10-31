use tauri::{command, AppHandle, Runtime};

use crate::models::*;
use crate::NetworkInformationExt;
use crate::Result;

#[command]
pub(crate) async fn connection<R: Runtime>(app: AppHandle<R>) -> Result<ConnectionType> {
    app.network_information().connection()
}

#[command]
pub(crate) async fn is_online<R: Runtime>(
    app: AppHandle<R>,
    payload: Option<IsOnlineRequest>,
) -> Result<bool> {
    app.network_information().is_online(payload)
}