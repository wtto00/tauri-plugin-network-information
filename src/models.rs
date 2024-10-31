
#[derive(Debug, Clone, serde_repr::Deserialize_repr, serde_repr::Serialize_repr)]
#[repr(u8)]
pub enum ConnectionType {
    UNKNOWN = 0,
    ETHERNET = 1,
    WIFI = 2,
    Cell2g = 3,
    Cell3g = 4,
    Cell4g = 5,
    Cell5g = 6,
    CELL = 7,
    NONE = 8,
}
impl Default for ConnectionType {
    fn default() -> Self {
        ConnectionType::UNKNOWN
    }
}

#[derive(Debug, serde::Deserialize, serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct IsOnlineRequest {
    pub hostname: Option<String>,
    pub port: Option<i32>,
}
