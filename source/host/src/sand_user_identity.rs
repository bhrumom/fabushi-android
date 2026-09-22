pub const MAX_FULL_NAME_LENGTH:usize=128;
pub fn normalize_sand_user_full_name(input:&str)->Option<String>{
    let compact=input.split_whitespace().collect::<Vec<_>>().join(" ");
    if compact.is_empty(){None}else{Some(compact.chars().take(MAX_FULL_NAME_LENGTH).collect())}
}
pub fn render_user_identity_system_prompt(name:Option<&str>)->String{
    normalize_sand_user_full_name(name.unwrap_or("")).map(|n|format!("The user's display name is {}.",n)).unwrap_or_default()
}
