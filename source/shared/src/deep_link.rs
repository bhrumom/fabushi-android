use url::Url;

pub const SAND_DEEP_LINK_SCHEME: &str = "sand";
pub const SAND_DEEP_LINK_AUTHORITY: &str = "app";
pub const SAND_HTTPS_DEEP_LINK_ORIGIN: &str = "https://cursor.com";
pub const SAND_HTTPS_DEEP_LINK_PATH_PREFIX: &str = "/sand/link";
pub const SAND_DEEP_LINK_MAX_LENGTH: usize = 2_048;
pub const SAND_PLUGIN_DEEP_LINK_PATH: &str = "/v1/plugin/add";
pub const SAND_OPEN_DEEP_LINK_URL: &str = "sand://app/v1/open";

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum SandDeepLinkSource {
    Protocol,
    Https,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum SandDeepLink {
    Info {
        source: SandDeepLinkSource,
    },
    PluginAdd {
        plugin_id: String,
        source: SandDeepLinkSource,
    },
    Open {
        source: SandDeepLinkSource,
    },
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ParsedSandDeepLink {
    pub link: SandDeepLink,
    pub canonical_url: String,
}

pub fn is_sand_deep_link_plugin_id(value: &str) -> bool {
    !value.is_empty()
        && value.len() <= 19
        && value.bytes().all(|byte| byte.is_ascii_digit())
}

pub fn canonical_sand_deep_link_url(link: &SandDeepLink) -> String {
    match link {
        SandDeepLink::Info { .. } => {
            "sand://app/v1/info?topic=deep-links".to_string()
        }
        SandDeepLink::PluginAdd { plugin_id, .. } => {
            format!("sand://app{SAND_PLUGIN_DEEP_LINK_PATH}?id={plugin_id}")
        }
        SandDeepLink::Open { .. } => SAND_OPEN_DEEP_LINK_URL.to_string(),
    }
}

fn printable_ascii(value: &str) -> bool {
    value.bytes().all(|byte| (33..=126).contains(&byte))
}

fn valid_percent_encoding(value: &str) -> bool {
    let bytes = value.as_bytes();
    let mut index = 0;
    while index < bytes.len() {
        if bytes[index] == b'%' {
            if index + 2 >= bytes.len()
                || !bytes[index + 1].is_ascii_hexdigit()
                || !bytes[index + 2].is_ascii_hexdigit()
            {
                return false;
            }
            index += 3;
        } else {
            index += 1;
        }
    }
    true
}

fn canonical_path_section(raw: &str) -> bool {
    let before_query = raw.split_once('?').map(|(head, _)| head).unwrap_or(raw);
    if before_query.contains('%') {
        return false;
    }
    !before_query
        .split('/')
        .any(|segment| segment == "." || segment == "..")
}

fn exact_query(
    url: &Url,
    allowed: &[(&str, &[&str])],
) -> Option<Vec<(String, String)>> {
    let pairs = url
        .query_pairs()
        .map(|(key, value)| (key.into_owned(), value.into_owned()))
        .collect::<Vec<_>>();
    if pairs.len() != allowed.len() {
        return None;
    }
    let mut out = Vec::with_capacity(pairs.len());
    for (key, choices) in allowed {
        let matches = pairs
            .iter()
            .filter(|(candidate, _)| candidate == key)
            .collect::<Vec<_>>();
        if matches.len() != 1 || !choices.contains(&matches[0].1.as_str()) {
            return None;
        }
        out.push(((*key).to_string(), matches[0].1.clone()));
    }
    Some(out)
}

pub fn parse_sand_deep_link(raw: &str) -> Option<ParsedSandDeepLink> {
    if raw.is_empty()
        || raw.len() > SAND_DEEP_LINK_MAX_LENGTH
        || !printable_ascii(raw)
        || raw.contains('#')
        || raw.contains('\\')
        || !valid_percent_encoding(raw)
        || !canonical_path_section(raw)
    {
        return None;
    }

    let lower = raw.to_ascii_lowercase();
    let source = if lower.starts_with("sand:") {
        SandDeepLinkSource::Protocol
    } else if lower.starts_with("https:") {
        SandDeepLinkSource::Https
    } else {
        return None;
    };

    let url = Url::parse(raw).ok()?;
    if !url.username().is_empty() || url.password().is_some() || url.port().is_some() {
        return None;
    }

    let path = url.path();
    let route = match source {
        SandDeepLinkSource::Protocol => {
            if url.scheme() != SAND_DEEP_LINK_SCHEME
                || url.host_str()? != SAND_DEEP_LINK_AUTHORITY
            {
                return None;
            }
            path.to_string()
        }
        SandDeepLinkSource::Https => {
            if url.scheme() != "https" || url.host_str()? != "cursor.com" {
                return None;
            }
            path.strip_prefix(SAND_HTTPS_DEEP_LINK_PATH_PREFIX)?
                .to_string()
        }
    };

    let link = match route.as_str() {
        "/v1/plugin/add" => {
            let query = exact_query(&url, &[("id", &[])]);
            let pairs = url.query_pairs().collect::<Vec<_>>();
            if query.is_some() || pairs.len() != 1 || pairs[0].0 != "id" {
                // The empty choice list above is deliberately not used for plugin ids;
                // validate the single decoded id explicitly below.
            }
            if pairs.len() != 1 || pairs[0].0 != "id" {
                return None;
            }
            let plugin_id = pairs[0].1.to_string();
            if !is_sand_deep_link_plugin_id(&plugin_id) {
                return None;
            }
            SandDeepLink::PluginAdd { plugin_id, source }
        }
        "/v1/open" => {
            if exact_query(&url, &[]).is_none() {
                return None;
            }
            SandDeepLink::Open { source }
        }
        "/v1/info" => {
            if exact_query(&url, &[("topic", &["deep-links"])]).is_none() {
                return None;
            }
            SandDeepLink::Info { source }
        }
        _ => return None,
    };

    Some(ParsedSandDeepLink {
        canonical_url: canonical_sand_deep_link_url(&link),
        link,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn protocol_and_https_forms_canonicalize_to_protocol() {
        let protocol = parse_sand_deep_link("sand://app/v1/info?topic=deep-links").unwrap();
        let https =
            parse_sand_deep_link("https://cursor.com/sand/link/v1/info?topic=deep-links")
                .unwrap();
        assert_eq!(protocol.canonical_url, "sand://app/v1/info?topic=deep-links");
        assert_eq!(https.canonical_url, protocol.canonical_url);

        let plugin =
            parse_sand_deep_link("https://cursor.com/sand/link/v1/plugin/add?id=123").unwrap();
        assert_eq!(plugin.canonical_url, "sand://app/v1/plugin/add?id=123");
    }

    #[test]
    fn parser_fails_closed_for_credentials_fragments_duplicates_and_path_tricks() {
        for raw in [
            "sand://user:pass@app/v1/open",
            "sand://app/v1/open#x",
            "sand://app/v1/../open",
            "sand://app/v1/%6fpen",
            "sand://app/v1/info?topic=deep-links&topic=deep-links",
            "sand://app/v1/plugin/add?id=123&id=456",
            "sand://app/v1/plugin/add?id=abc",
            "sand://app/v1/open?extra=1",
        ] {
            assert!(parse_sand_deep_link(raw).is_none(), "{raw}");
        }
    }

    #[test]
    fn plugin_id_is_one_to_nineteen_decimal_digits() {
        assert!(is_sand_deep_link_plugin_id("1"));
        assert!(is_sand_deep_link_plugin_id("1234567890123456789"));
        assert!(!is_sand_deep_link_plugin_id(""));
        assert!(!is_sand_deep_link_plugin_id("12345678901234567890"));
        assert!(!is_sand_deep_link_plugin_id("12-3"));
    }
}
