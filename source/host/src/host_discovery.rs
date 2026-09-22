use std::{fs, io, path::Path};

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct GatewayDiscoveryInfo {
    pub host: String,
    pub port: u16,
    pub auth_token: String,
    pub generation: u64,
}

impl GatewayDiscoveryInfo {
    pub fn validate(&self) -> bool { !self.host.is_empty() && self.port > 0 && self.auth_token.len() >= 16 && self.generation > 0 }
}

pub fn write_gateway_discovery(path: &Path, info: &GatewayDiscoveryInfo) -> io::Result<()> {
    if !info.validate() { return Err(io::Error::new(io::ErrorKind::InvalidInput, "invalid gateway discovery")); }
    if let Some(parent)=path.parent() { fs::create_dir_all(parent)?; }
    let temp=path.with_extension("tmp");
    fs::write(&temp, format!("{}\t{}\t{}\t{}\n", info.host, info.port, info.auth_token, info.generation))?;
    fs::rename(temp, path)
}

pub fn read_gateway_discovery(path: &Path) -> io::Result<GatewayDiscoveryInfo> {
    let text=fs::read_to_string(path)?;
    let mut parts=text.trim().split('\t');
    let info=GatewayDiscoveryInfo {
        host:parts.next().unwrap_or_default().to_string(),
        port:parts.next().unwrap_or_default().parse().unwrap_or(0),
        auth_token:parts.next().unwrap_or_default().to_string(),
        generation:parts.next().unwrap_or_default().parse().unwrap_or(0),
    };
    if info.validate() { Ok(info) } else { Err(io::Error::new(io::ErrorKind::InvalidData, "invalid gateway discovery")) }
}

pub fn clear_gateway_discovery(path: &Path) -> io::Result<()> {
    match fs::remove_file(path) { Ok(())=>Ok(()), Err(error) if error.kind()==io::ErrorKind::NotFound=>Ok(()), Err(error)=>Err(error) }
}
